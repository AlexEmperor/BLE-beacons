# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

An Android app for BLE beacon detection and indoor positioning, written in Kotlin with Jetpack Compose. Real BLE scanning is implemented via `BleBeaconScanner` (iBeacon parsing of Apple manufacturer data). The app flow is Splash → Welcome → Main, where Main has three tabs: **Сканер** (beacon list), **Маяки** (radar of current floor), **Корпус** (floor plan with location/floor selector).

## Build Commands

```powershell
# Build debug APK
./gradlew build

# Install on connected device/emulator
./gradlew installDebug

# Run unit tests
./gradlew test

# Run instrumentation tests (requires device)
./gradlew connectedAndroidTest

# Clean
./gradlew clean
```

Android SDK: `C:\Users\Sasha\AppData\Local\Android\Sdk`
Target SDK: 36, Min SDK: 29, Compile SDK: 36
Gradle: 9.4.1, Java: JDK 11

## Architecture

**Pattern**: MVVM — `MainViewModel` (an `AndroidViewModel`) is the single source of truth, exposing `StateFlow`s; composables observe via `collectAsState`.

### `MainViewModel` (`viewmodel/MainViewModel.kt`)
Owns a `BleBeaconScanner` and a derived view of the world per selected floor.

State:
- `selectedFloor: PlanFloor` — currently chosen floor (default: first floor of first location).
- `allBeacons` — every visible beacon (live + saved when toggle is on), sorted by RSSI.
- `currentBeacons` — beacons whose coordinates project onto the selected floor.
- `currentSeeds` — `BeaconSeed`s (positioned anchors) on the selected floor.
- `userPos: Offset` — estimated user position in normalized 0–1 floor coords.
- `isScanning: Boolean` — starts/stops/clears the scanner.
- `showSavedBeacons: Boolean` — merge hardcoded `seedsForFloor` with live data.

Positioning:
- Live beacons report GPS (`latitude`/`longitude` from iBeacon payload). `latLonToFloorOffset` projects them onto a floor using the floor's `refLat`/`refLon` and `widthMeters`/`heightMeters` (equirectangular, 111 320 m/deg, cosine-corrected longitude).
- `estimateUserPosition` is a weighted centroid with `weight = 1 / distance²`, distance derived from RSSI via `calculateDistance`.

### Data layer (`data/`)
- `Beacon.kt`: `Beacon` (id, mac, rssi, iBeacon fields `beaconId`/`latitude`/`longitude`/`major`/`minor`/`txPower`) and `BeaconPosition`.
- `PlanLocations.kt`: hierarchy `PlanLocation` → `PlanFloor` (asset path, SVG vs PNG flag, physical size in meters, reference GPS point). Three locations are defined: «Интеллект» (3 SVG floors), «BLE — Лаборатория» (PNG indoor + outdoor), «Niima_Lab (RTT)».
- `PlanNavGraph.kt`: navigation graph data for floor plans.
- `RoomBeacons.kt`: legacy hardcoded beacon positions for the radar.
- `RealBeacons.kt`: `seedsForFloor(floorId)` — saved beacon anchors per floor (used when "saved beacons" toggle is on).

### BLE (`ble/BleBeaconScanner.kt`)
Wraps `BluetoothLeScanner`. Filters by Apple company ID `0x004C`, parses iBeacon payloads via `parseIBeacon`, deduplicates by MAC, and emits sorted snapshots through an `onBeaconsChanged` callback. Requires `BLUETOOTH_SCAN` permission — `start()` is a no-op without it.

### Update flow (`update/UpdateManager.kt`)
Queries `api.github.com/repos/AlexEmperor/BLE-beacons/releases/latest`, compares `tag_name` to `BuildConfig.VERSION_NAME`, downloads `app-release.apk` to `cacheDir/updates`, and launches `ACTION_VIEW` via `FileProvider`. Prompts for `REQUEST_INSTALL_PACKAGES` if missing. `App.kt` checks once on `MainContent` entry and shows `UpdateDialog`.

### Utilities (`util/`)
- `Distance.kt`: `calculateDistance(rssi, txPower) = 10^((txPower - rssi) / 20)`.
- `RssiColor.kt`: RSSI → color (≥ -60 green → -70 lime → -80 yellow → -100 red → grey).
- `IBeaconParser.kt`: parses 25-byte iBeacon manufacturer data (Apple prefix `0x4C00`, type `0x0215`), extracts the GPS-extended fields stored in the UUID.
- `BeaconPalette.kt`: stable per-beacon colors.
- `PlanAssetLoader.kt`: loads SVG/PNG floor plans from `assets/plans/...`.

### UI (`ui/`)
- `App.kt`: stage machine (Splash → Welcome → Main), wires `MainViewModel`, hosts `SegmentedTabs`, `LegendMenuButton`, and the update dialog.
- `welcome/`: `SplashScreen.kt`, `WelcomeScreen.kt` (the "Start" button toggles scanning on if needed).
- `scan/`: `ScanScreen` + `BeaconCard` + `BeaconDetailsDialog`. Has scanning and "show saved beacons" toggles.
- `room/`: `RoomScreen` + `RadarView` — radar with distance circles, zoom/pan, pulsing user dot. Scale is `currentFloorMaxMeters()`.
- `map/`: `MapScreen` + `PlanView` — floor plan with location/floor picker, beacons, user position, zoom/pan.
- `common/`: `SegmentedTabs.kt` (top tab bar with icons + scan-active indicator), `LegendMenu.kt`.
- `theme/Theme.kt`: dark theme (`BgPrimary #0A0A0F`, `BgTertiary` background, accent cyan `#00E5FF`, green `#00FFA3`).

## Key Conventions

- All UI is Jetpack Compose with Material 3 — no XML layouts anywhere.
- Coordinate systems:
  - Floor coords are normalized 0–1 `Offset`, mapped to physical meters via `PlanFloor.widthMeters` / `heightMeters`.
  - Live beacons carry GPS; `latLonToFloorOffset` projects them per floor and rejects beacons more than ~15% outside the rectangle.
- A beacon with `latitude == 0 && longitude == 0` is treated as "no coordinates" and is excluded from positioning (`Beacon.hasCoordinates()`).
- User position is recomputed only when `estimateUserPosition` returns a non-null result, so it stays put when there's nothing to weigh.
- Trail visualization that existed in earlier versions has been removed — `MainViewModel` no longer simulates motion or keeps a movement history.
- No unit/instrumentation tests beyond the scaffolding in `app/src/test` and `app/src/androidTest`.
- Release flow: GitHub Releases on `AlexEmperor/BLE-beacons`, APK asset must be named `app-release.apk`, tag is `v<versionName>`.
