package com.yidaxiong.app.util

/**
 * 易达熊全局常量
 */
object Constants {
    // ── 相机配置 ──
    const val CAMERA_RESOLUTION_WIDTH = 640
    const val CAMERA_RESOLUTION_HEIGHT = 480
    const val INFERENCE_FPS = 10

    // ── 检测阈值（可配置，参考 RuleConfig） ──
    const val HEAD_DOWN_THRESHOLD = 30f    // 俯仰角阈值（度）
    const val HEAD_TILT_THRESHOLD = 15f    // 歪头高度差阈值（像素）
    const val FACE_AREA_THRESHOLD = 0.25f  // 人脸面积占比阈值
    const val GAZE_AWAY_SECONDS = 3        // 视线偏离判定时长（秒）
    const val POSTURE_VIOLATION_SECONDS = 2 // 坐姿违规判定时长（秒）
    const val HAND_MOTION_THRESHOLD = 50f  // 手部位移阈值（像素）

    // ── API ──
    const val API_BASE_URL = "https://api.yidaxiong.com/v1/"
}
