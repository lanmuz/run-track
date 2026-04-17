package com.sdevprem.runtrack.shared.common.extension

import com.amap.api.maps.model.LatLng
import com.sdevprem.runtrack.shared.domain.tracking.model.LocationInfo

fun LocationInfo.toLatLng() = LatLng(
    latitude,
    longitude
)