@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")

package com.sdevprem.runtrack.shared.ui.screen.currentrun

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sdevprem.runtrack.shared.ui.common.common.animation.ComposeUtils
import com.sdevprem.runtrack.shared.ui.screen.currentrun.components.CurrentRunStatsCard
import com.sdevprem.runtrack.shared.ui.screen.currentrun.components.Map
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.vectorResource
import org.koin.compose.viewmodel.koinViewModel
import runtrack.shared.generated.resources.Res
import runtrack.shared.generated.resources.ic_back

@Composable
fun CurrentRunScreen(
    navigateUp: () -> Unit,
    viewModel: CurrentRunViewModel = koinViewModel()
) {
    var isRunningFinished by rememberSaveable { mutableStateOf(false) }
    var shouldShowRunningCard by rememberSaveable { mutableStateOf(false) }
    var isCardExpanded by rememberSaveable { mutableStateOf(false) }

    val runState by viewModel.currentRunStateWithCalories.collectAsStateWithLifecycle()
    val runningDurationInMillis by viewModel.runningDurationInMillis.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()

    // 卡片顶部绝对位置（类似 CSS top: xx%）
    // 初始值 420f ≈ 只露出底部 280dp + 2% 溢出
    val cardTopOffset = remember { Animatable(420f) }

    LaunchedEffect(key1 = Unit) {
        delay(ComposeUtils.slideDownInDuration + 200L)
        shouldShowRunningCard = true
    }

    Box(modifier = Modifier.fillMaxSize()) {

        // ==================== 1. 地图铺满整个屏幕 ====================
        Map(
            modifier = Modifier.fillMaxSize(),
            pathPoints = runState.currentRunState.pathPoints,
            isRunningFinished = isRunningFinished,
            currentSpeedInKMH = runState.currentRunState.speedInKMH,
        ) {
            viewModel.finishRun(it)
            navigateUp()
        }

        TopBar(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp),
            onNavigateUp = navigateUp
        )

        // ==================== 2. 可拖拽卡片（绝对定位 + 全屏容器） ====================
        if (shouldShowRunningCard) {
            CurrentRunStatsCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(1f)                          // 始终全屏高度（解决断层，像抽纸）
                    .align(Alignment.BottomCenter)
                    .offset(y = cardTopOffset.value.dp)         // 绝对 top 位置（类似 CSS top: %）
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDrag = { _, dragAmount ->
                                scope.launch {
                                    val newTop = (cardTopOffset.value + dragAmount.y)
                                        .coerceIn(60f, 520f)
                                    cardTopOffset.snapTo(newTop)
                                }
                            },
                            onDragEnd = {
                                scope.launch {
                                    // 60% 阈值判断
                                    if (cardTopOffset.value < 220f) {
                                        isCardExpanded = true
                                        cardTopOffset.animateTo(
                                            60f,
                                            spring(stiffness = Spring.StiffnessMedium)
                                        )
                                    } else {
                                        isCardExpanded = false
                                        cardTopOffset.animateTo(
                                            420f,
                                            spring(stiffness = Spring.StiffnessMedium)
                                        )
                                    }
                                }
                            }
                        )
                    },
                onPlayPauseButtonClick = viewModel::playPauseTracking,
                runState = runState,
                durationInMillis = runningDurationInMillis,
                onFinish = { isRunningFinished = true },
                isExpanded = isCardExpanded,
                onToggleExpand = {
                    isCardExpanded = !isCardExpanded
                    scope.launch {
                        if (isCardExpanded) {
                            cardTopOffset.animateTo(60f)
                        } else {
                            cardTopOffset.animateTo(420f)
                        }
                    }
                }, //▲▲▲▲▲▲▲▲
                onPreviousStage = { viewModel.previousStage() },       // ← 新增：上一阶段按钮
                onNextStage = { viewModel.nextStage() }                // ← 新增：下一阶段按钮
            )
        }
    }
}

@Composable
private fun TopBar(
    modifier: Modifier = Modifier,
    onNavigateUp: () -> Unit
) {
    IconButton(
        onClick = onNavigateUp,
        modifier = modifier
            .size(32.dp)
            .shadow(
                elevation = 4.dp,
                shape = MaterialTheme.shapes.medium,
                clip = true
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
            )
            .padding(4.dp)
    ) {
        Icon(
            imageVector = vectorResource(Res.drawable.ic_back),
            contentDescription = "",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}