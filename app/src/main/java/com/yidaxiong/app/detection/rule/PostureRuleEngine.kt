package com.yidaxiong.app.detection.rule

import com.yidaxiong.app.domain.model.DetectionResult
import com.yidaxiong.app.domain.model.PostureStatus
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PostureRuleEngine @Inject constructor() {

    fun evaluate(result: DetectionResult): PostureStatus {
        return when {
            result.headPitch > RuleConfig.headDownPitchThreshold -> PostureStatus.HEAD_DOWN
            result.earYDiff > RuleConfig.headTiltHeightDiffThreshold -> PostureStatus.HEAD_TILTED
            result.faceAreaRatio > RuleConfig.faceAreaRatioThreshold -> PostureStatus.TOO_CLOSE
            result.shoulderTilt > RuleConfig.shoulderTiltThreshold -> PostureStatus.LEANING
            else -> PostureStatus.GOOD
        }
    }
}
