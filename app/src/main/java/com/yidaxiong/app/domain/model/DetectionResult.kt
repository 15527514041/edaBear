package com.yidaxiong.app.domain.model

data class DetectionResult(
    val postureStatus: PostureStatus = PostureStatus.UNKNOWN,
    val focusStatus: FocusStatus = FocusStatus.UNKNOWN,
    val headPitch: Float = 0f,
    val faceAreaRatio: Float = 0f,
    val shoulderTilt: Float = 0f,
    val earYDiff: Float = 0f,
    val gazeVector: Pair<Float, Float> = 0f to 0f,
    val handMotionDelta: Float = 0f,
    val handPinchDistance: Float = 0f,
    val headPitchDelta: Float = 0f,
    val gazeDelta: Float = 0f
)

enum class PostureStatus {
    GOOD, HEAD_DOWN, HEAD_TILTED, TOO_CLOSE, LEANING, UNKNOWN;

    fun toDisplayString(): String = when (this) {
        GOOD -> "良好 ✅"; HEAD_DOWN -> "低头 ⚠️"; HEAD_TILTED -> "歪头 ⚠️"
        TOO_CLOSE -> "过近 ⚠️"; LEANING -> "侧身 ⚠️"; UNKNOWN -> "--"
    }
}

enum class FocusStatus {
    FOCUSED, GAZE_AWAY, FIDGETING, PLAYING_OBJECT, DAZING, UNKNOWN;

    fun toDisplayString(): String = when (this) {
        FOCUSED -> "专注 ⭐"; GAZE_AWAY -> "走神 ⚠️"; FIDGETING -> "小动作 ⚠️"
        PLAYING_OBJECT -> "把玩文具 ⚠️"; DAZING -> "发呆 ⚠️"; UNKNOWN -> "--"
    }
}
