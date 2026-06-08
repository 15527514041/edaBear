package com.yidaxiong.app.data.remote.api

import com.yidaxiong.app.data.remote.dto.*
import kotlinx.serialization.Serializable

/**
 * 通用 API 响应包装
 */
@Serializable
data class ApiResponse<T>(
    val code: Int = 200,
    val message: String = "success",
    val data: T? = null
)
