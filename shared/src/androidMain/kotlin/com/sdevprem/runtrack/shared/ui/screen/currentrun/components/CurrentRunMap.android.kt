@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.sdevprem.runtrack.shared.ui.screen.currentrun.components

import android.net.ConnectivityManager //▲▲▲▲▲▲▲▲
import android.net.NetworkCapabilities //▲▲▲▲▲▲▲▲
import android.content.Context //▲▲▲▲▲▲▲▲
import android.content.Intent
import android.location.Geocoder
import android.provider.Settings
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.amap.api.location.AMapLocation
import com.amap.api.location.AMapLocationClient
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.LocationSource
import com.amap.api.maps.LocationSource.OnLocationChangedListener
import com.amap.api.maps.model.LatLng
import com.amap.api.maps.model.MyLocationStyle
import com.melody.map.gd_compose.GDMap
import com.melody.map.gd_compose.MapApplier
import com.melody.map.gd_compose.model.GDMapComposable
import com.melody.map.gd_compose.overlay.Marker
import com.melody.map.gd_compose.overlay.Polyline
import com.melody.map.gd_compose.overlay.rememberMarkerState
import com.melody.map.gd_compose.poperties.MapProperties
import com.melody.map.gd_compose.poperties.MapUiSettings
import com.melody.map.gd_compose.position.rememberCameraPositionState
import com.melody.map.gd_compose.utils.MapUtils
import com.sdevprem.runtrack.shared.R
import com.sdevprem.runtrack.shared.common.extension.toGcjLatLng
import com.sdevprem.runtrack.shared.domain.tracking.model.LocationInfo
import com.sdevprem.runtrack.shared.domain.tracking.model.PathPoint
import com.sdevprem.runtrack.shared.domain.tracking.model.firstLocationPoint
import com.sdevprem.runtrack.shared.domain.tracking.model.lasLocationPoint
import com.sdevprem.runtrack.shared.ui.theme.RTColor
import com.sdevprem.runtrack.shared.ui.theme.md_theme_light_primary
import com.sdevprem.runtrack.shared.ui.utils.MapUtils as AppMapUtils
import org.jetbrains.compose.resources.vectorResource
import runtrack.shared.generated.resources.Res
import runtrack.shared.generated.resources.ic_location_marker
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
actual fun Map(
    modifier: Modifier,
    pathPoints: List<PathPoint>,
    isRunningFinished: Boolean,
    currentSpeedInKMH: Float,
    onSnapshot: (ByteArray) -> Unit,
) {
    var mapSize by remember { mutableStateOf(Size(0f, 0f)) }
    var mapCenter by remember { mutableStateOf(Offset(0f, 0f)) }
    var isMapLoaded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                val rect = it.boundsInRoot()
                mapSize = rect.size
                mapCenter = rect.center
            }
    ) {
        ShowMapLoadingProgressBar(!isMapLoaded)
        RenderMapContent(
            pathPoints = pathPoints,
            isRunningFinished = isRunningFinished,
            currentSpeedInKMH = currentSpeedInKMH,
            mapCenter = mapCenter,
            mapSize = mapSize,
            onMapLoaded = { isMapLoaded = true },
            onSnapshot = onSnapshot
        )
    }
}

