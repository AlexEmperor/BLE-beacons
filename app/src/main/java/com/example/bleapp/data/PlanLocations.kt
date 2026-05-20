package com.example.bleapp.data

data class PlanFloor(
    val id: String,
    val name: String,
    val assetPath: String,
    val isSvg: Boolean,
    val widthMeters: Float,
    val heightMeters: Float,
    /** Уровень в здании: 1 — первый этаж и т.д. */
    val level: Int = 1,
    /** Опорная GPS-точка центра плана (для расчёта дистанций между этажами/локациями). */
    val refLat: Double = 0.0,
    val refLon: Double = 0.0,
    /** Угол поворота плана относительно севера, по часовой стрелке (градусы). */
    val bearingDeg: Float = 0f,
    /** Если true — вместо растрового/SVG-плана отрисовываем настоящую карту (osmdroid). */
    val isWorldMap: Boolean = false,
    /** Если true — этаж отрисовывается как HTML/SVG в WebView (3D-просмотр). */
    val isHtml3d: Boolean = false
)

/** Локальная встроенная локация с 3D-просмотром из ассетов (HTML+SVG в WebView). */
val intellect3dLocation: PlanLocation = PlanLocation(
    id = "intellect_3d",
    name = "Интеллект-3D",
    floors = listOf(
        PlanFloor("intellect_3d_f1", "1 этаж", "intellect_3d/index.html", false, 83.33f, 59f, 1, 55.84341, 37.53777, isHtml3d = true),
        PlanFloor("intellect_3d_f2", "2 этаж", "intellect_3d/index.html", false, 83.04f, 59f, 2, 55.84340, 37.53777, isHtml3d = true),
        PlanFloor("intellect_3d_f3", "3 этаж", "intellect_3d/index.html", false, 83.20f, 59f, 3, 55.84341, 37.53778, isHtml3d = true)
    )
)

data class PlanLocation(
    val id: String,
    val name: String,
    val floors: List<PlanFloor>
)

val planLocations: List<PlanLocation> = listOf(
    PlanLocation(
        id = "intellect_gnss",
        name = "Интеллект",
        floors = listOf(
            PlanFloor("ig_f1", "1 этаж", "plans/intellect_gnss/floor1.svg", true, 83.33f, 59.00f, 1, 55.84341, 37.53777),
            PlanFloor("ig_f2", "2 этаж", "plans/intellect_gnss/floor2.svg", true, 83.04f, 59.00f, 2, 55.84340, 37.53777),
            PlanFloor("ig_f3", "3 этаж", "plans/intellect_gnss/floor3.svg", true, 83.20f, 59.00f, 3, 55.84341, 37.53778)
        )
    ),
    PlanLocation(
        id = "ble_lab",
        name = "BLE — Лаборатория",
        floors = listOf(
            PlanFloor("ble_indoor", "NIIMA-Lab (BLE)", "plans/ble_lab/niima_lab_ble.png", false, 15.45f, 18.81f, 1, 55.84364764, 37.5383035),
            PlanFloor("ble_outdoor", "Улица", "plans/ble_lab/outdoor.png", false, 245.91f, 155.68f, 1, 55.84340244, 37.53807038)
        )
    ),
    PlanLocation(
        id = "niima_rtt",
        name = "Niima_Lab (RTT)",
        floors = listOf(
            PlanFloor("niima_f1", "1 этаж", "plans/niima_rtt/niima_lab.png", false, 15.98f, 19.43f, 1, 55.84364841, 37.5383073)
        )
    ),
    PlanLocation(
        id = "moscow",
        name = "Москва",
        floors = listOf(
            PlanFloor(
                id = "moscow_map",
                name = "Карта",
                assetPath = "",
                isSvg = false,
                widthMeters = 50_000f,
                heightMeters = 50_000f,
                level = 1,
                refLat = 55.7558,
                refLon = 37.6173,
                isWorldMap = true
            )
        )
    )
)
