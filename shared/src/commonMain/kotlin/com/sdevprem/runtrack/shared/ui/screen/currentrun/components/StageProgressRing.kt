@Composable
fun StageProgressRing(
    currentStage: Int,
    stageDistanceInMeters: Int,
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
            color = if (isComplete) Color.Green else MaterialTheme.colorScheme.primary, // 彩色覆盖
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
        // 提示完成 (可在 ViewModel 中触发 Snackbar)
        LaunchedEffect(Unit) {
            // SnackbarHostState.showSnackbar("阶段完成！进入 HIIT 下一个阶段: ${nextHiitStage}")
        }
    }
}