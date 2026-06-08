package com.yidaxiong.app.domain.usecase

import com.yidaxiong.app.data.repository.TaskRepository
import com.yidaxiong.app.data.repository.UserRepository
import com.yidaxiong.app.domain.model.Task
import com.yidaxiong.app.domain.model.UserProfile
import javax.inject.Inject

/**
 * 主页数据聚合用例
 *
 * 汇总多个 Repository 的数据，一次性提供给 HomeViewModel。
 */
class GetHomeDataUseCase @Inject constructor(
    private val taskRepository: TaskRepository,
    private val userRepository: UserRepository
) {
    data class HomeData(
        val tasks: List<Task>,
        val userProfile: UserProfile
    )

    suspend operator fun invoke(): HomeData {
        val tasks = taskRepository.getTodayTasks()
        val profile = userRepository.getUserProfile()
        return HomeData(tasks = tasks, userProfile = profile)
    }
}
