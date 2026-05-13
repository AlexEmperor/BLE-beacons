-- Создаёт тестового юзера admin / 123456.
-- Пароль хешируется bcrypt через pgcrypto (gen_salt('bf')).
-- При повторном запуске — no-op (ON CONFLICT).

INSERT INTO users (login, password_hash)
VALUES ('admin', crypt('123456', gen_salt('bf')))
ON CONFLICT (login) DO NOTHING;