@Composable
private fun RenderMapContent(
    pathPoints: List<PathPoint>,
    isRunningFinished: Boolean,
    currentSpeedInKMH: Float,
    mapCenter: Offset,
    mapSize: Size,
    onMapLoaded: () -> Unit,
    onSnapshot: (ByteArray) -> Unit,
) {
    val context = LocalContext.current

    val mapUiSettings = remember {
        MapUiSettings(
            isCompassEnabled = true,
            isScaleControlsEnabled = true,
            isScrollGesturesEnabled = true,
            isZoomGesturesEnabled = true,
            isZoomEnabled = true,
            myLocationButtonEnabled = true
        )
    }

    val mapProperties = remember {
        MapProperties(
            isMyLocationEnabled = true,
            myLocationStyle = MyLocationStyle().apply {
                myLocationIcon(
                    AppMapUtils.bitmapDescriptorFromVector( //▲▲▲▲▲▲▲▲
                        context = context,
                        vectorResId = R.drawable.ic_location_marker,
                        tint = Color.Blue.toArgb(),
                        sizeInPx = 48
                    )
                )
                myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE)
                strokeColor(Color.Black.toArgb())
                radiusFillColor(Color(0x640000B4).toArgb())
                strokeWidth(0.1f)
            }
        )
    }

    val cameraPositionState = rememberCameraPositionState()
    val lastLocationPoint by remember(pathPoints) {
        derivedStateOf { pathPoints.lasLocationPoint() }
    }

    var showAddressDialog by remember { mutableStateOf(false) }
    var currentAddress by remember { mutableStateOf("") }
    var isNetworkAvailable by remember { mutableStateOf(true) }
    var locationError by remember { mutableStateOf<String?>(null) }
    var showGpsDialog by remember { mutableStateOf(false) } 


    // LocationSource for Gaode blue dot + live location updates (sync with our tracking)
    val locationSource = remember {
        object : LocationSource {
            private var listener: OnLocationChangedListener? = null
            override fun activate(l: OnLocationChangedListener?) {
                listener = l
            }
            override fun deactivate() {
                listener = null
            }
            fun onLocationUpdate(latLng: LatLng, speedInMS: Float = 0f) {
                val amapLocation = AMapLocation("run-track").apply {
                    latitude = latLng.latitude
                    longitude = latLng.longitude
                    this.speed = speedInMS
                    accuracy = 10f
                    time = System.currentTimeMillis()
                }
                listener?.onLocationChanged(amapLocation)
            }
        }
    }

    //▲▲▲▲▲▲▲▲ 新增：隐私检查 + 错误状态（屏幕打印高德错误）
    //▲▲▲▲▲▲▲▲ 参考 LocationTrackingActivity 的 GPS 提示逻辑
    var locationError by remember { mutableStateOf<String?>(null) }
    var showGpsDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        MapUtils.setMapPrivacy(context, true) //▲▲▲▲▲▲▲▲
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager //▲▲▲▲▲▲▲▲
        val network = cm.activeNetwork //▲▲▲▲▲▲▲▲
        val caps = network?.let { cm.getNetworkCapabilities(it) } //▲▲▲▲▲▲▲▲
        isNetworkAvailable = caps?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true //▲▲▲▲▲▲▲▲
        if (!isNetworkAvailable) locationError = "网络连接异常.详细:#id:ELA==网络异常.未连接到网络.请检查互联网连接" //▲▲▲▲▲▲▲▲
    } //▲▲▲▲▲▲▲▲

    //▲▲▲▲▲▲▲▲ 新增：AMapLocationClient 持续定位 + 错误报告
    //▲▲▲▲▲▲▲▲ 高德错误会在屏幕上打印
    val locationClient = remember { AMapLocationClient(context) }
    LaunchedEffect(Unit) {
        locationClient.setLocationListener { loc ->
            if (loc.errorCode != 0) {
                val msg = when (loc.errorCode) {
                    12 -> "缺少定位权限"
                    13 -> "定位服务未开启"
                    else -> "高德错误(${loc.errorCode}): ${loc.errorInfo}"
                }
                locationError = msg
                Log.e("GaodeMap", msg)
                if (loc.errorCode == 13) showGpsDialog = true
                if (loc.errorCode == 4 || (loc.errorInfo?.contains("网络") == true)) { //▲▲▲▲▲▲▲▲
                    locationError = "网络连接异常.详细:#id:ELA==网络异常.未连接到网络.请确保设备已连接互联网、高德Key有效且Manifest权限完整" //▲▲▲▲▲▲▲▲
                }
            }
        }
        locationClient.startLocation()
    }

    LaunchedEffect(lastLocationPoint) {
        lastLocationPoint?.let {
            val latLng = it.locationInfo.toGcjLatLng(context)
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(latLng, 15f)
            )
            locationSource.onLocationUpdate(
                latLng = latLng,
                speedInMS = currentSpeedInKMH / 3.6f
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GDMap(
            modifier = Modifier.fillMaxSize(),
            uiSettings = mapUiSettings,
            properties = mapProperties,
            cameraPositionState = cameraPositionState,
            locationSource = locationSource,
            onMapLoaded = onMapLoaded,
        ) {
            DrawPathPoints(pathPoints = pathPoints, isRunningFinished = isRunningFinished)

            TakeScreenShot(
                take = isRunningFinished,
                context = context,
                mapCenter = mapCenter,
                mapSize = mapSize,
                pathPoints = pathPoints,
                onSnapshot = onSnapshot
            )
        }

        //▲▲▲▲▲▲▲▲ 新增：屏幕错误提示卡片
        locationError?.let { error ->
            Card(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        // Reference-inspired positioning button (click to locate + popup address)
        FloatingActionButton(
            onClick = {
                lastLocationPoint?.let {
                    val latLng = it.locationInfo.toGcjLatLng(context)
                    cameraPositionState.move(CameraUpdateFactory.newLatLngZoom(latLng, 18f))
                    currentAddress = "正在解析地址..."
                    showAddressDialog = true
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(
                imageVector = vectorResource(Res.drawable.ic_location_marker), // or ic_run
                contentDescription = "定位",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }

    if (showAddressDialog) {
        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = { Text("当前位置") },
            text = { Text(currentAddress) },
            confirmButton = {
                TextButton(onClick = { showAddressDialog = false }) {
                    Text("确定")
                }
            }
        )
    }

    //▲▲▲▲▲▲▲▲ 新增：GPS 对话框（参考 gd_map_location_gps_no_open）
    if (showGpsDialog) {
        AlertDialog(
            onDismissRequest = { showGpsDialog = false },
            title = { Text("定位服务未开启") },
            text = { Text("定位失败，打开定位服务来获取位置信息") },
            confirmButton = {
                TextButton(onClick = {
                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    context.startActivity(intent)
                    showGpsDialog = false
                }) {
                    Text("开启定位")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    LaunchedEffect(showAddressDialog, lastLocationPoint) {
        if (!showAddressDialog) return@LaunchedEffect
        val point = lastLocationPoint ?: return@LaunchedEffect
        val latLng = point.locationInfo.toGcjLatLng(context)
        currentAddress = resolveAddress(context, latLng)
    }
}

@GDMapComposable
@Composable
private fun TakeScreenShot(
    take: Boolean,
    context: android.content.Context,
    mapCenter: Offset,
    mapSize: Size,
    pathPoints: List<PathPoint>,
    onSnapshot: (ByteArray) -> Unit
) {
    if (!take) return

    val mapApplier = currentComposer.applier as? MapApplier
    LaunchedEffect(take, mapApplier, mapCenter, mapSize) {
        if (take && mapApplier?.map != null) {
            AppMapUtils.takeSnapshot(
                context = context,
                mapApplier.map,
                pathPoints,
                mapCenter,
                onSnapshot,
                snapshotSideLength = mapSize.width / 2f
            )
        }
    }
}

@Composable
@GDMapComposable
private fun DrawPathPoints(
    pathPoints: List<PathPoint>,
    isRunningFinished: Boolean,
) {
    val context = LocalContext.current
    val lastMarkerState = rememberMarkerState() //▲▲▲▲▲▲▲▲
    val largeLastMarkerState = rememberMarkerState() //▲▲▲▲▲▲▲▲
    val lastLocationPoint by remember(pathPoints) {
        derivedStateOf { pathPoints.lasLocationPoint() }
    }
    val firstLocationPoint by remember(pathPoints) {
        derivedStateOf { pathPoints.firstLocationPoint() }
    }
    val density = LocalDensity.current
    val largeLocationIconSize = remember { with(density) { 32.dp.toPx().toInt() } }
    val smallLocationIconSize = remember { with(density) { 16.dp.toPx().toInt() } }
    val flagSize = remember { with(density) { 32.dp.toPx().toInt() } }
    val flagOffset = remember { Offset(0.5f, 0.8f) }

    LaunchedEffect(key1 = lastLocationPoint) {
        pathPoints.lasLocationPoint()?.let {
            val latLng = it.locationInfo.toGcjLatLng(context)
            lastMarkerState.position = latLng
            largeLastMarkerState.position = latLng
        }
    }

    val locationInfoList = mutableListOf<LocationInfo>()
    pathPoints.fastForEach { pathPoint ->
        if (pathPoint is PathPoint.EmptyLocationPoint) {
            Polyline(
                points = locationInfoList.map { it.toGcjLatLng(context) },
                color = md_theme_light_primary,
            )
            locationInfoList.clear()
        } else if (pathPoint is PathPoint.LocationPoint) {
            locationInfoList += pathPoint.locationInfo
        }
    }

    //add the last path points
    if (locationInfoList.isNotEmpty()) {
        Polyline(
            points = locationInfoList.map { it.toGcjLatLng(context) },
            color = md_theme_light_primary
        )
    }

    val currentPosIcon = remember(isRunningFinished) {
        if (isRunningFinished.not()) {
            AppMapUtils.bitmapDescriptorFromVector( //▲▲▲▲▲▲▲▲
                context = context,
                vectorResId = R.drawable.ic_circle,
                tint = md_theme_light_primary.toArgb(),
                sizeInPx = smallLocationIconSize
            )
        } else {
            AppMapUtils.bitmapDescriptorFromVector( //▲▲▲▲▲▲▲▲
                context = context,
                vectorResId = R.drawable.ic_location_marker,
                tint = Color.Red.toArgb(),
                sizeInPx = flagSize
            )
        }
    }
    val currentPosLargeIcon = remember(isRunningFinished) {
        if (isRunningFinished) {
            null
        } else {
            AppMapUtils.bitmapDescriptorFromVector( //▲▲▲▲▲▲▲▲
                context = context,
                vectorResId = R.drawable.ic_circle,
                tint = md_theme_light_primary.copy(alpha = 0.4f).toArgb(),
                sizeInPx = largeLocationIconSize
            )
        }
    }

    currentPosLargeIcon?.let {
        Marker(
            icon = currentPosLargeIcon,
            state = largeLastMarkerState,
            anchor = Offset(0.5f, 0.5f),
            visible = lastLocationPoint != null
        )
    }

    Marker(
        icon = currentPosIcon,
        state = lastMarkerState,
        anchor = if (isRunningFinished) flagOffset else Offset(0.5f, 0.5f),
        visible = lastLocationPoint != null
    )

    firstLocationPoint?.let {
        val firstLocationIcon = remember(isRunningFinished) {
            AppMapUtils.bitmapDescriptorFromVector( //▲▲▲▲▲▲▲▲
                context = context,
                vectorResId = R.drawable.ic_location_marker,
                tint = RTColor.CHATEAU_GREEN.toArgb(),
                sizeInPx = flagSize
            )
        }
        Marker(
            icon = firstLocationIcon,
            state = rememberMarkerState(position = it.locationInfo.toGcjLatLng(context)),
            anchor = flagOffset,
        )
    }
}

private suspend fun resolveAddress(
    context: android.content.Context,
    latLng: LatLng
): String = withContext(Dispatchers.IO) {
    runCatching {
        val geocoder = Geocoder(context, Locale.getDefault())
        val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
        val line = addresses?.firstOrNull()?.getAddressLine(0)?.takeIf { it.isNotBlank() }
        line ?: "当前位置:\n纬度: ${latLng.latitude}\n经度: ${latLng.longitude}"
    }.getOrDefault("当前位置:\n纬度: ${latLng.latitude}\n经度: ${latLng.longitude}")
}

@Composable
private fun ShowMapLoadingProgressBar(
    visible: Boolean = false
) {
    AnimatedVisibility(
        modifier = Modifier
            .fillMaxSize(),
        visible = visible,
        enter = EnterTransition.None,
        exit = fadeOut(),
    ) {
        CircularProgressIndicator(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .wrapContentSize()
        )
    }
}