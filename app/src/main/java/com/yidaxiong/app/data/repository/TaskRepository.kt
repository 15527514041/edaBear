package com.yidaxiong.app.data.repository

import com.yidaxiong.app.data.remote.api.YiDaXiongApi
import com.yidaxiong.app.domain.model.Task
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 任务数据仓库
 *
 * MVP 阶段直接使用 Mock API，后续可切换为真实后端。
 */
@Singleton
class TaskRepository @Inject constructor(
    private val api: YiDaXiongApi
) {

    suspend fun getTodayTasks(): List<Task> {
        val response = api.getTodayTasks()
        return response.data?.map { dto ->
            Task(
                id = dto.id,
                title = dto.title,
                duration = dto.duration,
                isCompleted = dto.isCompleted
            )
        } ?: emptyList()
    }

    suspend fun completeTask(taskId: String) {
        api.completeTask(taskId)
    }
}
