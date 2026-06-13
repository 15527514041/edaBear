package com.yidaxiong.app.detection.rule

import com.yidaxiong.app.domain.model.DetectionResult
import com.yidaxiong.app.domain.model.PostureStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * 坐姿规则引擎
 *
 * 判定优先级（由危害程度决定）：TOO_CLOSE > HEAD_DOWN > HEAD_TILTED > LEANING
 */
@Singleton
class PostureRuleEngine @Inject constructor() {

    fun evaluate(result: DetectionResult): PostureStatus {
        return when {
            // ── 离屏幕过近（最高优先级：伤害视力） ──
            isTooClose(result) -> PostureStatus.TOO_CLOSE

            // ── 低头驼背 ──
            isHeadDown(result) -> PostureStatus.HEAD_DOWN

            // ── 左右歪头 ──
            isHeadTilted(result) -> PostureStatus.HEAD_TILTED

            // ── 侧身趴桌 ──
            isLeaning(result) -> PostureStatus.LEANING

            else -> PostureStatus.GOOD
        }
    }

    /**
     * 离屏幕过近
     *
     * 人脸包围盒面积占比 > 阈值 或 人脸框宽度占比 > 阈值
     */
    private fun isTooClose(result: DetectionResult): Boolean {
        return result.faceAreaRatio > RuleConfig.faceAreaRatioThreshold ||
                result.faceBboxWidth > RuleConfig.faceBboxWidthThreshold
    }

    /**
     * 低头驼背
     *
     * 俯仰角 > 阈值（正值 = 低头）。
     * 辅助鼻梁角度，两者加权平均以增加稳定性。
     */
    private fun isHeadDown(result: DetectionResult): Boolean {
        val avgPitch = (result.headPitch + result.noseBridgeAngle) / 2f
        return avgPitch > RuleConfig.headDownPitchThreshold
    }

    /**
     * 左右歪头
     *
     * 使用归一化的双眼高度差比，消除距离影响：
     * 双眼内角 Y 差 / 脸高 + 双眼外角 Y 差 / 脸高，二者取平均。
     *
     * 归一化后值与距离无关：
     * - 正脸平视 → ~0
     * - 歪头 15° → ~0.05
     * - 歪头 30° → ~0.12
     */
    private fun isHeadTilted(result: DetectionResult): Boolean {
        return result.normalizedEyeYDiff > RuleConfig.headTiltNormThreshold
    }

    /**
     * 侧身趴桌
     *
     * 综合判定：肩膀倾斜 + 头部中心偏移 + 耳朵可见度。
     * 当肩膀不可见时（正面视角），头部 X 偏移为主判定依据。
     */
    private fun isLeaning(result: DetectionResult): Boolean {
        return result.combinedLeanScore > RuleConfig.leanThreshold
    }
}
