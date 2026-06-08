package com.yidaxiong.app.ui.detection

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yidaxiong.app.ui.detection.components.AlertBanner
import com.yidaxiong.app.ui.detection.components.DetectionOverlay

/**
 * 检测页面 — MVP 骨架
 *
 * 相机预览 + 检测状态 Overlay + 违规提醒
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionScreen(
    onBack: () -> Unit,
    viewModel: DetectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "学习检测中",
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("← 返回", color = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // ── 相机预览占位 ──
            // Phase 3 实现 CameraPreview
            CameraPreviewPlaceholder()

            // ── 检测信息叠加层 ──
            DetectionOverlay(
                postureStatus = uiState.postureStatus,
                focusStatus = uiState.focusStatus,
                elapsedTime = uiState.elapsedTime
            )

            // ── 底部操作按钮 ──
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                OutlinedButton(
                    onClick = { viewModel.toggleDetection() },
                    modifier = Modifier
                        .width(160.dp)
                        .height(48.dp)
                ) {
                    Text(
                        if (uiState.isDetecting) "⏸️ 暂停" else "▶️ 继续",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Button(
                    onClick = {
                        onBack()
                    },
                    modifier = Modifier
                        .width(160.dp)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Text(
                        "✅ 完成任务",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }

            // ── 违规提醒横幅 ──
            uiState.alertMessage?.let { message ->
                AlertBanner(message = message)
            }
        }
    }
}

/**
 * 相机预览占位 - Phase 3 替换为 CameraPreview Composable
 */
@Composable
private fun CameraPreviewPlaceholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "📷",
                    style = MaterialTheme.typography.headlineLarge
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "相机预览区域\n(Phase 3 实现)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
