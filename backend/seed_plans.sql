-- seed_plans.sql
-- Миграция всех существующих хардкод-данных (PlanLocations.kt + RealBeacons.kt)
-- из APK в БД. Идемпотентно через ON CONFLICT.
-- Запускать ОДИН раз ПОСЛЕ 002_plans.sql.

-- 1. Локации
INSERT INTO plan_locations (code, name, is_public, sort_order) VALUES
  ('intellect_gnss', 'Интеллект',           false, 10),
  ('ble_lab',        'BLE — Лаборатория',   false, 20),
  ('niima_rtt',      'Niima_Lab (RTT)',     false, 30),
  ('moscow',         'Москва',              true,  90)
ON CONFLICT (code) DO UPDATE SET
  name       = EXCLUDED.name,
  is_public  = EXCLUDED.is_public,
  sort_order = EXCLUDED.sort_order;

-- 2. Этажи (location_code → floor)
WITH loc AS (SELECT id FROM plan_locations WHERE code = 'intellect_gnss')
INSERT INTO plan_floors
  (location_id, code, name, asset_path, is_svg, width_meters, height_meters, level, ref_lat, ref_lon, sort_order)
SELECT loc.id, v.code, v.name, v.asset_path, v.is_svg, v.w, v.h, v.lvl, v.lat, v.lon, v.so FROM loc, (VALUES
  ('ig_f1', '1 этаж', 'plans/intellect_gnss/floor1.svg', true, 83.33, 59.00, 1, 55.84341, 37.53777, 1),
  ('ig_f2', '2 этаж', 'plans/intellect_gnss/floor2.svg', true, 83.04, 59.00, 2, 55.84340, 37.53777, 2),
  ('ig_f3', '3 этаж', 'plans/intellect_gnss/floor3.svg', true, 83.20, 59.00, 3, 55.84341, 37.53778, 3)
) AS v(code, name, asset_path, is_svg, w, h, lvl, lat, lon, so)
ON CONFLICT (location_id, code) DO UPDATE SET
  name = EXCLUDED.name, asset_path = EXCLUDED.asset_path, is_svg = EXCLUDED.is_svg,
  width_meters = EXCLUDED.width_meters, height_meters = EXCLUDED.height_meters,
  level = EXCLUDED.level, ref_lat = EXCLUDED.ref_lat, ref_lon = EXCLUDED.ref_lon,
  sort_order = EXCLUDED.sort_order;

WITH loc AS (SELECT id FROM plan_locations WHERE code = 'ble_lab')
INSERT INTO plan_floors
  (location_id, code, name, asset_path, is_svg, width_meters, height_meters, level, ref_lat, ref_lon, sort_order)
SELECT loc.id, v.code, v.name, v.asset_path, v.is_svg, v.w, v.h, v.lvl, v.lat, v.lon, v.so FROM loc, (VALUES
  ('ble_indoor',  'NIIMA-Lab (BLE)', 'plans/ble_lab/niima_lab_ble.png', false, 15.45,  18.81,  1, 55.84364764, 37.5383035,  1),
  ('ble_outdoor', 'Улица',           'plans/ble_lab/outdoor.png',       false, 245.91, 155.68, 1, 55.84340244, 37.53807038, 2)
) AS v(code, name, asset_path, is_svg, w, h, lvl, lat, lon, so)
ON CONFLICT (location_id, code) DO UPDATE SET
  name = EXCLUDED.name, asset_path = EXCLUDED.asset_path, is_svg = EXCLUDED.is_svg,
  width_meters = EXCLUDED.width_meters, height_meters = EXCLUDED.height_meters,
  level = EXCLUDED.level, ref_lat = EXCLUDED.ref_lat, ref_lon = EXCLUDED.ref_lon,
  sort_order = EXCLUDED.sort_order;

WITH loc AS (SELECT id FROM plan_locations WHERE code = 'niima_rtt')
INSERT INTO plan_floors
  (location_id, code, name, asset_path, is_svg, width_meters, height_meters, level, ref_lat, ref_lon, sort_order)
