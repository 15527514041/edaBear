package com.yidaxiong.app.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val id: String = "",
    val nickname: String = "小朋友",
    val avatar: String = "",
    val totalStars: Int = 0
)
