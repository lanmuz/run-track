package com.sdevprem.runtrack.shared.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.amap.api.maps.AMap
import com.amap.api.maps.CameraUpdateFactory
import com.amap.api.maps.model.BitmapDescriptor
import com.amap.api.maps.model.BitmapDescriptorFactory
import com.amap.api.maps.model.LatLngBounds
import com.sdevprem.runtrack.shared.common.extension.toGcjLatLng
import com.sdevprem.runtrack.shared.domain.tracking.model.PathPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object MapUtils {

    private const val MAP_SNAPSHOT_DELAY = 500L

    suspend fun takeSnapshot(
        context: Context,
        map: AMap,
        pathPoints: List<PathPoint>,
        mapCenter: Offset,
        onSnapshot: (ByteArray) -> Unit,
        snapshotSideLength: Float
    ) {
        val locationPoints = pathPoints.filterIsInstance<PathPoint.LocationPoint>()
        if (locationPoints.isEmpty()) return

        val boundsBuilder = LatLngBounds.Builder()
        locationPoints.forEach {
            boundsBuilder.include(it.locationInfo.toGcjLatLng(context))
        }
        map.moveCamera(
            CameraUpdateFactory
                .newLatLngBounds(
                    boundsBuilder.build(),
                    snapshotSideLength.toInt(),
                    snapshotSideLength.toInt(),
                    (snapshotSideLength * 0.2).toInt()
                )
        )

        //since move camera bounds the map in the specified LocationInfo
        //from the center withing the bounding box (of side snapshotSideLength)
        //so get the coordinate of the starting point of the box
        val startOffset = mapCenter - Offset(snapshotSideLength / 2, snapshotSideLength / 2)

        //A delay to load the icons and map properly before snapshot
        delay(MAP_SNAPSHOT_DELAY)

        val bitmap = suspendCancellableCoroutine<Bitmap?> { continuation ->
            map.getMapScreenShot(object : AMap.OnMapScreenShotListener {
                override fun onMapScreenShot(bitmap: Bitmap?) {
                    continuation.resume(bitmap)
                }

                override fun onMapScreenShot(bitmap: Bitmap?, status: Int) {
                    continuation.resume(bitmap)
                }
            })
        }

        bitmap?.let {
            //crop to get a square image which fits the user path
            val side = snapshotSideLength.toInt().coerceAtLeast(1)
            val safeX = startOffset.x.toInt().coerceIn(0, (it.width - 1).coerceAtLeast(0))
            val safeY = startOffset.y.toInt().coerceIn(0, (it.height - 1).coerceAtLeast(0))
            val safeWidth = side.coerceAtMost(it.width - safeX).coerceAtLeast(1)
            val safeHeight = side.coerceAtMost(it.height - safeY).coerceAtLeast(1)
            val croppedBitmap = Bitmap.createBitmap(it, safeX, safeY, safeWidth, safeHeight)
            onSnapshot(croppedBitmap.toByteArray())
        }
    }

    fun bitmapDescriptorFromVector(
        context: Context,
        @DrawableRes vectorResId: Int,
        tint: Int? = null,
        sizeInPx: Int? = null,
    ): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)!!
        tint?.let { vectorDrawable.setTint(it) }

        vectorDrawable.setBounds(
            0,
            0,
            sizeInPx ?: vectorDrawable.intrinsicWidth,
            sizeInPx ?: vectorDrawable.intrinsicHeight
        )

        val bitmap = createBitmap(
            sizeInPx ?: vectorDrawable.intrinsicWidth,
            sizeInPx ?: vectorDrawable.intrinsicHeight
        )

        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun Bitmap.toByteArray(): ByteArray {
        return java.io.ByteArrayOutputStream().use {
            compress(Bitmap.CompressFormat.PNG, 100, it)
            return@use it.toByteArray()
        }
    }
}