SELECT loc.id, v.code, v.name, v.asset_path, v.is_svg, v.w, v.h, v.lvl, v.lat, v.lon, v.so FROM loc, (VALUES
  ('niima_f1', '1 этаж', 'plans/niima_rtt/niima_lab.png', false, 15.98, 19.43, 1, 55.84364841, 37.5383073, 1)
) AS v(code, name, asset_path, is_svg, w, h, lvl, lat, lon, so)
ON CONFLICT (location_id, code) DO UPDATE SET
  name = EXCLUDED.name, asset_path = EXCLUDED.asset_path, is_svg = EXCLUDED.is_svg,
  width_meters = EXCLUDED.width_meters, height_meters = EXCLUDED.height_meters,
  level = EXCLUDED.level, ref_lat = EXCLUDED.ref_lat, ref_lon = EXCLUDED.ref_lon,
  sort_order = EXCLUDED.sort_order;

WITH loc AS (SELECT id FROM plan_locations WHERE code = 'moscow')
INSERT INTO plan_floors
  (location_id, code, name, asset_path, is_svg, is_world_map,
   width_meters, height_meters, level, ref_lat, ref_lon, sort_order)
SELECT loc.id, v.code, v.name, v.asset_path, v.is_svg, v.is_world_map,
       v.w, v.h, v.lvl, v.lat, v.lon, v.so FROM loc, (VALUES
  ('moscow_map', 'Карта', '', false, true, 50000.0, 50000.0, 1, 55.7558, 37.6173, 1)
) AS v(code, name, asset_path, is_svg, is_world_map, w, h, lvl, lat, lon, so)
ON CONFLICT (location_id, code) DO UPDATE SET
  name = EXCLUDED.name, asset_path = EXCLUDED.asset_path, is_svg = EXCLUDED.is_svg,
  is_world_map = EXCLUDED.is_world_map,
  width_meters = EXCLUDED.width_meters, height_meters = EXCLUDED.height_meters,
  level = EXCLUDED.level, ref_lat = EXCLUDED.ref_lat, ref_lon = EXCLUDED.ref_lon,
  sort_order = EXCLUDED.sort_order;

-- 3. Маяки (по floor_code; используем lookup через CTE)
-- ig_f1
WITH f AS (
  SELECT pf.id FROM plan_floors pf
  JOIN plan_locations pl ON pl.id = pf.location_id
  WHERE pl.code = 'intellect_gnss' AND pf.code = 'ig_f1'
)
INSERT INTO plan_beacons (floor_id, beacon_id, mac, major, minor, tx_power, numeric_id, x_norm, y_norm, latitude, longitude, description)
SELECT f.id, v.bid, v.mac, v.major, v.minor, v.tx, v.nid, v.x, 1 - v.ky, v.lat, v.lon, v.descr FROM f, (VALUES
  ('i9h2', '02:00:70:8C:B3:15', 28812, 45845, -65, 1760, 0.87469718, 0.25178026, 55.84322368256353, 37.538112610661074, 'i9h2'),
  ('YSXI', '02:00:48:17:79:C0', 18455, 31168, -65, 1763, 0.79440203, 0.24973469, 55.84322215981929, 37.53803880142772, 'YSXI'),
  ('fvX6', '02:00:9F:66:DE:DF', 40806, 57055, -65, 1765, 0.8443152,  0.38269475, 55.84332113671387, 37.53808468281423, 'fvX6'),
  ('n7w1', '02:00:E7:49:7D:60', 59209, 32096, -65, 1768, 0.24023438, 0.26073584, 55.84323034919333, 37.53752939719352, 'n7w1'),
  ('76nq', '02:00:B4:9B:3B:FD', 46235, 15357, -65, 1771, 0.11368109, 0.25706384, 55.84322761571715, 37.537413066365154,'76nq'),
  ('0wYF', '02:00:EF:A2:D4:3B', 61346, 54331, -65, 1780, 0.15348763, 0.3775809,  55.843317329909084,37.53744965749426, '0wYF')
) AS v(bid, mac, major, minor, tx, nid, x, ky, lat, lon, descr)
ON CONFLICT (floor_id, mac) DO UPDATE SET
  beacon_id = EXCLUDED.beacon_id, major = EXCLUDED.major, minor = EXCLUDED.minor,
  tx_power = EXCLUDED.tx_power, numeric_id = EXCLUDED.numeric_id,
  x_norm = EXCLUDED.x_norm, y_norm = EXCLUDED.y_norm,
  latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude,
  description = EXCLUDED.description;

