package com.sdevprem.runtrack.shared.domain.tracking.model

import kotlinx.serialization.Serializable

/**
 * 单个 HIIT 阶段的完整配置
 */
@Serializable
data class HiitStage(
    val name: String,                           // 阶段名称
    val distanceMeters: Int,                    // 本阶段目标距离（米）
    val backgroundMusic: String = "",           // 背景音乐资源
    val beatSeconds: Float = 1.5f,              // 固定打点间隔（秒），例如 1.5f
    val startPrompt: String = "",               // 开始提示语
    val endPrompt: String = "",                 // 结束提示语
    val midPrompts: List<String> = emptyList(), // 中间提示（格式："10#加油"）
    val overlayMusic: List<String> = emptyList(), // 叠加音乐（格式："10#音乐链接"）
    val effects: List<String> = emptyList()     // 特效（格式："10#特效名称"）
)

/**
 * 当前跑步实时状态
 */
data class CurrentRunState(
    val distanceInMeters: Int = 0,
    val speedInKMH: Float = 0f,
    val isTracking: Boolean = false,
    val pathPoints: List<PathPoint> = emptyList(),
    val currentStage: Int = 1,
    val stageDistanceInMeters: Float = 0f,
    val hiitStages: List<HiitStage> = defaultHiitStages
)

// ==================== 默认丰富的 HIIT 阶段列表 ====================
val defaultHiitStages = listOf(
    HiitStage(
        name = "慢跑500米/快走200米",
        distanceMeters = 700,
        backgroundMusic = "warmup_bgm",
        beatSeconds = 1.5f,                    // 固定打点间隔 1.5 秒
        startPrompt = "这一段跑步锻炼心肺功能，最好小幅度、高步频的跑，加油吧！",
        endPrompt = "非常棒！你完成了这一阶段的跑步！",
        midPrompts = listOf("10#加油", "20#坚持住", "30#快到了"),
        overlayMusic = listOf("15#cheer_music"),
        effects = listOf("10#粒子特效", "25#光晕")
    ),
    HiitStage(
        name = "高强度冲刺300米",
        distanceMeters = 300,
        backgroundMusic = "intense_bgm",
        beatSeconds = 0.6f,                    // 固定打点间隔 0.6 秒
        startPrompt = "高强度冲刺开始！全力以赴，感受速度与激情！",
        endPrompt = "太强了！你突破了自己的极限！",
        midPrompts = listOf("8#加速", "15#再坚持5秒"),
        overlayMusic = listOf("10#power_music"),
        effects = listOf("5#闪电特效", "12#燃烧粒子")
    ),
    HiitStage(
        name = "恢复慢跑400米",
        distanceMeters = 400,
        backgroundMusic = "recovery_bgm",
        beatSeconds = 2.0f,
        startPrompt = "进入恢复阶段，调整呼吸，保持轻松节奏。",
        endPrompt = "恢复完成，身体感觉更好啦！",
        midPrompts = listOf("15#深呼吸", "25#放松肩膀"),
        overlayMusic = emptyList(),
        effects = listOf("20#清新光效")
    ),
    HiitStage(
        name = "爆发冲刺200米",
        distanceMeters = 200,
        backgroundMusic = "sprint_bgm",
        beatSeconds = 0.4f,
        startPrompt = "爆发冲刺！用尽全力，超越自己！",
        endPrompt = "无敌！你就是最强的！",
        midPrompts = listOf("5#冲啊", "10#最后5秒"),
        overlayMusic = listOf("3#explode_music"),
        effects = listOf("2#爆炸特效", "8#金色光芒")
    ),
    HiitStage(
        name = "冷却放松300米",
        distanceMeters = 300,
        backgroundMusic = "cooldown_bgm",
        beatSeconds = 2.2f,
        startPrompt = "进入冷却阶段，慢慢放慢脚步，放松全身。",
        endPrompt = "完美！你今天的表现非常出色！",
        midPrompts = listOf("20#深呼吸", "40#感谢自己"),
        overlayMusic = emptyList(),
        effects = listOf("30#柔光特效")
    )
)