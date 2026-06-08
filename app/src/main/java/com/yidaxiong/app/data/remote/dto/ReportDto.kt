package com.yidaxiong.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ReportDto(
    val date: String = "",
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val totalStars: Int = 0,
    val postureScore: Int = 0,
    val focusScore: Int = 0,
    val suggestions: List<String> = emptyList()
)
