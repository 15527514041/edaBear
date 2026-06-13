package com.yidaxiong.app.domain.model

data class DetectionResult(
    val postureStatus: PostureStatus = PostureStatus.UNKNOWN,
    val focusStatus: FocusStatus = FocusStatus.UNKNOWN,

    // ── 头部姿态 ──
    /** 俯仰角（度），正值=低头 */
    val headPitch: Float = 0f,
    /** 鼻梁俯仰角（度），更精确的低头判定 */
    val noseBridgeAngle: Float = 0f,
    /** 头部中心 X（归一化 0~1），用于侧身判定 */
    val headCenterX: Float = 0.5f,

    // ── 人脸区域 ──
    /** 人脸包围盒面积占比（归一化） */
    val faceAreaRatio: Float = 0f,
    /** 人脸框宽度（归一化） */
    val faceBboxWidth: Float = 0f,
    /** 人脸框高度（归一化） */
    val faceBboxHeight: Float = 0f,

    // ── 歪头特征 ──
    /** 左右耳 Y 坐标差（归一化） */
    val earYDiff: Float = 0f,
    /** 归一化双眼高度差比 */
    val normalizedEyeYDiff: Float = 0f,

    // ── 视线方向 ──
    /** 视线向量 (dx, dy) */
    val gazeVector: Pair<Float, Float> = 0f to 0f,
    /** 左瞳孔在眼框中的归一化偏移 (0~1, 0.5=正中) */
    val leftPupilOffset: Float = 0.5f,
    /** 右瞳孔在眼框中的归一化偏移 */
    val rightPupilOffset: Float = 0.5f,
    /** 左右眼纵横比（睁眼程度，越小=眨眼/闭眼） */
    val leftEyeAspectRatio: Float = 0.3f,
    val rightEyeAspectRatio: Float = 0.3f,

    // ── 肩膀/身体 ──
    /** 肩膀高度差（归一化） */
    val shoulderTilt: Float = 0f,

    // ── 手部 ──
    /** 手部帧间位移（归一化） */
    val handMotionDelta: Float = 0f,
    /** 手部归一化速度（帧间位移/脸宽，去距离影响） */
    val handVelocity: Float = 0f,
    /** 拇指-食指捏合距离（归一化） */
    val handPinchDistance: Float = 0f,
    /** 手到脸距离（归一化，1=远，0=贴在脸上） */
    val handFaceDistance: Float = 1f,

    // ── 帧间变化 ──
    /** 俯仰角帧间变化量 */
    val headPitchDelta: Float = 0f,
    /** 视线帧间变化量 */
    val gazeDelta: Float = 0f,

    // ── 综合评分 ──
    /** 侧身综合评分（肩膀倾斜 + 头部偏移加权） */
    val combinedLeanScore: Float = 0f,
    /** 发呆活跃度评分（越低=越呆） */
    val activityScore: Float = 1f,

    // ── 手部坐标（供分心引擎用） ──
    val handCenterX: Float = 0f,
    val handCenterY: Float = 1f
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
