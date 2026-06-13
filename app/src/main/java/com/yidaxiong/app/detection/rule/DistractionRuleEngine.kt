package com.yidaxiong.app.detection.rule

import com.yidaxiong.app.domain.model.DetectionResult
import com.yidaxiong.app.domain.model.FocusStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * 分心规则引擎
 *
 * 判定优先级：GAZE_AWAY > FIDGETING > PLAYING_OBJECT > DAZING
 */
@Singleton
class DistractionRuleEngine @Inject constructor() {

    fun evaluate(result: DetectionResult): FocusStatus {
        return when {
            // ── 走神东张西望（视线偏离屏幕） ──
            isGazeAway(result) -> FocusStatus.GAZE_AWAY

            // ── 手部频繁小动作 ──
            isFidgeting(result) -> FocusStatus.FIDGETING

            // ── 把玩文具（手在脸前/桌面区域做抓握动作） ──
            isPlayingObject(result) -> FocusStatus.PLAYING_OBJECT

            // ── 发呆摸鱼（长时间无任何专注动作） ──
            isDazing(result) -> FocusStatus.DAZING

            else -> FocusStatus.FOCUSED
        }
    }

    /**
     * 走神东张西望
     *
     * 使用两个独立信号综合判断：
     * 1. 瞳孔偏移量（精确的视线方向）— pupilOffset 偏离 0.5 超过阈值
     * 2. 视线向量（头部转动 + 眼球转动综合）— gazeVector 模长超过阈值
     */
    private fun isGazeAway(result: DetectionResult): Boolean {
        val leftGazeAway = abs(result.leftPupilOffset - 0.5f) > RuleConfig.pupilOffsetThreshold
        val rightGazeAway = abs(result.rightPupilOffset - 0.5f) > RuleConfig.pupilOffsetThreshold
        val gazeVectorAway = abs(result.gazeVector.first) > RuleConfig.gazeVectorThreshold ||
                abs(result.gazeVector.second) > RuleConfig.gazeVectorThreshold

        // 瞳孔偏离或视线向量偏离 → 走神
        return (leftGazeAway || rightGazeAway) || gazeVectorAway
    }

    /**
     * 手部频繁小动作
     *
     * 手部帧间速度超过阈值，表示在动；
     * 同时手在画面中出现（非放下状态）。
     */
    private fun isFidgeting(result: DetectionResult): Boolean {
        // 手在画面中（handCenterY < 0.95，不在画面最底部）
        val handVisible = result.handCenterY < 0.95f
        // 手部速度超过阈值
        val fastMotion = result.handVelocity > RuleConfig.handVelocityThreshold
        // 手部位移累积超过阈值
        val largeMotion = result.handMotionDelta > RuleConfig.handMotionDeltaThreshold

        return handVisible && (fastMotion || largeMotion)
    }

    /**
     * 把玩文具
     *
     * 特征：
     * 1. 手在脸附近区域（handFaceDistance 小）
     * 2. 或手在桌面区域（handCenterY 大）
     * 3. 反复抓握动作（pinchDistance 在阈值范围内变化）
     * 4. 手有小幅度但持续的运动（不是静止也不是大幅挥动）
     */
    private fun isPlayingObject(result: DetectionResult): Boolean {
        val nearFace = result.handFaceDistance < RuleConfig.playingHandFaceDistThreshold
        val onDesk = result.handCenterY > RuleConfig.playingHandYThreshold
        val handVisible = result.handCenterY < 0.95f

        // 手在脸附近 或 在桌面区域
        val inPlayingZone = nearFace || onDesk

        // 有抓握动作（拇指食指距离在合理范围0.01~0.08之间）
        val hasPinchGrip = result.handPinchDistance in 0.01f..0.08f

        // 手有小幅度运动（有动作但不是剧烈挥手）
        val gentleMotion = result.handVelocity in 0.005f..0.04f

        return handVisible && inPlayingZone && (hasPinchGrip || gentleMotion)
    }

    /**
     * 长时间无专注动作（发呆摸鱼）
     *
     * 综合活跃度评分低于阈值，表示：
     * - 头部基本不动（headPitchDelta 小）
     * - 视线无变化（gazeDelta 小）
     * - 手部无动作（handMotionDelta 小 / handVelocity 小）
     *
     * 活跃度评分由 DetectionEngine 通过 30 帧窗口计算，
     * 此处仅做阈值比较。
     */
    private fun isDazing(result: DetectionResult): Boolean {
        // 活跃度已由 DetectionEngine 的时间窗口统计得出
        return result.activityScore < RuleConfig.activityScoreThreshold
    }
}
