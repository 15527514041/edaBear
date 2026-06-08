package com.yidaxiong.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class TaskDto(
    val id: String = "",
    val title: String = "",
    val subject: String = "",
    val duration: Int = 0,
    val isCompleted: Boolean = false,
    val createdAt: String = ""
)
