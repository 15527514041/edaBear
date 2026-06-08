package com.yidaxiong.app.detection.rule

import com.yidaxiong.app.domain.model.DetectionResult
import com.yidaxiong.app.domain.model.FocusStatus
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class DistractionRuleEngine @Inject constructor() {

    private val gazeAwayX = 0.3f; private val gazeAwayY = 0.3f
    private val pinchDist = 0.05f
    private val dazeHead = 0.5f; private val dazeGaze = 0.02f; private val dazeHand = 5f

    fun evaluate(result: DetectionResult): FocusStatus {
        return when {
            abs(result.gazeVector.first) > gazeAwayX || abs(result.gazeVector.second) > gazeAwayY -> FocusStatus.GAZE_AWAY
            result.handMotionDelta > RuleConfig.handMotionDeltaThreshold -> FocusStatus.FIDGETING
            result.handPinchDistance > 0f && result.handPinchDistance < pinchDist && result.handMotionDelta < RuleConfig.handMotionDeltaThreshold * 0.5f -> FocusStatus.PLAYING_OBJECT
            result.headPitchDelta < dazeHead && result.gazeDelta < dazeGaze && result.handMotionDelta < dazeHand -> FocusStatus.DAZING
            else -> FocusStatus.FOCUSED
        }
    }
}
