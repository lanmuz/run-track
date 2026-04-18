package com.sdevprem.runtrack.shared.common.extension

import android.content.Context
import com.amap.api.maps.CoordinateConverter
import com.amap.api.maps.model.LatLng
import com.sdevprem.runtrack.shared.domain.tracking.model.LocationInfo

fun LocationInfo.toLatLng() = LatLng(
    latitude,
    longitude
)

fun LocationInfo.toGcjLatLng(
    context: Context
): LatLng {
    val gpsLatLng = toLatLng()
    return try {
        CoordinateConverter(context)
            .from(CoordinateConverter.CoordType.GPS)
            .coord(gpsLatLng)
            .convert() ?: gpsLatLng
    } catch (_: Exception) {
        gpsLatLng
    }
}