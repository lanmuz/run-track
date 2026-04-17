# RunTrack

RunTrack is a Fitness Tracking app utilizing modern Android/KMP technologies, including
Jetpack Compose, MVVM architecture, and **OmniMap-Compose (Gaode/高德地图)**. The app allows users to
track their running activities with real-time GPS, displaying routes on an interactive map
(Polyline + Markers) while storing statistics using Room. (Google Maps replaced for better China support and simpler Compose integration.)

## Features
1. Live tracking of running activity using GPS.
2. Tracking of user's running path in Map using OmniMap-Compose (GDMap) with Polyline, Markers, camera follow, and finish snapshot.
3. Using Foreground Service, even if the user closes the app and removes it from the background, this app still continues to track user running stats.
4. Room database to store and manage running statistics.
5. Handling nested navigation, Deep linking, conditional navigation to onboarding screen using Jetpack Navigation Component.
6. New Jetpack Compose image picker - helps to pick image without any permission.
7. Paging3 integration.
8. Dynamic color support in dark and light theme.
9. Weekly Statistics with filters in graph.

## Screenshot

|                                                                                                                         |                                                                                                               |                                                                                                                |
|-------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------|
| ![run_track_home_ss](https://github.com/user-attachments/assets/6a104b92-06ac-40fd-9b32-0b85d9544446)                   | ![runtrack_live_tracking_ss](https://github.com/user-attachments/assets/fd86f2cb-4114-41c5-ac43-06005cfcfbe9) | ![runtrack_running_info_ss](https://github.com/user-attachments/assets/78e422ba-b6dd-41a6-8ccc-045a99658aae)   |
| ![run_track_statistics_ss](https://github.com/sDevPrem/run-track/assets/130966261/b9d92744-7de1-461e-b96f-56950689e0a4) | ![runtrack_profile_ss](https://github.com/user-attachments/assets/0574c8e1-87a3-4858-9c09-5a347d4b5c02)       | ![runtrack_all_run_screen_ss](https://github.com/user-attachments/assets/58b6d8c6-bca2-40b4-ade8-e05e0e4ceec9) |

## Package Structure

* [`background`](app/src/main/java/com/sdevprem/runtrack/background): Handles background related
  process like service.
* [`data`](app/src/main/java/com/sdevprem/runtrack/data): Responsible for producing data. Contains
  entity, database and tracking related classes.
    * [`tracking`](app/src/main/java/com/sdevprem/runtrack/data/tracking): Classes that handles
      tracking like location tracking.
* [`di`](app/src/main/java/com/sdevprem/runtrack/di) : Hilt Modules.
* [`domain`](app/src/main/java/com/sdevprem/runtrack/domain): Contains common use case and
  interfaces.
* [`ui`](app/src/main/java/com/sdevprem/runtrack/ui): UI Layer of the app.
    * `nav`: Contains app navigation and destinations.
    * `screen`: Contains UI.
    * `theme`: Material3 theme.
    * `common`: UI utility classes and common components.
* [`common`](app/src/main/java/com/sdevprem/runtrack/common): Utility class used across the app.

## Build With

[Kotlin](https://kotlinlang.org/):
As the programming language.

[Jetpack Compose](https://developer.android.com/jetpack/compose) :
To build UI.

[Jetpack Navigation](https://developer.android.com/jetpack/compose/navigation) :
For navigation between screens and deep linking.

[Room](https://developer.android.com/jetpack/androidx/releases/room) :
To store and manage running statistics.

[OmniMap-Compose (Gaode)](https://github.com/TheMelody/OmniMap-Compose) :
Drop-in Jetpack Compose map library (GDMap) for Polyline (running track), Markers, camera, snapshot. Replaces Google Maps.

[Hilt](https://developer.android.com/training/dependency-injection/hilt-android) :
For injecting dependencies.

[Preferences DataStore](https://developer.android.com/topic/libraries/architecture/datastore) :
To store user related data.

[Coil](https://coil-kt.github.io/coil/compose/) :
To load image asynchronously.

[Vico](https://patrykandpatrick.com/vico/) :
To show graphs in statistics screen.

## Architecture

This app follows MVVM architecture, Uni Directional Flow (UDF) pattern and Single architecture
pattern.
HLD of tracking architecture is shown in the below image:
![tracking_architecture](https://github.com/sDevPrem/run-track/assets/130966261/932e9df7-cf34-4902-aa84-73a6431ca236)

## Installation

Simple clone this app and open in Android Studio.

### Amap (高德地图) Integration via OmniMap-Compose

The project has been updated to use [OmniMap-Compose](https://github.com/TheMelody/OmniMap-Compose) (GDMap 1.0.7) instead of Google Maps Compose. **Full replacement summary** (Android only; iOS remains Google for KMP):

**Changes made:**
- **Deps/Build**: Swapped `maps-compose`/`play-services-maps` for `gd-compose`; JVM target upgraded to 19 (library requirement); `AndroidManifest.xml` meta-data switched to Amap v2 key.
- **Init**: `RunTrackApp.kt` calls `GdMapUtils.setMapPrivacy(...)` (mandatory for map to render).
- **Map UI (`CurrentRunMap.android.kt`)**: `GDMap` + `MapUiSettings`/`rememberCameraPositionState` + `@GDMapComposable` content slot for Polyline (running track from PathPoints), Markers (start/current/finish with vector icons), camera follow via `CameraUpdateFactory`. 
- **Snapshot**: `MapUtils.kt` (renamed from GoogleMapUtils) fully ported — uses `AMap.getMapScreenShot` + `suspendCancellableCoroutine` + bounds/crop for finish image stored in Room. Uses internal `MapApplier` (suppressed) for map reference in composition.
- **Coords**: `LocationInfoExt.toLatLng()` updated to AMap `LatLng`.
- **Docs**: This section + top description updated; `local.properties` with your key.

`local.properties` has your key. 1. Verify key at [lbs.amap.com](https://lbs.amap.com/dev/key) (package+SHA1). 2. Privacy handled. 3. Sync/rebuild (JVM 19 required). 

**Status**: Compiles cleanly. 

**Fixed in this update (from Gaode reference sample)**:
- **White/blank map**: Added `MapProperties(isMyLocationEnabled = true, myLocationStyle = MyLocationStyle() with custom icon, colors, rotate type)` + `locationSource` + `myLocationButtonEnabled` to `GDMap`. This loads tiles, shows blue dot, and enables location.
- **Location & running feature**: `locationSource` (implements `LocationSource`) + `LaunchedEffect` on `lastLocationPoint` calls `onLocationUpdate` with AMapLocation (syncs our TrackingManager pathPoints/speed to Gaode blue dot and camera). Matches reference `LocationTrackingViewModel` + `LocationTrackingRepository` (AMapLocationClient, MyLocationStyle, activate/deactivate, handleLocationChange).
- **SHA1 mismatch prompt**: RunTrackApp now logs current SHA1 on start (update in Amap console if map white or no location; reference uses keystore.properties for signing).
- **Map positioning button + address popup**: Added floating button (ic_location_marker) in CurrentRunMap Box. Click centers map (CameraUpdateFactory.newLatLngZoom) and shows AlertDialog with current address (lat/lng; reference uses RegeocodeSearch/PoiSearchV2 for full street address — extendable). Matches sample's myLocationButton + onMapClick patterns.
- Permission/GPS dialog from reference can be added to `CurrentRunScreen.kt` if needed (already has commented LocationUtils checks; Manifest has permissions).

Test: Grant location permission, start run — blue dot appears/follows, click positioning button for center + address popup, Polyline draws track, camera follows, finish snapshot works. If still white, check logged SHA1 vs Amap console and rebuild.

The tracking, GPS, Room, and foreground service logic are untouched. See `CurrentRunMap.android.kt` (now includes reference patterns for button/address), `MapUtils.kt`, `DefaultLocationTrackingManager.kt`, RunTrackApp.kt for details. Reference folder `高德参考/gaode` (now in .gitignore) fully audited for location init, properties, client, button, and search.

## Project Status

These features are left to be implemented:

1. Profile menu implementation.
2. Unit Tests
3. Currently, we are storing (maps screenshot) bitmap directly into db in form of bytes
   which is not good. Save the image to the storage and store only its uri or id.
4. App Icon
5. Suitable markers for the start, end, and current position.
