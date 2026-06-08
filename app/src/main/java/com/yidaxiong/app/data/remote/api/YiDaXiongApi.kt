package com.yidaxiong.app.data.remote.api

import com.yidaxiong.app.data.remote.dto.*
import retrofit2.http.*

/**
 * 易达熊后端 API 接口定义
 *
 * MVP 阶段使用 MockInterceptor 返回模拟数据。
 */
interface YiDaXiongApi {

    @GET("tasks/today")
    suspend fun getTodayTasks(): ApiResponse<List<TaskDto>>

    @POST("tasks/{id}/complete")
    suspend fun completeTask(
        @Path("id") taskId: String
    ): ApiResponse<Unit>

    @GET("report/daily")
    suspend fun getDailyReport(
        @Query("date") date: String
    ): ApiResponse<ReportDto>

    @GET("user/profile")
    suspend fun getUserProfile(): ApiResponse<UserDto>

    @POST("detection/record")
    suspend fun uploadDetectionRecord(
        @Body record: DetectionRecordDto
    ): ApiResponse<Unit>
}
