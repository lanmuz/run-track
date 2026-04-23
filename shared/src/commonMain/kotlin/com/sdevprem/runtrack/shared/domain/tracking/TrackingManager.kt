package com.sdevprem.runtrack.shared.domain.tracking

import com.sdevprem.runtrack.shared.common.utils.LocationUtils
import com.sdevprem.runtrack.shared.domain.tracking.background.BackgroundTrackingManager
import com.sdevprem.runtrack.shared.domain.tracking.location.LocationTrackingManager
import com.sdevprem.runtrack.shared.domain.tracking.model.CurrentRunState
import com.sdevprem.runtrack.shared.domain.tracking.model.LocationTrackingInfo
import com.sdevprem.runtrack.shared.domain.tracking.model.PathPoint
import com.sdevprem.runtrack.shared.domain.tracking.timer.TimeTracker
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.math.round

class TrackingManager(
    private val locationTrackingManager: LocationTrackingManager,
    private val timeTracker: TimeTracker,
    private val backgroundTrackingManager: BackgroundTrackingManager
) {
    private var isTracking = false
        set(value) {
            _currentRunState.update { it.copy(isTracking = value) }
            field = value
        }

    private val _currentRunState = MutableStateFlow(CurrentRunState())
    val currentRunState = _currentRunState

    private val _trackingDurationInMs = MutableStateFlow(0L)
    val trackingDurationInMs = _trackingDurationInMs.asStateFlow()

    private val timeTrackerCallback = { timeElapsed: Long ->
        _trackingDurationInMs.update { timeElapsed }
    }

    private var isFirst = true

    private val locationCallback = object : LocationTrackingManager.LocationCallback {

        override fun onLocationUpdate(results: List<LocationTrackingInfo>) {
            if (isTracking) {
                results.forEach { info ->
                    addPathPoints(info)
                }
            }
        }
    }

    private fun postInitialValue() {
        _currentRunState.update {
            CurrentRunState()
        }
        _trackingDurationInMs.update { 0 }
    }
private fun getSafeDistance(p1: PathPoint, p2: PathPoint): Float {
    return if (p1 is PathPoint.LocationPoint && p2 is PathPoint.LocationPoint) {
        LocationUtils.getDistanceBetweenPathPoints(p1, p2).toFloat()
    } else {
        0f
    }
}


private fun addPathPoints(info: LocationTrackingInfo) {
    _currentRunState.update { state ->
        val pathPoints = state.pathPoints + PathPoint.LocationPoint(info.locationInfo)

        val newDelta = if (pathPoints.size > 1) {
            getSafeDistance(
                pathPoints[pathPoints.size - 1],
                pathPoints[pathPoints.size - 2]
            )
        } else 0f

        val newStageDist = state.stageDistanceInMeters + newDelta
        val isStageComplete = newStageDist >= 500f
        val newStage = if (isStageComplete) (state.currentStage % state.hiitStages.size) + 1 else state.currentStage

        state.copy(
            pathPoints = pathPoints,
            distanceInMeters = state.distanceInMeters.run {
                var distance = this
                if (pathPoints.size > 1) {
                    distance += getSafeDistance(
                        pathPoints[pathPoints.size - 1],
                        pathPoints[pathPoints.size - 2]
                    ).toInt()
                }
                distance
            },
            speedInKMH = round(info.speedInMS * 3.6f * 100f) / 100f,
            stageDistanceInMeters = if (isStageComplete) 0f else newStageDist,
            currentStage = newStage
        )
    }
}
    fun startResumeTracking() {
        if (isTracking)
            return
        if (isFirst) {
            postInitialValue()
            backgroundTrackingManager.startBackgroundTracking()
            isFirst = false
        }
        isTracking = true
        timeTracker.startResumeTimer(timeTrackerCallback)
        locationTrackingManager.setCallback(locationCallback)
    }

    private fun addEmptyPolyLine() {
        _currentRunState.update {
            it.copy(
                pathPoints = it.pathPoints + PathPoint.EmptyLocationPoint
            )
        }
    }

    fun pauseTracking() {
        isTracking = false
        locationTrackingManager.removeCallback()
        timeTracker.pauseTimer()
        addEmptyPolyLine()
    }

    fun stop() {
        pauseTracking()
        backgroundTrackingManager.stopBackgroundTracking()
        timeTracker.stopTimer()
        postInitialValue()
        isFirst = true
    }
fun startNewStage() {
    _currentRunState.update { state ->
        val size = state.hiitStages.size
        if (size == 0) return@update state
        state.copy(
            stageDistanceInMeters = 0f,
            currentStage = (state.currentStage % size) + 1
        )
    }
}

// ==================== 手动切换阶段 ====================
    fun previousStage() {
        _currentRunState.update { state ->
            if (state.currentStage > 1) {                                    // 只有不是第一阶段才能后退
                state.copy(
                    currentStage = state.currentStage - 1,                   // 阶段号减1
                    stageDistanceInMeters = 0f                               // 重置当前阶段进度
                )
            } else {
                state                                                    // 已经是第一阶段则不做任何事
            }
        }
    }

    fun nextStage() {
        _currentRunState.update { state ->
            val size = state.hiitStages.size
            if (state.currentStage < size) {                                 // 只有不是最后阶段才能前进
                state.copy(
                    currentStage = state.currentStage + 1,                   // 阶段号加1
                    stageDistanceInMeters = 0f                               // 重置当前阶段进度
                )
            } else {
                state                                                    // 已经是最后阶段则不做任何事
            }
        }
    }
    



}