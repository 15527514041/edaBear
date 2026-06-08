package com.yidaxiong.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yidaxiong.app.domain.model.Task
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 主页 ViewModel
 *
 * MVP 阶段使用 Mock 数据，后续接入 Repository。
 */
data class HomeUiState(
    val isLoading: Boolean = false,
    val tasks: List<Task> = emptyList(),
    val postureStatus: String = "--",
    val focusStatus: String = "--",
    val todayStars: Int = 0
)

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadMockData()
    }

    private fun loadMockData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // 模拟网络延迟
            delay(500)

            val mockTasks = listOf(
                Task(id = "1", title = "📖 语文生字练习", duration = 20, isCompleted = false),
                Task(id = "2", title = "🧮 数学口算题", duration = 15, isCompleted = false),
                Task(id = "3", title = "📝 英语单词跟读", duration = 10, isCompleted = false)
            )

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                tasks = mockTasks,
                postureStatus = "良好 ✅",
                focusStatus = "专注 ⭐⭐⭐",
                todayStars = 3
            )
        }
    }
}