-- ig_f2
WITH f AS (
  SELECT pf.id FROM plan_floors pf
  JOIN plan_locations pl ON pl.id = pf.location_id
  WHERE pl.code = 'intellect_gnss' AND pf.code = 'ig_f2'
)
INSERT INTO plan_beacons (floor_id, beacon_id, mac, major, minor, tx_power, numeric_id, x_norm, y_norm, latitude, longitude, description)
SELECT f.id, v.bid, v.mac, v.major, v.minor, v.tx, v.nid, v.x, 1 - v.ky, v.lat, v.lon, v.descr FROM f, (VALUES
  ('61FF', '02:00:02:36:B0:EE',   566, 45294, -65, 1762, 0.81284817, 0.24947902, 55.84321131350116, 37.538059581727005,'61FF'),
  ('7CsC', '02:00:D7:61:21:CB', 55137,  8651, -65, 1766, 0.76438179, 0.30266303, 55.84325137044426, 37.53801471978,    '7CsC'),
  ('kdiK', '02:00:EC:0B:3F:51', 60427, 16209, -65, 1770, 0.75931814, 0.48573875, 55.843389258762556,37.538010032712435,'kdiK'),
  ('D4cr', '02:00:DF:43:65:47', 57155, 25927, -65, 1775, 0.76148828, 0.6647234,  55.843524065783335,37.538012041459645,'D4cr'),
  ('cMEB', '02:00:CB:D3:27:4A', 52179, 10058, -65, 1777, 0.70723479, 0.26788888, 55.843225179373896,37.53796182278862, 'cMEB'),
  ('V09o', '02:00:50:97:D8:CA', 20631, 55498, -65, 1779, 0.59583431, 0.26788888, 55.843225179373896,37.53785870713546, 'V09o'),
  ('fFMG', '02:00:25:2B:9F:E0',  9515, 40928, -65, 1784, 0.43741416, 0.26584331, 55.843223638698774,37.53771206865384, 'fFMG'),
  ('nWlS', '02:00:79:10:90:67', 30992, 36967, -65, 1787, 0.31009934, 0.26379777, 55.84322209804625, 37.537594222206316,'nWlS'),
  ('aBNw', '02:00:1A:E8:60:5D',  6888, 24669, -65, 1788, 0.18327734, 0.47441153, 55.84338072736737, 37.53747683192789, 'aBNw'),
  ('voEF', '02:00:E0:CC:F5:1E', 57548, 62750, -65, 1789, 0.18183062, 0.32304169, 55.843266719170764,37.537475492800105,'voEF'),
  ('ecPP', '02:00:00:AE:7D:0B',   174, 32011, -65, 1791, 0.18255392, 0.59970084, 55.84347509232334, 37.53747616230846, 'ecPP'),
  ('jScZ', '02:00:15:39:44:3F',  5433, 17471, -65, 1792, 0.10804579, 0.31281399, 55.84325901590813, 37.537407195330125,'jScZ'),
  ('ZRXj', '02:00:25:D7:5D:D9',  9687, 24025, -65, 1793, 0.80706116, 0.29754919, 55.84324751882047, 37.53805422509554, 'ZRXj'),
  ('zl3V', '02:00:4F:95:3E:C6', 20373, 16070, -65, 1794, 0.21388981, 0.25254733, 55.843213624479944,37.537505167755874,'zl3V')
) AS v(bid, mac, major, minor, tx, nid, x, ky, lat, lon, descr)
ON CONFLICT (floor_id, mac) DO UPDATE SET
  beacon_id = EXCLUDED.beacon_id, major = EXCLUDED.major, minor = EXCLUDED.minor,
  tx_power = EXCLUDED.tx_power, numeric_id = EXCLUDED.numeric_id,
  x_norm = EXCLUDED.x_norm, y_norm = EXCLUDED.y_norm,
  latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude,
  description = EXCLUDED.description;

