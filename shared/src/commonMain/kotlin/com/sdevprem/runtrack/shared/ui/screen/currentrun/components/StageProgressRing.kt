package com.sdevprem.runtrack.shared.ui.screen.currentrun.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp


@Composable
fun StageProgressRing(
    currentStage: Int,
    stageDistanceInMeters: Float,
    targetMeters: Int = 500,
    modifier: Modifier = Modifier,
    hiitStageName: String = "HIIT Stage"
) {
    val progress = (stageDistanceInMeters.toFloat() / targetMeters).coerceIn(0f, 1f)
    val isComplete = progress >= 1f

    Box(modifier = modifier.size(80.dp), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.size(80.dp),
            color = if (isComplete) Color.Green else MaterialTheme.colorScheme.primary,
            strokeWidth = 8.dp,
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
            // SnackbarHostState.showSnackbar("�׶���ɣ����� HIIT ��һ���׶�: ${nextHiitStage}")
        }
    }
}