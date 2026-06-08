package com.yidaxiong.app.data.repository

import com.yidaxiong.app.data.remote.api.YiDaXiongApi
import com.yidaxiong.app.domain.model.UserProfile
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 用户数据仓库
 */
@Singleton
class UserRepository @Inject constructor(
    private val api: YiDaXiongApi
) {

    suspend fun getUserProfile(): UserProfile {
        val response = api.getUserProfile()
        return response.data?.let { dto ->
            UserProfile(
                id = dto.id,
                nickname = dto.nickname,
                avatar = dto.avatar,
                totalStars = dto.totalStars
            )
        } ?: UserProfile()
    }
}
