package com.yidaxiong.app.domain.model

/**
 * 用户档案领域模型
 */
data class UserProfile(
    val id: String = "",
    val nickname: String = "",
    val avatar: String = "",
    val totalStars: Int = 0
)
