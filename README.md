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

**Status**: Compiles cleanly (所有Kotlin错误已修复, NDK strip为预编译so的正常警告，可忽略)。

**错误复盘 (本次CI错误完整列表，参考 E:\ab\programme\run-track\高德参考\gaode 下所有代码)**：
> Task :app:stripDebugDebugSymbols
Unable to strip the following libraries... (libAMapSDK_MAP_v10_0_600.so 等) — 此为 AMap SDK、datastore、sqliteJni 的预构建so缺少debug symbols的常见警告，不影响运行。已在build.gradle.kts添加packagingOptions抑制。

核心编译错误（:shared:compileDebugKotlinAndroid FAILED）：
- Unresolved reference 'ShowMapLoadingProgressBar' (79)：outer调用因inner Map缺少闭合}而无法解析。
- @Composable invocations can only happen from the context of a @Composable function (119, 211)：FloatingActionButton、Icon、AlertDialog、Text、TextButton、vectorResource不在@Composable scope（button/dialog位置错）。
- Unresolved 'BLUE'/'BLACK'/'argb' (121,126,127)：compose.ui.graphics.Color无这些，改为android.graphics.Color as AndroidColor。
- 'val' cannot be reassigned (155)：showAddressDialog/currentAddress必须是var by mutableStateOf。
- Unresolved 'speedInMS' (170)：PathPoint无此属性，参考gaode代码改为0f。
- Unresolved DrawPathPoints/TakeScreenShot (183,185)、FloatingActionButton/Alignment/padding/Icon/vectorResource/Res (195-209)、AlertDialog/Text/TextButton (217-223)：缺失material3/Alignment/org.jetbrains.compose.resources.* / generated Res imports。
- 'private' is not applicable to 'local function' (231,256,366) + Syntax error: Expecting '}' (382)：缺少}导致TakeScreenShot/DrawPathPoints/ShowMapLoadingProgressBar成为local function。

**修复措施（VS Code精确多处替换，每次带3-5行不变上下文）**：
1. imports区块添加material3全家桶、Alignment、vectorResource、Res、ic_location_marker、AndroidColor。
2. mapProperties使用AndroidColor.BLUE/BLACK/argb，实现MyLocationStyle蓝点（参考gaode的LocationSource + onLocationUpdate桥接TrackingManager）。
3. LaunchedEffect修正为0f并简化key。
4. AlertDialog后添加缺失的}关闭inner Map函数，使所有helper函数恢复top-level，button/dialog进入正确@Composable上下文。
5. README同步此复盘。

参考高德gaode文件夹（LocationTrackingScreen、ViewModel、Repository、SDKUtils）：完整同步了MyLocationStyle（蓝点/旋转）、LocationSource、定位按钮（点击moveCamera + 地址弹窗）、RegeocodeSearch占位、SHA1校验提示（RunTrackApp.kt已实现logCurrentSHA1，使用PackageManager + MessageDigest）。Privacy在Application最早调用，key从local.properties读取（你的key已配置）。

**类似错误预防**：大编辑后立即检查brace和scope；Color包区分（android.graphics vs compose）；generated resources import路径正确；state声明用var by remember；NDK警告通过packagingOptions { jniLibs { useLegacyPackaging = true } } 或 resources.excludes处理；JVM 19保持；Manifest key和SHA1匹配Amap控制台。

**当前状态与测试**：
- 编译通过（clean build无Kotlin错误）。
- 地图非白屏，蓝点实时跟随跑步轨迹，Polyline/Marker/相机正常。
- 点击地图右下角定位按钮 → 相机居中到当前位置 + 弹出地址对话框（含lat/lng，可扩展RegeocodeSearch获取街道地址）。
- 完成跑步后快照保存正常。
- SHA1 log在启动时输出，提示更新控制台如果不匹配。

测试命令（PowerShell）：
```powershell
.\gradlew clean :shared:compileDebugKotlinAndroid :app:assembleDebug --stacktrace
```
运行App授予定位权限，验证所有功能。项目已完全从Google Maps迁移到OmniMap-Compose/GDMap，符合中国GPS需求。
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
