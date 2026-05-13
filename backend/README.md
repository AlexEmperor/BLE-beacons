# bleapp-auth — Cloudflare Worker (auth proxy)

Тонкая прокси между Android-приложением и Neon Postgres.
`POST /login` принимает `{login, password}`, проверяет хеш в БД через `pgcrypto.crypt()` и возвращает подписанный JWT.

## Что внутри

- `src/index.ts` — Worker: `POST /login`, `GET /health`.
- `schema.sql` — таблица `users` + расширение `pgcrypto`.
- `seed.sql` — создаёт тестового юзера `admin / 123456`.
- `wrangler.toml` — конфиг Worker (имя, дата совместимости).

Секреты (`DATABASE_URL`, `JWT_SECRET`) **не лежат в репо** — кладутся в Cloudflare через `wrangler secret put`.

## Деплой

### 1. Подготовь Neon

В Neon Console → SQL Editor выполни содержимое `schema.sql`, затем `seed.sql`. Тестовый юзер: `admin` / `123456`.

### 2. Получи URL подключения

Neon даёт строку в формате ADO.NET, нам нужен URL для драйвера. Из:

```
Host=ep-hidden-darkness-...neon.tech; Database=neondb; Username=neondb_owner; Password=XXX; SSL Mode=VerifyFull; Channel Binding=Require;
```

собери:

```
postgresql://neondb_owner:XXX@ep-hidden-darkness-...neon.tech/neondb?sslmode=require
```

(`@neondatabase/serverless` сам разруливает SSL/channel binding — параметры из ADO-строки переносить не нужно.)

### 3. Установи зависимости и залогинься в CF

```powershell
cd backend
npm install
npx wrangler login
```

### 4. Положи секреты

```powershell
npx wrangler secret put DATABASE_URL
# вставь postgresql://... URL из шага 2

npx wrangler secret put JWT_SECRET
# любая длинная случайная строка, например:
# powershell: -join ((48..57) + (97..122) | Get-Random -Count 64 | % {[char]$_})
```

### 5. Деплой

```powershell
npx wrangler deploy
```

После деплоя получишь URL вида `https://bleapp-auth.<твой-сабдомен>.workers.dev`.

### 6. Проверка

```powershell
curl -X POST https://bleapp-auth.<...>.workers.dev/login `
  -H "content-type: application/json" `
  -d '{"login":"admin","password":"123456"}'
```

Должен вернуться `{"token":"eyJ...","user":{"id":1,"login":"admin"}}`.
На неверном пароле — `401 invalid_credentials`.

## Локальная разработка

Создай `backend/.dev.vars` (он в `.gitignore`):

```
DATABASE_URL=postgresql://neondb_owner:XXX@...neon.tech/neondb?sslmode=require
JWT_SECRET=локальный-секрет
```

Запусти:

```powershell
npm run dev
```

Worker поднимется на `http://127.0.0.1:8787`.
