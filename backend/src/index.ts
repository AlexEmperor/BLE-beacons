import { neon } from "@neondatabase/serverless";
import { SignJWT, jwtVerify } from "jose";

export interface Env {
  DATABASE_URL: string;
  JWT_SECRET: string;
}

const TOKEN_TTL = "3h";
const DEFAULT_NEW_USER_LOCATION = "intellect_gnss";

type Role = "admin" | "user";

interface User {
  id: number;
  login: string;
  role: Role;
}

export default {
  async fetch(req: Request, env: Env): Promise<Response> {
    try {
      const url = new URL(req.url);
      const path = url.pathname;
      const method = req.method;

      if (method === "POST" && path === "/login")    return await handleLogin(req, env);
      if (method === "POST" && path === "/register") return await handleRegister(req, env);
      if (method === "POST" && path === "/password") return await handlePasswordChange(req, env);
      if (method === "GET"  && path === "/me")       return await handleMe(req, env);
      if (method === "GET"  && path === "/plans")    return await handlePlans(req, env);
      if (method === "GET"  && path === "/admin/state")  return await handleAdminState(req, env);
      if (method === "POST" && path === "/admin/grant")  return await handleAdminGrant(req, env);
      if (method === "POST" && path === "/admin/revoke") return await handleAdminRevoke(req, env);
      if (method === "GET"  && path === "/health")   return json({ ok: true });

      return json({ error: "not_found" }, 404);
    } catch (e) {
      const err = e as Error;
      console.error("worker_error", err?.message, err?.stack);
      return json({ error: "internal", message: err?.message ?? String(e) }, 500);
    }
  },
};

async function handleLogin(req: Request, env: Env): Promise<Response> {
  const body = await safeJson(req);
  const login = readLogin(body?.login);
  const password = readPassword(body?.password);
  if (!login || !password) return json({ error: "bad_request" }, 400);

  const sql = neon(env.DATABASE_URL);
  const rows = (await sql`
    SELECT id, login, role
    FROM users
    WHERE login = ${login}
      AND password_hash = crypt(${password}, password_hash)
    LIMIT 1
  `) as Array<User>;
  if (rows.length === 0) return json({ error: "invalid_credentials" }, 401);

  return await issueTokenResponse(rows[0], env);
}

async function handleRegister(req: Request, env: Env): Promise<Response> {
  const body = await safeJson(req);
  const login = readLogin(body?.login);
  const password = readPassword(body?.password);
  if (!login) return json({ error: "bad_login" }, 400);
  if (login.length < 3 || login.length > 64) return json({ error: "bad_login" }, 400);
  if (!password || password.length < 6) return json({ error: "weak_password" }, 400);

  const sql = neon(env.DATABASE_URL);
  let user: User;
  try {
    const rows = (await sql`
      INSERT INTO users (login, password_hash)
      VALUES (${login}, crypt(${password}, gen_salt('bf')))
      RETURNING id, login, role
    `) as Array<User>;
    user = rows[0];
  } catch (e: unknown) {
    const code = (e as { code?: string })?.code;
    if (code === "23505") return json({ error: "user_exists" }, 409);
    throw e;
  }

  // Дефолтный доступ — одна локация
  await sql`
    INSERT INTO user_plan_access (user_id, location_id)
    SELECT ${user.id}, id FROM plan_locations WHERE code = ${DEFAULT_NEW_USER_LOCATION}
    ON CONFLICT DO NOTHING
  `;

  return await issueTokenResponse(user, env);
}

async function handlePasswordChange(req: Request, env: Env): Promise<Response> {
  const auth = await requireAuth(req, env);
  if (auth.kind !== "ok") return auth.response;

  const body = await safeJson(req);
  const oldPassword = readPassword(body?.oldPassword);
  const newPassword = readPassword(body?.newPassword);
  if (!oldPassword || !newPassword) return json({ error: "bad_request" }, 400);
  if (newPassword.length < 6) return json({ error: "weak_password" }, 400);

  const sql = neon(env.DATABASE_URL);
  const rows = (await sql`
    UPDATE users
    SET password_hash = crypt(${newPassword}, gen_salt('bf'))
    WHERE id = ${auth.user.id}
      AND password_hash = crypt(${oldPassword}, password_hash)
    RETURNING id, login, role
  `) as Array<User>;
  if (rows.length === 0) return json({ error: "invalid_credentials" }, 401);

  return await issueTokenResponse(rows[0], env);
}

async function handleMe(req: Request, env: Env): Promise<Response> {
  const auth = await requireAuth(req, env);
  if (auth.kind !== "ok") return auth.response;
  return json({ user: auth.user });
}