-- ig_f3
WITH f AS (
  SELECT pf.id FROM plan_floors pf
  JOIN plan_locations pl ON pl.id = pf.location_id
  WHERE pl.code = 'intellect_gnss' AND pf.code = 'ig_f3'
)
INSERT INTO plan_beacons (floor_id, beacon_id, mac, major, minor, tx_power, numeric_id, x_norm, y_norm, latitude, longitude, description)
SELECT f.id, v.bid, v.mac, v.major, v.minor, v.tx, v.nid, v.x, 1 - v.ky, v.lat, v.lon, v.descr FROM f, (VALUES
  ('njkD', '02:00:E3:E8:6F:7F', 58344, 28543, -65, 1761, 0.1044921875,    0.303417328271, 55.843261841926434, 37.53741295449138, 'njkD'),
  ('FVqr', '02:00:93:0A:9D:15', 37642, 40213, -65, 1764, 0.1865234375,    0.354504659993, 55.843300344814736, 37.53748908244872, 'FVqr'),
  ('aRVu', '02:00:FB:A7:02:D5', 64423,   725, -65, 1767, 0.18896484375,   0.518812564722, 55.843424178428464, 37.53749134816174, 'aRVu'),
  ('zy3u', '02:00:BE:91:6C:34', 48785, 27700, -65, 1769, 0.25,             0.282706247843, 55.843246232647395, 37.53754799098714, 'zy3u'),
  ('BDpe', '02:00:5B:6B:63:38', 23403, 25400, -65, 1772, 0.1689453125,    0.246807041767, 55.84321917656372,  37.53747276931501, 'BDpe'),
  ('2Ca7', '02:00:BE:E9:2E:9E', 48873, 11934, -65, 1773, 0.33203125,      0.279944770452, 55.84324415141019,  37.537624118944485,'2Ca7'),
  ('egx4', '02:00:EA:34:86:85', 59956, 34437, -65, 1774, 0.4404296875,    0.259233690024, 55.84322854213114,  37.537724716602405,'egx4'),
  ('5JEo', '02:00:00:26:29:19',    38, 10521, -65, 1776, 0.57275390625,   0.279254401105, 55.84324363110088,  37.53784751824788, '5JEo'),
  ('6qsL', '02:00:85:C5:47:E9', 34245, 18409, -65, 1778, 0.693359375,     0.259233690024, 55.84322854213114,  37.53795944447088, '6qsL'),
  ('Oxjm', '02:00:A5:3F:1D:86', 42303,  7558, -65, 1781, 0.8134765625,    0.282706247843, 55.843246232647395, 37.53807091755127, 'Oxjm'),
  ('wwl7', '02:00:85:56:8F:6F', 34134, 36719, -65, 1782, 0.814453125,     0.240593717639, 55.84321449378001,  37.53807182383648, 'wwl7'),
  ('hsu5', '02:00:9B:21:D9:A6', 39713, 55718, -65, 1783, 0.76318359375,   0.339316534346, 55.8432888980101,   37.53802424386314, 'hsu5'),
  ('st20', '02:00:71:DB:9D:AB', 29147, 40363, -65, 1785, 0.7646484375,    0.50224370038,  55.84341169100523,  37.53802560329095, 'st20'),
  ('j3Pm', '02:00:23:29:82:8E',  9001, 33422, -65, 1786, 0.7646484375,    0.658957542285, 55.84352980121664,  37.53802560329095, 'j3Pm'),
  ('Y2F2', '02:00:E8:44:7F:19', 59460, 32537, -65, 1790, 0.85107421875,   0.616845012081, 55.843498062349255, 37.53810580953172, 'Y2F2'),
  ('q3in', '02:00:C7:28:EF:CF', 50984, 61391, -65, 1795, 0.112223312259,  0.384880897242, 55.84332323841341,  37.537420129253874,'q3in')
) AS v(bid, mac, major, minor, tx, nid, x, ky, lat, lon, descr)
ON CONFLICT (floor_id, mac) DO UPDATE SET
  beacon_id = EXCLUDED.beacon_id, major = EXCLUDED.major, minor = EXCLUDED.minor,
  tx_power = EXCLUDED.tx_power, numeric_id = EXCLUDED.numeric_id,
  x_norm = EXCLUDED.x_norm, y_norm = EXCLUDED.y_norm,
  latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude,
  description = EXCLUDED.description;

