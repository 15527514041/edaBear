package com.yidaxiong.app.detection.result

/**
 * 检测统计指标
 *
 * 记录一次检测会话的汇总数据：
 * - 总检测时长
 * - 各类违规次数
 * - 坐姿/专注度综合评分
 */
data class DetectionMetrics(
    val totalDurationMs: Long = 0L,
    val totalViolations: Int = 0,
    val postureViolations: Int = 0,
    val distractionViolations: Int = 0,
    val postureScore: Int = 100,   // 0-100
    val focusScore: Int = 100      // 0-100
) {
    companion object {
        fun empty() = DetectionMetrics()
    }
}
