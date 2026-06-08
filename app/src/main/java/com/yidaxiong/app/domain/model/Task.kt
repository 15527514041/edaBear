package com.yidaxiong.app.domain.model

/**
 * 任务领域模型
 */
data class Task(
    val id: String,
    val title: String,
    val duration: Int,       // 分钟
    val isCompleted: Boolean = false
)
