package com.sdevprem.runtrack.shared.ui.screen.currentrun.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.util.fastForEach
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.CameraPosition
import com.amap.api.maps.model.LatLng
import com.melody.map.gd_compose.GDMap
import com.melody.map.gd_compose.MapApplier
import com.melody.map.gd_compose.model.GDMapComposable
import com.melody.map.gd_compose.overlay.Marker
import com.melody.map.gd_compose.overlay.Polyline
import com.melody.map.gd_compose.overlay.rememberMarkerState
import com.melody.map.gd_compose.poperties.MapUiSettings
import com.melody.map.gd_compose.position.rememberCameraPositionState
import com.sdevprem.runtrack.shared.R
import com.sdevprem.runtrack.shared.common.extension.toLatLng
import com.sdevprem.runtrack.shared.domain.tracking.model.LocationInfo
import com.sdevprem.runtrack.shared.domain.tracking.model.PathPoint
import com.sdevprem.runtrack.shared.domain.tracking.model.firstLocationPoint
import com.sdevprem.runtrack.shared.domain.tracking.model.lasLocationPoint
import com.sdevprem.runtrack.shared.ui.theme.RTColor
import com.sdevprem.runtrack.shared.ui.theme.md_theme_light_primary
import com.sdevprem.runtrack.shared.ui.utils.MapUtils

@Composable
actual fun Map(
    modifier: Modifier,
    pathPoints: List<PathPoint>,
    isRunningFinished: Boolean,
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
        Map(
            pathPoints = pathPoints,
            isRunningFinished = isRunningFinished,
            mapCenter = mapCenter,
            mapSize = mapSize,
            onMapLoaded = { isMapLoaded = true },
            onSnapshot = onSnapshot
        )
    }
}

@Composable
private fun Map(
    pathPoints: List<PathPoint>,
    isRunningFinished: Boolean,
    mapCenter: Offset,
    mapSize: Size,
    onMapLoaded: () -> Unit,
    onSnapshot: (ByteArray) -> Unit,
) {

    val mapUiSettings = remember {
        MapUiSettings(
            isCompassEnabled = true,
            isZoomControlsEnabled = false,
            isScaleControlsEnabled = true,
            isScrollGesturesEnabled = true,
            isZoomGesturesEnabled = true
        )
    }
    val cameraPositionState = rememberCameraPositionState()
    val lastLocationPoint by remember(pathPoints) {
        derivedStateOf { pathPoints.lasLocationPoint() }
    }

    LaunchedEffect(key1 = lastLocationPoint) {
        lastLocationPoint?.let {
            val latLng = it.locationInfo.toLatLng()
            cameraPositionState.move(
                CameraUpdateFactory.newLatLngZoom(latLng, 15f)
            )
        }
    }

    GDMap(
        modifier = Modifier
            .fillMaxSize(),
        uiSettings = mapUiSettings,
        cameraPositionState = cameraPositionState,
        onMapLoaded = onMapLoaded,
    ) {
        DrawPathPoints(pathPoints = pathPoints, isRunningFinished = isRunningFinished)

        TakeScreenShot(
            take = isRunningFinished,
            mapCenter = mapCenter,
            mapSize = mapSize,
            pathPoints = pathPoints,
            onSnapshot = onSnapshot
        )
    }
}

@OptIn(MapsComposeExperimentalApi::class)
@GDMapComposable
@Composable
private fun TakeScreenShot(
    take: Boolean,
    mapCenter: Offset,
    mapSize: Size,
    pathPoints: List<PathPoint>,
    onSnapshot: (ByteArray) -> Unit
) {
    if (!take) return

    val mapApplier = currentComposer.applier as? MapApplier
    LaunchedEffect(take, mapApplier, mapCenter, mapSize) {
        if (take && mapApplier?.map != null) {
            MapUtils.takeSnapshot(
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
    val lastMarkerState = rememberMarkerState()
    val largeLastMarkerState = rememberMarkerState()
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
            val latLng = it.locationInfo.toLatLng()
            lastMarkerState.position = latLng
            largeLastMarkerState.position = latLng
        }
    }

    val locationInfoList = mutableListOf<LocationInfo>()
    pathPoints.fastForEach { pathPoint ->
        if (pathPoint is PathPoint.EmptyLocationPoint) {
            Polyline(
                points = locationInfoList.map { it.toLatLng() },
                color = md_theme_light_primary,
            )
            locationInfoList.clear()
        } else if (pathPoint is PathPoint.LocationPoint) {
            locationInfoList += pathPoint.locationInfo
        }
    }

    //add the last path points
    if (locationInfoList.isNotEmpty())
        Polyline(
            points = locationInfoList.map { it.toLatLng() },
            color = md_theme_light_primary
        )

    val currentPosIcon = remember(isRunningFinished) {
        if (isRunningFinished.not()) {
            MapUtils.bitmapDescriptorFromVector(
                context = context,
                vectorResId = R.drawable.ic_circle,
                tint = md_theme_light_primary.toArgb(),
                sizeInPx = smallLocationIconSize
            )
        } else {
            MapUtils.bitmapDescriptorFromVector(
                context = context,
                vectorResId = R.drawable.ic_location_marker,
                tint = Color.Red.toArgb(),
                sizeInPx = flagSize
            )
        }
    }
    val currentPosLargeIcon = remember(isRunningFinished) {
        if (isRunningFinished) return@remember null

        GoogleMapUtils.bitmapDescriptorFromVector(
            context = context,
            vectorResId = R.drawable.ic_circle,
            tint = md_theme_light_primary.copy(alpha = 0.4f).toArgb(),
            sizeInPx = largeLocationIconSize
        )
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
            MapUtils.bitmapDescriptorFromVector(
                context = context,
                vectorResId = R.drawable.ic_location_marker,
                tint = RTColor.CHATEAU_GREEN.toArgb(),
                sizeInPx = flagSize
            )
        }
        Marker(
            icon = firstLocationIcon,
            state = rememberMarkerState(position = it.locationInfo.toLatLng()),
            anchor = flagOffset,

            )
    }
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