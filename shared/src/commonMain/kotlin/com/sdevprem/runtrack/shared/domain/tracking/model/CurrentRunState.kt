package com.sdevprem.runtrack.shared.domain.tracking.model

import kotlinx.serialization.Serializable //▲▲▲▲▲▲▲▲
import com.sdevprem.runtrack.shared.domain.tracking.model.PathPoint //▲▲▲▲▲▲▲▲ (确保PathPoint解析)

data class CurrentRunState(
    val distanceInMeters: Int = 0,
    val speedInKMH: Float = 0f,
    val isTracking: Boolean = false,
    val pathPoints: List<PathPoint> = emptyList(),
    val currentStage: Int = 1,
val stageDistanceInMeters: Int = 0,  // 当前阶段累加 (新阶段时重置为 0)
val hiitStages: List<String> = listOf("Warm-up", "High Intensity", "Recovery", "Sprint", "Cool-down") // HIIT列表示例
)
