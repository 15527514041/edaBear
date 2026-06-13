package com.yidaxiong.app.detection.rule

/**
 * 规则引擎可配置阈值
 *
 * 所有检测阈值集中管理，方便调参。
 * 坐标值均为归一化坐标（0~1），与画面分辨率解耦。
 */
object RuleConfig {

    // ── 坐姿阈值 ──
    /** 低头判定：俯仰角阈值（度） */
    var headDownPitchThreshold: Float = 25f

    /** 低头判定：鼻梁辅助角度阈值（度），与 headPitch 加权 */
    var noseBridgeAngleThreshold: Float = 20f

    /** 歪头判定：归一化双眼高度差比值阈值（双眼Y差/脸高） */
    var headTiltNormThreshold: Float = 0.08f

    /** 过近判定：人脸面积占画面比例阈值 */
    var faceAreaRatioThreshold: Float = 0.20f

    /** 过近辅助：人脸框宽度占比阈值 */
    var faceBboxWidthThreshold: Float = 0.55f

    /** 侧身判定：左右肩高度差阈值（归一化） */
    var shoulderTiltThreshold: Float = 0.04f

    /** 侧身综合评分阈值（肩膀倾斜 + 头部偏移加权） */
    var leanThreshold: Float = 0.12f

    /** 侧身判定：头部中心 X 偏离中线的阈值（0~1, 0.5=正中） */
    var headCenterXOffsetThreshold: Float = 0.15f

    // ── 分心阈值 ──
    /** 走神判定：瞳孔偏移偏离正中的阈值（0.5±threshold） */
    var pupilOffsetThreshold: Float = 0.20f

    /** 走神判定：视线向量综合距离阈值 */
    var gazeVectorThreshold: Float = 0.15f

    /** 小动作判定：手部速度阈值（归一化帧间位移） */
    var handVelocityThreshold: Float = 0.03f

    /** 小动作判定：手部动作频率阈值（次/秒） */
    var handFrequencyThreshold: Float = 2.5f

    /** 小动作判定：手部位移累积阈值（归一化） */
    var handMotionDeltaThreshold: Float = 0.06f

    /** 把玩文具：手到脸距离上限（1=远，0=贴脸） */
    var playingHandFaceDistThreshold: Float = 0.35f

    /** 把玩文具：手部 Y 坐标下限（大于此值=在桌面区域） */
    var playingHandYThreshold: Float = 0.55f

    /** 发呆判定：活跃度评分阈值（低于此值=发呆） */
    var activityScoreThreshold: Float = 0.15f

    /** 发呆判定：帧间俯仰角变化阈值（度） */
    var dazeHeadPitchDeltaThreshold: Float = 2.0f

    /** 发呆判定：帧间视线变化阈值 */
    var dazeGazeDeltaThreshold: Float = 0.01f

    /** 发呆判定：帧间手部位移阈值（归一化） */
    var dazeHandDeltaThreshold: Float = 0.005f

    // ── 时间窗口（秒） ──
    /** 坐姿违规持续判定时长（秒） */
    var postureViolationDuration: Int = 2

    /** 分心违规持续判定时长（秒） */
    var distractionViolationDuration: Int = 3

    /** 发呆专用判定时长（秒），比一般分心更长 */
    var dazingDuration: Int = 8

    /** 违规提醒冷却间隔（秒），避免频繁提醒 */
    var alertCooldownSeconds: Int = 10

    // ── 缓冲区大小（帧数） ──
    /** 手部速度缓冲区大小 */
    const val HAND_VELOCITY_BUFFER_SIZE = 10

    /** 活跃度评分缓冲区大小 */
    const val ACTIVITY_BUFFER_SIZE = 30
}
