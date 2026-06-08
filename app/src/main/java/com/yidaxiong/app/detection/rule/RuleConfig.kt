package com.yidaxiong.app.detection.rule

/**
 * 规则引擎可配置阈值
 *
 * 所有检测阈值集中管理，方便调参。
 */
object RuleConfig {

    // ── 坐姿阈值 ──
    /** 低头判定：俯仰角阈值（度） */
    var headDownPitchThreshold: Float = 30f

    /** 歪头判定：左右耳高度差阈值（像素，640×480 坐标系） */
    var headTiltHeightDiffThreshold: Float = 30f

    /** 过近判定：人脸面积占画面比例阈值 */
    var faceAreaRatioThreshold: Float = 0.25f

    /** 侧身判定：左右肩高度差阈值（像素） */
    var shoulderTiltThreshold: Float = 30f

    // ── 分心阈值 ──
    /** 走神判定：视线偏离累计时长阈值（秒） */
    var gazeAwayDurationThreshold: Int = 3

    /** 小动作判定：手部帧间位移阈值（像素） */
    var handMotionDeltaThreshold: Float = 50f

    /** 发呆判定：头部静止累计时长阈值（秒） */
    var dazingDurationThreshold: Int = 5

    // ── 时间窗口 ──
    /** 坐姿违规持续判定时长（秒） */
    var postureViolationDuration: Int = 2

    /** 分心违规持续判定时长（秒） */
    var distractionViolationDuration: Int = 3

    /** 违规提醒冷却间隔（秒），避免频繁提醒 */
    var alertCooldownSeconds: Int = 10
}