-- ble_indoor
WITH f AS (
  SELECT pf.id FROM plan_floors pf
  JOIN plan_locations pl ON pl.id = pf.location_id
  WHERE pl.code = 'ble_lab' AND pf.code = 'ble_indoor'
)
INSERT INTO plan_beacons (floor_id, beacon_id, mac, major, minor, tx_power, numeric_id, x_norm, y_norm, latitude, longitude, description)
SELECT f.id, v.bid, v.mac, v.major, v.minor, v.tx, v.nid, v.x, 1 - v.ky, v.lat, v.lon, v.descr FROM f, (VALUES
  ('RightUgol',     '02:00:10:7E:C9:6F',  4222, 51567, -65, 1872, 0.875460116933, 0.710623631269, 55.69296436838312, 37.34710318493669, 'RightUgol'),
  ('BackLeftUgol',  '02:00:EE:12:66:34', 60946, 26164, -65, 1873, 0.572640245509, 0.084509469492, 55.69372159429248, 37.34763693357519, 'BackLeftUgol'),
  ('BackRightUgol', '02:00:96:93:D7:C7', 38547, 55239, -65, 1874, 0.919459611274, 0.074410850811, 55.69373380761743, 37.347025631670164,'BackRightUgol'),
  ('SZQN',          '02:00:C6:BD:CD:40', 50877, 52544, -65, 1875, 0.868989642988, 0.382152879074, 55.693361622716615,37.347114589758455,'SZQN'),
  ('egBO',          '02:00:00:01:00:1F',     1,    31, -65, 1876, 0.504052802078, 0.673418215367, 55.693009364817655,37.34775782542544, 'egBO'),
  ('Rl1Q',          '02:00:00:01:00:2F',     1,    47, -65, 1877, 0.502758701118, 0.880705568739, 55.69275867035291, 37.34776010640067, 'Rl1Q')
) AS v(bid, mac, major, minor, tx, nid, x, ky, lat, lon, descr)
ON CONFLICT (floor_id, mac) DO UPDATE SET
  beacon_id = EXCLUDED.beacon_id, major = EXCLUDED.major, minor = EXCLUDED.minor,
  tx_power = EXCLUDED.tx_power, numeric_id = EXCLUDED.numeric_id,
  x_norm = EXCLUDED.x_norm, y_norm = EXCLUDED.y_norm,
  latitude = EXCLUDED.latitude, longitude = EXCLUDED.longitude,
  description = EXCLUDED.description;

-- 4. Дать admin'у доступ ко всем приватным локациям (хотя серверная логика
--    и так пропустит admin, оставим записи чтобы /plans для admin не зависел от роли).
INSERT INTO user_plan_access (user_id, location_id)
SELECT u.id, pl.id
FROM users u CROSS JOIN plan_locations pl
WHERE u.role = 'admin' AND pl.is_public = false
ON CONFLICT DO NOTHING;
