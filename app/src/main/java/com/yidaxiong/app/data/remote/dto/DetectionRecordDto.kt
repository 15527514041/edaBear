package com.yidaxiong.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DetectionRecordDto(
    val taskId: String = "",
    val startTime: String = "",
    val endTime: String = "",
    val postureViolations: Int = 0,
    val distractionViolations: Int = 0,
    val totalDuration: Int = 0
)
