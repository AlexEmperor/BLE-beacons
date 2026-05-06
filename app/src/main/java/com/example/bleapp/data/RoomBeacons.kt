package com.example.bleapp.data

// 📡 Координаты маяков для РАДАРА (вкладка «Позиция»).
// Тут позиции реальные в пространстве — некоторые маяки далеко, за пределами комнаты.
// Видны только при отдалении (zoom out).
val roomBeacons = listOf(
    // ближние — внутри комнаты
    BeaconPosition("B1", 0.25f, 0.20f),
    BeaconPosition("B2", 0.75f, 0.25f),
    BeaconPosition("B3", 0.70f, 0.75f),
    BeaconPosition("B4", 0.30f, 0.70f),
    // дальние — за стенами
    BeaconPosition("B5", -0.6f, 0.5f),
    BeaconPosition("B6", 1.6f, 0.4f),
    BeaconPosition("B7", 0.5f, -0.7f),
    BeaconPosition("B8", 0.5f, 1.7f)
)

// 🗺️ Координаты тех же маяков на ПЛАНЕ ЗДАНИЯ (вкладка «Карта»).
// Здесь все 8 маяков расположены в разных частях здания.
// Все координаты в пределах [0..1] — внутри плана.
val mapBeacons = listOf(
    // комнаты по углам
    BeaconPosition("B1", 0.20f, 0.18f),
    BeaconPosition("B2", 0.80f, 0.18f),
    BeaconPosition("B3", 0.80f, 0.82f),
    BeaconPosition("B4", 0.20f, 0.82f),
    // коридор / центр
    BeaconPosition("B5", 0.50f, 0.30f),
    BeaconPosition("B6", 0.50f, 0.70f),
    // боковые стены
    BeaconPosition("B7", 0.10f, 0.50f),
    BeaconPosition("B8", 0.90f, 0.50f)
)