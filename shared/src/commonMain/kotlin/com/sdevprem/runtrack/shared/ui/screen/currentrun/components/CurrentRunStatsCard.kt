package com.sdevprem.runtrack.shared.ui.screen.currentrun.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sdevprem.runtrack.shared.common.extension.roundTo
import com.sdevprem.runtrack.shared.common.utils.DateUtils.getFormattedStopwatchTime
import com.sdevprem.runtrack.shared.domain.model.CurrentRunStateWithCalories
import com.sdevprem.runtrack.shared.domain.tracking.model.CurrentRunState
import com.sdevprem.runtrack.shared.ui.common.compose.components.RunningStatsItem
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.vectorResource
import runtrack.shared.generated.resources.Res
import runtrack.shared.generated.resources.bolt
import runtrack.shared.generated.resources.fire
import runtrack.shared.generated.resources.ic_finish
import runtrack.shared.generated.resources.ic_pause
import runtrack.shared.generated.resources.ic_play
import runtrack.shared.generated.resources.running_boy
import androidx.compose.foundation.layout.Box //▲▲▲▲▲▲▲▲ (保留原有import不变, 新增此行)
import androidx.compose.material3.CircularProgressIndicator //▲▲▲▲▲▲▲▲
import androidx.compose.ui.text.style.TextAlign //▲▲▲▲▲▲▲▲
import androidx.compose.runtime.LaunchedEffect //▲▲▲▲▲▲▲▲
import androidx.compose.ui.graphics.Color //▲▲▲▲▲▲▲▲
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.foundation.shape.RoundedCornerShape


import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale


@Composable
fun CurrentRunStatsCard(
    modifier: Modifier = Modifier,
    durationInMillis: Long = 0L,
    runState: CurrentRunStateWithCalories,
    onPlayPauseButtonClick: () -> Unit = {},
    onFinish: () -> Unit,
    isExpanded: Boolean = false, //▲▲▲▲▲▲▲▲
    onToggleExpand: () -> Unit = {} //▲▲▲▲▲▲▲▲
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth(),
            // ▼▼▼▼▼▼▼▼ 修改部分开始 ▼▼▼▼▼▼▼▼
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.elevatedCardColors(
        containerColor = Color.Transparent
    ),
        // ▲▲▲▲▲▲▲▲ 修改部分结束 ▲▲▲▲▲▲▲▲
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
 // 新增展开按钮 (置于顶端, 默认缩回状态)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onToggleExpand) { //▲▲▲▲▲▲▲▲
Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowDown 
                                 else Icons.Default.KeyboardArrowUp,
                    contentDescription = if (isExpanded) "收起" else "展开"
                )
            } //▲▲▲▲▲▲▲▲
        }





        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(top = 24.dp, bottom = 16.dp)
                .padding(horizontal = 20.dp)
        ) {
            RunningCardTime(
                modifier = Modifier
                    .weight(1f),
                durationInMillis = durationInMillis,
            )
            TrackingControlButton(
                isRunning = runState.currentRunState.isTracking,
                durationInMillis = durationInMillis,
                onFinish = onFinish,
                onPlayPauseButtonClick = onPlayPauseButtonClick
            )
        }
        RunningStats(runState, isExpanded = isExpanded)
    }
}

@Composable
private fun RunningStats(
    runState: CurrentRunStateWithCalories,
    isExpanded: Boolean = false //▲▲▲▲▲▲▲▲
) {
    Row(
        horizontalArrangement = Arrangement.SpaceAround,
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(bottom = 20.dp)
            .height(IntrinsicSize.Min)
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)

    ) {
        RunningStatsItem(
            modifier = Modifier,
            painter = painterResource(Res.drawable.running_boy),
            unit = "km",
            value = (runState.currentRunState.distanceInMeters / 1000f).toString()
        )
        StageProgressRing(
            currentStage = runState.currentRunState.currentStage,
            stageDistanceInMeters = runState.currentRunState.stageDistanceInMeters,
            hiitStageName = runState.currentRunState.hiitStages.getOrElse(runState.currentRunState.currentStage - 1) { "Stage" },
            isExpanded = isExpanded, //▲▲▲▲▲▲▲▲ (控制ring大小/位置/呼吸效果, 默认小比例靠RunningTime右侧)
            modifier = if (isExpanded) Modifier.size(180.dp).align(Alignment.CenterVertically) else Modifier.size(48.dp)
        )
        VerticalDivider(
            thickness = 1.dp,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )
        RunningStatsItem(
            modifier = Modifier,
            painter = painterResource(Res.drawable.fire),
            unit = "kcal",
            value = runState.caloriesBurnt.toString()
        )

        VerticalDivider(
            thickness = 1.dp,
            modifier = Modifier
                .padding(vertical = 8.dp)
        )
        RunningStatsItem(
            modifier = Modifier,
            painter = painterResource(Res.drawable.bolt),
            unit = "km/hr",
            value = runState.currentRunState.speedInKMH.toString()
        )
    }
}

