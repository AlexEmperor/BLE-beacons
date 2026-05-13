-- 002_plans.sql
-- Роли пользователей + хранилище планов/этажей/маяков + доступы.
-- Запускать в Neon SQL Editor ОДИН раз. Идемпотентно.

-- 1. Роли
ALTER TABLE users
  ADD COLUMN IF NOT EXISTS role TEXT NOT NULL DEFAULT 'user'
    CHECK (role IN ('admin', 'user'));

UPDATE users SET role = 'admin' WHERE login = 'admin';

-- 2. Локации (здание / большой объект)
CREATE TABLE IF NOT EXISTS plan_locations (
  id          BIGSERIAL PRIMARY KEY,
  code        TEXT NOT NULL UNIQUE,        -- стабильный ключ для backend/Android
  name        TEXT NOT NULL,
  is_public   BOOLEAN NOT NULL DEFAULT false,  -- видна всем (карта Москвы)
  sort_order  INTEGER NOT NULL DEFAULT 0,
  created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- 3. Этажи внутри локации
CREATE TABLE IF NOT EXISTS plan_floors (
  id              BIGSERIAL PRIMARY KEY,
  location_id     BIGINT NOT NULL REFERENCES plan_locations(id) ON DELETE CASCADE,
  code            TEXT NOT NULL,
  name            TEXT NOT NULL,
  asset_path      TEXT NOT NULL DEFAULT '',  -- путь в assets/plans/...
  is_svg          BOOLEAN NOT NULL DEFAULT true,
  is_world_map    BOOLEAN NOT NULL DEFAULT false,  -- если true — рисуем OSM-карту, не файл
  width_meters    DOUBLE PRECISION NOT NULL,
  height_meters   DOUBLE PRECISION NOT NULL,
  level           INTEGER NOT NULL DEFAULT 1,
  ref_lat         DOUBLE PRECISION NOT NULL DEFAULT 0,
  ref_lon         DOUBLE PRECISION NOT NULL DEFAULT 0,
  sort_order      INTEGER NOT NULL DEFAULT 0,
  UNIQUE (location_id, code)
);

-- 4. Известные маяки на этажах
CREATE TABLE IF NOT EXISTS plan_beacons (
  id            BIGSERIAL PRIMARY KEY,
  floor_id      BIGINT NOT NULL REFERENCES plan_floors(id) ON DELETE CASCADE,
  beacon_id     TEXT NOT NULL,                  -- короткое имя ("i9h2")
  mac           TEXT NOT NULL,
  protocol      TEXT NOT NULL DEFAULT 'iBeacon',
  major         INTEGER NOT NULL DEFAULT 0,
  minor         INTEGER NOT NULL DEFAULT 0,
  tx_power      INTEGER NOT NULL DEFAULT -65,   -- iBeacon TX Power Level @ 1m
  numeric_id    BIGINT NOT NULL DEFAULT 0,      -- наш расширенный iBeacon: уникальный uint32
  -- координаты в нормализованных 0..1 координатах плана (image-space, y=0 — верх)
  x_norm        DOUBLE PRECISION NOT NULL,
  y_norm        DOUBLE PRECISION NOT NULL,
  -- абсолютные географические координаты маяка
  latitude      DOUBLE PRECISION NOT NULL DEFAULT 0,
  longitude     DOUBLE PRECISION NOT NULL DEFAULT 0,
  description   TEXT NOT NULL DEFAULT '',
  UNIQUE (floor_id, mac)
);

-- 5. Доступы юзеров к локациям (admin не нуждается в записях — видит всё)
CREATE TABLE IF NOT EXISTS user_plan_access (
  user_id      BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  location_id  BIGINT NOT NULL REFERENCES plan_locations(id) ON DELETE CASCADE,
  granted_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (user_id, location_id)
);

CREATE INDEX IF NOT EXISTS idx_plan_floors_location ON plan_floors(location_id);
CREATE INDEX IF NOT EXISTS idx_plan_beacons_floor   ON plan_beacons(floor_id);
CREATE INDEX IF NOT EXISTS idx_user_plan_user       ON user_plan_access(user_id);