async function handlePlans(req: Request, env: Env): Promise<Response> {
  const auth = await requireAuth(req, env);
  if (auth.kind !== "ok") return auth.response;

  const sql = neon(env.DATABASE_URL);
  const isAdmin = auth.user.role === "admin";

  // Локации: всё public + всё, к чему есть user_plan_access (admin тоже фильтруется
  // через user_plan_access, но в seed мы дали ему все).
  const locations = (await sql`
    SELECT pl.id, pl.code, pl.name, pl.is_public, pl.sort_order
    FROM plan_locations pl
    WHERE pl.is_public = true
       OR ${isAdmin}::boolean = true
       OR EXISTS (
         SELECT 1 FROM user_plan_access upa
         WHERE upa.user_id = ${auth.user.id} AND upa.location_id = pl.id
       )
    ORDER BY pl.sort_order, pl.id
  `) as Array<{ id: number; code: string; name: string; is_public: boolean; sort_order: number }>;

  if (locations.length === 0) {
    return json({ user: auth.user, locations: [] });
  }

  const locationIds = locations.map((l) => l.id);
  const floors = (await sql`
    SELECT id, location_id, code, name, asset_path, is_svg, is_world_map,
           width_meters, height_meters, level, ref_lat, ref_lon, bearing_deg, sort_order
    FROM plan_floors
    WHERE location_id = ANY(${locationIds})
    ORDER BY location_id, sort_order, id
  `) as Array<{
    id: number; location_id: number; code: string; name: string;
    asset_path: string; is_svg: boolean; is_world_map: boolean;
    width_meters: number; height_meters: number; level: number;
    ref_lat: number; ref_lon: number; bearing_deg: number; sort_order: number;
  }>;

  const floorIds = floors.map((f) => f.id);
  const beacons = floorIds.length === 0
    ? []
    : (await sql`
        SELECT id, floor_id, beacon_id, mac, protocol, major, minor, tx_power,
               numeric_id, x_norm, y_norm, latitude, longitude, description
        FROM plan_beacons
        WHERE floor_id = ANY(${floorIds})
        ORDER BY floor_id, id
      `) as Array<{
        id: number; floor_id: number; beacon_id: string; mac: string;
        protocol: string; major: number; minor: number; tx_power: number;
        numeric_id: number; x_norm: number; y_norm: number;
        latitude: number; longitude: number; description: string;
      }>;

  const beaconsByFloor = new Map<number, unknown[]>();
  for (const b of beacons) {
    const list = beaconsByFloor.get(b.floor_id) ?? [];
    list.push({
      id: b.beacon_id,
      mac: b.mac,
      protocol: b.protocol,
      major: b.major,
      minor: b.minor,
      txPower: b.tx_power,
      beaconId: b.numeric_id,
      xNorm: b.x_norm,
      yNorm: b.y_norm,
      latitude: b.latitude,
      longitude: b.longitude,
      description: b.description,
    });
    beaconsByFloor.set(b.floor_id, list);
  }

  const floorsByLocation = new Map<number, unknown[]>();
  for (const f of floors) {
    const list = floorsByLocation.get(f.location_id) ?? [];
    list.push({
      code: f.code,
      name: f.name,
      assetPath: f.asset_path,
      isSvg: f.is_svg,
      isWorldMap: f.is_world_map,
      widthMeters: f.width_meters,
      heightMeters: f.height_meters,
      level: f.level,
      refLat: f.ref_lat,
      refLon: f.ref_lon,
      bearingDeg: f.bearing_deg,
      sortOrder: f.sort_order,
      beacons: beaconsByFloor.get(f.id) ?? [],
    });
    floorsByLocation.set(f.location_id, list);
  }

  const result = locations.map((l) => ({
    code: l.code,
    name: l.name,
    isPublic: l.is_public,
    sortOrder: l.sort_order,
    floors: floorsByLocation.get(l.id) ?? [],
  }));

  return json({ user: auth.user, locations: result });
}

async function handleAdminState(req: Request, env: Env): Promise<Response> {
  const auth = await requireAdmin(req, env);
  if (auth.kind !== "ok") return auth.response;

  const sql = neon(env.DATABASE_URL);
  const [users, locations, grants] = await Promise.all([
    sql`SELECT id, login, role FROM users ORDER BY id` as Promise<
      Array<{ id: number; login: string; role: Role }>
    >,
    sql`SELECT code, name, is_public FROM plan_locations ORDER BY sort_order, id` as Promise<
      Array<{ code: string; name: string; is_public: boolean }>
    >,
    sql`
      SELECT upa.user_id, pl.code AS location_code
      FROM user_plan_access upa
      JOIN plan_locations pl ON pl.id = upa.location_id
    ` as Promise<Array<{ user_id: number; location_code: string }>>,
  ]);

  return json({
    users,
    locations: locations.map((l) => ({
      code: l.code,
      name: l.name,
      isPublic: l.is_public,
    })),
    grants: grants.map((g) => ({
      userId: g.user_id,
      locationCode: g.location_code,
    })),
  });
}

