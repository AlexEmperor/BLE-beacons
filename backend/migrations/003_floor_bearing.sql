-- 003_floor_bearing.sql
-- Угол поворота плана этажа относительно севера (по часовой стрелке, градусы).
-- 0 — план «север сверху». Для зданий, повёрнутых относительно сторон света,
-- этот угол выставляется вручную, чтобы overlay лёг ровно по контуру здания.

ALTER TABLE plan_floors
  ADD COLUMN IF NOT EXISTS bearing_deg DOUBLE PRECISION NOT NULL DEFAULT 0;
