package com.yidaxiong.app.ui.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yidaxiong.app.ui.home.components.HonorStars
import com.yidaxiong.app.ui.home.components.StatusPanel
import com.yidaxiong.app.ui.home.components.TaskCard

/**
 * 易达熊 — 主页
 *
 * 显示今日任务、学习状态、荣誉星星，入口进入检测模式。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartLearning: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "易达熊陪读",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ── 顶部内边距 ──
            Spacer(modifier = Modifier.height(24.dp))

            // ── 今日任务卡片 ──
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "📋 今日任务",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    uiState.tasks.forEach { task ->
                        TaskCard(task = task)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (uiState.tasks.isEmpty()) {
                        Text(
                            text = "暂无任务，先休息一下吧 🎉",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── 今日状态面板 ──
            StatusPanel(
                postureStatus = uiState.postureStatus,
                focusStatus = uiState.focusStatus,
                todayStars = uiState.todayStars
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ── 开始学习按钮 ──
            Button(
                onClick = onStartLearning,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "🚀 开始学习",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── 荣誉星星 ──
            HonorStars(count = uiState.todayStars)
        }
    }
}