// 新增/修改的StageProgressRing (置于文件末尾, 保留原有所有preview/函数/注释/缩进不变)
@Composable
fun StageProgressRing(
    currentStage: Int,
    stageDistanceInMeters: Float,
    targetMeters: Int = 500,
    modifier: Modifier = Modifier,
    hiitStageName: String = "HIIT Stage",
    isExpanded: Boolean = false //▲▲▲▲▲▲▲▲
) {
    val progress = (stageDistanceInMeters.toFloat() / targetMeters).coerceIn(0f, 1f) //▲▲▲▲▲▲▲▲
    val isComplete = progress >= 1f //▲▲▲▲▲▲▲▲
    val infiniteTransition = rememberInfiniteTransition() //▲▲▲▲▲▲▲▲ (Keep式呼吸光)
    val breathScale by infiniteTransition.animateFloat( //▲▲▲▲▲▲▲▲
        initialValue = if (isExpanded) 0.95f else 1f, //▲▲▲▲▲▲▲▲
        targetValue = if (isExpanded) 1.05f else 1f, //▲▲▲▲▲▲▲▲
        animationSpec = infiniteRepeatable( //▲▲▲▲▲▲▲▲
            animation = tween(1200, easing = FastOutSlowInEasing), //▲▲▲▲▲▲▲▲
            repeatMode = RepeatMode.Reverse //▲▲▲▲▲▲▲▲
        ) //▲▲▲▲▲▲▲▲
    ) //▲▲▲▲▲▲▲▲
    val glowAlpha by infiniteTransition.animateFloat( //▲▲▲▲▲▲▲▲
        initialValue = if (isExpanded) 0.3f else 0f, //▲▲▲▲▲▲▲▲
        targetValue = if (isExpanded) 0.7f else 0f, //▲▲▲▲▲▲▲▲
        animationSpec = infiniteRepeatable( //▲▲▲▲▲▲▲▲
            animation = tween(800), //▲▲▲▲▲▲▲▲
            repeatMode = RepeatMode.Reverse //▲▲▲▲▲▲▲▲
        ) //▲▲▲▲▲▲▲▲
    ) //▲▲▲▲▲▲▲▲
    Box(modifier = modifier.size(if (isExpanded) 180.dp else 48.dp), contentAlignment = Alignment.Center) { //▲▲▲▲▲▲▲▲ (默认小, 展开居中大)
        if (isExpanded) { //▲▲▲▲▲▲▲▲ (呼吸光特效层)
            Box( //▲▲▲▲▲▲▲▲
                modifier = Modifier
                    .size(200.dp)
                    .scale(breathScale)
                    .alpha(glowAlpha)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                        shape = CircleShape
                    )
            ) //▲▲▲▲▲▲▲▲
        } //▲▲▲▲▲▲▲▲
        CircularProgressIndicator( //▲▲▲▲▲▲▲▲
            progress = { progress },
            modifier = Modifier.size(if (isExpanded) 180.dp else 48.dp).scale(if (isExpanded) breathScale else 1f), //▲▲▲▲▲▲▲▲
            color = if (isComplete) Color.Green else MaterialTheme.colorScheme.primary,
            strokeWidth = if (isExpanded) 16.dp else 6.dp, //▲▲▲▲▲▲▲▲
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$stageDistanceInMeters/$targetMeters m",
                style = MaterialTheme.typography.labelSmall,
                color = if (isComplete) Color.Green else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Stage $currentStage\n$hiitStageName",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        }
    }
    if (isComplete) {
        LaunchedEffect(Unit) {
            // Snackbar or prompt: "阶段完成！进入 HIIT 下一个阶段" (在ViewModel中处理)
        }
    }
}

@Composable
private fun RunningCardTime(
    modifier: Modifier = Modifier,
    durationInMillis: Long,
) {
    Column(modifier = modifier) {
        Text(
            text = "Running Time",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Normal),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = getFormattedStopwatchTime(durationInMillis),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun TrackingControlButton(
    modifier: Modifier = Modifier,
    isRunning: Boolean,
    durationInMillis: Long,
    onFinish: () -> Unit,
    onPlayPauseButtonClick: () -> Unit
) {
    Row(modifier = modifier) {
        if (!isRunning && durationInMillis > 0) {
            IconButton(
                onClick = onFinish,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = MaterialTheme.shapes.medium
                    )
            ) {
                Icon(
                    imageVector = vectorResource(Res.drawable.ic_finish),
                    contentDescription = "",
                    modifier = Modifier
                        .size(16.dp),
                    tint = MaterialTheme.colorScheme.onError
                )
            }
            Spacer(modifier = Modifier.size(16.dp))
        }
        IconButton(
            onClick = onPlayPauseButtonClick,
            modifier = Modifier
                .size(40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Icon(
                imageVector = vectorResource(
                    if (isRunning) Res.drawable.ic_pause else Res.drawable.ic_play
                ),
                contentDescription = "",
                modifier = Modifier
                    .size(16.dp),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
//@Preview(showBackground = true)
private fun CurrentRunStatsCardPreview() {
    var isRunning by rememberSaveable { mutableStateOf(false) }
    CurrentRunStatsCard(
        durationInMillis = 5400000,
        runState = CurrentRunStateWithCalories(
            currentRunState = CurrentRunState(
                distanceInMeters = 600,
                speedInKMH = (6.935 /* m/s */ * 3.6)
                    .toFloat()
                    .roundTo(2),
                isTracking = isRunning
            ),
            caloriesBurnt = 532
        ),
        onPlayPauseButtonClick = {
            isRunning = !isRunning
        },
        onFinish = {}
    )
}
