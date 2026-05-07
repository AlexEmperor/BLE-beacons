package com.example.bleapp.data

// Реальные маяки, выгруженные из all_beacons.json (Navigine).
// kx, ky — нормализованные координаты на плане (Navigine: ky=0 — низ);
// lat/lon — реальные географические координаты маяка; level — этаж в здании.

data class RealBeacon(
    val id: String,
    val mac: String,
    val major: Int,
    val minor: Int,
    val kx: Float,
    val ky: Float,
    val lat: Double,
    val lon: Double,
    val level: Int = 1,
    val txPower: Int = -65,
    val description: String = "",
    val beaconId: Long = 0
)

val realBeaconsByFloor: Map<String, List<RealBeacon>> = mapOf(
    "ig_f1" to listOf(
        RealBeacon("i9h2", "02:00:70:8C:B3:15", 28812, 45845, 0.87469718f, 0.25178026f, 55.84322368256353, 37.538112610661074, level = 1, description = "i9h2", beaconId = 1760),
        RealBeacon("YSXI", "02:00:48:17:79:C0", 18455, 31168, 0.79440203f, 0.24973469f, 55.84322215981929, 37.53803880142772, level = 1, description = "YSXI", beaconId = 1763),
        RealBeacon("fvX6", "02:00:9F:66:DE:DF", 40806, 57055, 0.8443152f, 0.38269475f, 55.84332113671387, 37.53808468281423, level = 1, description = "fvX6", beaconId = 1765),
        RealBeacon("n7w1", "02:00:E7:49:7D:60", 59209, 32096, 0.24023438f, 0.26073584f, 55.84323034919333, 37.53752939719352, level = 1, description = "n7w1", beaconId = 1768),
        RealBeacon("76nq", "02:00:B4:9B:3B:FD", 46235, 15357, 0.11368109f, 0.25706384f, 55.84322761571715, 37.537413066365154, level = 1, description = "76nq", beaconId = 1771),
        RealBeacon("0wYF", "02:00:EF:A2:D4:3B", 61346, 54331, 0.15348763f, 0.3775809f, 55.843317329909084, 37.53744965749426, level = 1, description = "0wYF", beaconId = 1780),
    ),
    "ig_f2" to listOf(
        RealBeacon("61FF", "02:00:02:36:B0:EE", 566, 45294, 0.81284817f, 0.24947902f, 55.84321131350116, 37.538059581727005, level = 2, description = "61FF", beaconId = 1762),
        RealBeacon("7CsC", "02:00:D7:61:21:CB", 55137, 8651, 0.76438179f, 0.30266303f, 55.84325137044426, 37.53801471978, level = 2, description = "7CsC", beaconId = 1766),
        RealBeacon("kdiK", "02:00:EC:0B:3F:51", 60427, 16209, 0.75931814f, 0.48573875f, 55.843389258762556, 37.538010032712435, level = 2, description = "kdiK", beaconId = 1770),
        RealBeacon("D4cr", "02:00:DF:43:65:47", 57155, 25927, 0.76148828f, 0.6647234f, 55.843524065783335, 37.538012041459645, level = 2, description = "D4cr", beaconId = 1775),
        RealBeacon("cMEB", "02:00:CB:D3:27:4A", 52179, 10058, 0.70723479f, 0.26788888f, 55.843225179373896, 37.53796182278862, level = 2, description = "cMEB", beaconId = 1777),
        RealBeacon("V09o", "02:00:50:97:D8:CA", 20631, 55498, 0.59583431f, 0.26788888f, 55.843225179373896, 37.53785870713546, level = 2, description = "V09o", beaconId = 1779),
        RealBeacon("fFMG", "02:00:25:2B:9F:E0", 9515, 40928, 0.43741416f, 0.26584331f, 55.843223638698774, 37.53771206865384, level = 2, description = "fFMG", beaconId = 1784),
        RealBeacon("nWlS", "02:00:79:10:90:67", 30992, 36967, 0.31009934f, 0.26379777f, 55.84322209804625, 37.537594222206316, level = 2, description = "nWlS", beaconId = 1787),
        RealBeacon("aBNw", "02:00:1A:E8:60:5D", 6888, 24669, 0.18327734f, 0.47441153f, 55.84338072736737, 37.53747683192789, level = 2, description = "aBNw", beaconId = 1788),
        RealBeacon("voEF", "02:00:E0:CC:F5:1E", 57548, 62750, 0.18183062f, 0.32304169f, 55.843266719170764, 37.537475492800105, level = 2, description = "voEF", beaconId = 1789),
        RealBeacon("ecPP", "02:00:00:AE:7D:0B", 174, 32011, 0.18255392f, 0.59970084f, 55.84347509232334, 37.53747616230846, level = 2, description = "ecPP", beaconId = 1791),
        RealBeacon("jScZ", "02:00:15:39:44:3F", 5433, 17471, 0.10804579f, 0.31281399f, 55.84325901590813, 37.537407195330125, level = 2, description = "jScZ", beaconId = 1792),
        RealBeacon("ZRXj", "02:00:25:D7:5D:D9", 9687, 24025, 0.80706116f, 0.29754919f, 55.84324751882047, 37.53805422509554, level = 2, description = "ZRXj", beaconId = 1793),
        RealBeacon("zl3V", "02:00:4F:95:3E:C6", 20373, 16070, 0.21388981f, 0.25254733f, 55.843213624479944, 37.537505167755874, level = 2, description = "zl3V", beaconId = 1794),
    ),
    "ig_f3" to listOf(
        RealBeacon("njkD", "02:00:E3:E8:6F:7F", 58344, 28543, 0.1044921875f, 0.303417328271f, 55.843261841926434, 37.53741295449138, level = 3, description = "njkD", beaconId = 1761),
        RealBeacon("FVqr", "02:00:93:0A:9D:15", 37642, 40213, 0.1865234375f, 0.354504659993f, 55.843300344814736, 37.53748908244872, level = 3, description = "FVqr", beaconId = 1764),
        RealBeacon("aRVu", "02:00:FB:A7:02:D5", 64423, 725, 0.18896484375f, 0.518812564722f, 55.843424178428464, 37.53749134816174, level = 3, description = "aRVu", beaconId = 1767),
        RealBeacon("zy3u", "02:00:BE:91:6C:34", 48785, 27700, 0.25f, 0.282706247843f, 55.843246232647395, 37.53754799098714, level = 3, description = "zy3u", beaconId = 1769),
        RealBeacon("BDpe", "02:00:5B:6B:63:38", 23403, 25400, 0.1689453125f, 0.246807041767f, 55.84321917656372, 37.53747276931501, level = 3, description = "BDpe", beaconId = 1772),
        RealBeacon("2Ca7", "02:00:BE:E9:2E:9E", 48873, 11934, 0.33203125f, 0.279944770452f, 55.84324415141019, 37.537624118944485, level = 3, description = "2Ca7", beaconId = 1773),
        RealBeacon("egx4", "02:00:EA:34:86:85", 59956, 34437, 0.4404296875f, 0.259233690024f, 55.84322854213114, 37.537724716602405, level = 3, description = "egx4", beaconId = 1774),
        RealBeacon("5JEo", "02:00:00:26:29:19", 38, 10521, 0.57275390625f, 0.279254401105f, 55.84324363110088, 37.53784751824788, level = 3, description = "5JEo", beaconId = 1776),
        RealBeacon("6qsL", "02:00:85:C5:47:E9", 34245, 18409, 0.693359375f, 0.259233690024f, 55.84322854213114, 37.53795944447088, level = 3, description = "6qsL", beaconId = 1778),
        RealBeacon("Oxjm", "02:00:A5:3F:1D:86", 42303, 7558, 0.8134765625f, 0.282706247843f, 55.843246232647395, 37.53807091755127, level = 3, description = "Oxjm", beaconId = 1781),
        RealBeacon("wwl7", "02:00:85:56:8F:6F", 34134, 36719, 0.814453125f, 0.240593717639f, 55.84321449378001, 37.53807182383648, level = 3, description = "wwl7", beaconId = 1782),
        RealBeacon("hsu5", "02:00:9B:21:D9:A6", 39713, 55718, 0.76318359375f, 0.339316534346f, 55.8432888980101, 37.53802424386314, level = 3, description = "hsu5", beaconId = 1783),
        RealBeacon("st20", "02:00:71:DB:9D:AB", 29147, 40363, 0.7646484375f, 0.50224370038f, 55.84341169100523, 37.53802560329095, level = 3, description = "st20", beaconId = 1785),
        RealBeacon("j3Pm", "02:00:23:29:82:8E", 9001, 33422, 0.7646484375f, 0.658957542285f, 55.84352980121664, 37.53802560329095, level = 3, description = "j3Pm", beaconId = 1786),
        RealBeacon("Y2F2", "02:00:E8:44:7F:19", 59460, 32537, 0.85107421875f, 0.616845012081f, 55.843498062349255, 37.53810580953172, level = 3, description = "Y2F2", beaconId = 1790),
        RealBeacon("q3in", "02:00:C7:28:EF:CF", 50984, 61391, 0.112223312259f, 0.384880897242f, 55.84332323841341, 37.537420129253874, level = 3, description = "q3in", beaconId = 1795),
    ),
    "ble_indoor" to listOf(
        RealBeacon("RightUgol", "02:00:10:7E:C9:6F", 4222, 51567, 0.875460116933f, 0.710623631269f, 55.69296436838312, 37.34710318493669, level = 1, description = "RightUgol", beaconId = 1872),
        RealBeacon("BackLeftUgol", "02:00:EE:12:66:34", 60946, 26164, 0.572640245509f, 0.084509469492f, 55.69372159429248, 37.34763693357519, level = 1, description = "BackLeftUgol", beaconId = 1873),
        RealBeacon("BackRightUgol", "02:00:96:93:D7:C7", 38547, 55239, 0.919459611274f, 0.074410850811f, 55.69373380761743, 37.347025631670164, level = 1, description = "BackRightUgol", beaconId = 1874),
        RealBeacon("SZQN", "02:00:C6:BD:CD:40", 50877, 52544, 0.868989642988f, 0.382152879074f, 55.693361622716615, 37.347114589758455, level = 1, description = "SZQN", beaconId = 1875),
        RealBeacon("egBO", "02:00:00:01:00:1F", 1, 31, 0.504052802078f, 0.673418215367f, 55.693009364817655, 37.34775782542544, level = 1, description = "egBO", beaconId = 1876),
        RealBeacon("Rl1Q", "02:00:00:01:00:2F", 1, 47, 0.502758701118f, 0.880705568739f, 55.69275867035291, 37.34776010640067, level = 1, description = "Rl1Q", beaconId = 1877),
    ),
)