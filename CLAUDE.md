# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

An Android app for BLE beacon detection and indoor positioning, written in Kotlin with Jetpack Compose. Currently uses **simulated data** — real BLE scanning is not yet implemented. Three tabs: Scan (beacon list), Position/Radar, and Map (floor plan).

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

**Pattern**: MVVM — `MainViewModel` holds all state as `StateFlow`, UI composables observe it.

**`MainViewModel`** is the single source of truth:
- `beacons`: list of detected beacons with simulated RSSI
- `userPos`: user position in normalized coords (0–1, scaled to 6m world unit)
- `isScanning`: toggle state
- `trail`: movement history (capped at 60 points)
- Simulates user movement between waypoints every 200ms; RSSI is derived from distance

**Data layer** (`data/`):
- `Beacon.kt`: core data class (id, MAC, RSSI, iBeacon fields: major, minor, txPower)
- `RoomBeacons.kt`: 8 hardcoded beacon positions (4 inside room, 4 outside walls)

**Utilities** (`util/`):
- `Distance.kt`: RSSI↔distance via `10^((txPower - rssi) / 20)`
- `RssiColor.kt`: color mapping — ≥ -60 green → -70 lime → -80 yellow → -100 red → grey
- `IBeaconParser.kt`: parses 25-byte iBeacon manufacturer data (Apple prefix 0x4C00, type 0x0215)

**UI** (`ui/`):
- `App.kt`: tab navigation shell
- `scan/`: beacon list with signal cards and details dialog
- `room/`: interactive radar with distance circles, zoom/pan gestures, pulsing user dot
- `map/`: floor plan with grid, walls, beacon placement, zoom/pan
- `common/LegendMenu.kt`: legend popup
- `theme/Theme.kt`: dark theme (BgPrimary `#0A0A0F`, accent cyan `#00E5FF`, green `#00FFA3`)

## Key Conventions

- All UI is Jetpack Compose with Material 3 — no XML layouts anywhere.
- Coordinate system: normalized 0–1 float range internally, scaled to 6m for distance math.
- RSSI simulation: distance → RSSI inverse formula in `MainViewModel`; real scanning entry point is `MainActivity.kt` (currently unused).
- No tests are implemented yet beyond scaffolding placeholders.