async function handleAdminGrant(req: Request, env: Env): Promise<Response> {
  const auth = await requireAdmin(req, env);
  if (auth.kind !== "ok") return auth.response;

  const body = await safeJson(req);
  const userId = Number(body?.userId);
  const locationCode = readLogin(body?.locationCode);
  if (!Number.isFinite(userId) || !locationCode) {
    return json({ error: "bad_request" }, 400);
  }

  const sql = neon(env.DATABASE_URL);
  const result = (await sql`
    INSERT INTO user_plan_access (user_id, location_id)
    SELECT ${userId}, id FROM plan_locations WHERE code = ${locationCode}
    ON CONFLICT DO NOTHING
    RETURNING user_id
  `) as Array<{ user_id: number }>;

  // Если ничего не вставилось и при этом локация существует — значит запись уже была
  // (что для grant это успех). Если локации нет — 404.
  if (result.length === 0) {
    const exists = (await sql`
      SELECT 1 FROM plan_locations WHERE code = ${locationCode} LIMIT 1
    `) as Array<unknown>;
    if (exists.length === 0) return json({ error: "location_not_found" }, 404);
  }
  return json({ ok: true });
}

async function handleAdminRevoke(req: Request, env: Env): Promise<Response> {
  const auth = await requireAdmin(req, env);
  if (auth.kind !== "ok") return auth.response;

  const body = await safeJson(req);
  const userId = Number(body?.userId);
  const locationCode = readLogin(body?.locationCode);
  if (!Number.isFinite(userId) || !locationCode) {
    return json({ error: "bad_request" }, 400);
  }

  const sql = neon(env.DATABASE_URL);
  await sql`
    DELETE FROM user_plan_access
    WHERE user_id = ${userId}
      AND location_id = (SELECT id FROM plan_locations WHERE code = ${locationCode})
  `;
  return json({ ok: true });
}

type AuthResult =
  | { kind: "ok"; user: User }
  | { kind: "err"; response: Response };

async function requireAdmin(req: Request, env: Env): Promise<AuthResult> {
  const auth = await requireAuth(req, env);
  if (auth.kind !== "ok") return auth;
  if (auth.user.role !== "admin") {
    return { kind: "err", response: json({ error: "forbidden" }, 403) };
  }
  return auth;
}

async function requireAuth(req: Request, env: Env): Promise<AuthResult> {
  const header = req.headers.get("authorization") ?? "";
  const m = /^Bearer\s+(.+)$/i.exec(header);
  if (!m) return { kind: "err", response: json({ error: "unauthorized" }, 401) };
  const secret = new TextEncoder().encode(env.JWT_SECRET);
  try {
    const { payload } = await jwtVerify(m[1], secret);
    const sub = payload.sub;
    if (!sub) return { kind: "err", response: json({ error: "token_invalid" }, 401) };
    const claims = payload as Record<string, unknown>;
    const role = claims.role === "admin" ? "admin" : "user";
    return {
      kind: "ok",
      user: {
        id: Number(sub),
        login: String(claims.login ?? ""),
        role,
      },
    };
  } catch {
    return { kind: "err", response: json({ error: "token_invalid" }, 401) };
  }
}

async function issueTokenResponse(user: User, env: Env): Promise<Response> {
  const secret = new TextEncoder().encode(env.JWT_SECRET);
  const token = await new SignJWT({ login: user.login, role: user.role })
    .setProtectedHeader({ alg: "HS256" })
    .setSubject(String(user.id))
    .setIssuedAt()
    .setExpirationTime(TOKEN_TTL)
    .sign(secret);
  return json({ token, user });
}

async function safeJson(req: Request): Promise<Record<string, unknown> | null> {
  try {
    const v = await req.json();
    return (typeof v === "object" && v !== null) ? (v as Record<string, unknown>) : null;
  } catch {
    return null;
  }
}

function readLogin(v: unknown): string {
  return typeof v === "string" ? v.trim() : "";
}

function readPassword(v: unknown): string {
  return typeof v === "string" ? v : "";
}

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });
}
