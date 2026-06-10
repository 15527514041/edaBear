package com.yidaxiong.app.ui.detection

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.yidaxiong.app.ui.detection.components.AlertBanner
import com.yidaxiong.app.ui.detection.components.DetectionOverlay

/**
 * 检测页面
 *
 * 相机实时预览 + 坐姿/专注度检测 Overlay + 违规弹窗/语音提醒。
 * 进入页面时自动请求相机权限，授权后自动启动检测。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetectionScreen(
    onBack: () -> Unit,
    viewModel: DetectionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // ── 相机权限请求 ──
    var hasCameraPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    // 首次进入自动请求权限
    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    // 权限授予后自动启动检测
    var hasAutoStarted by remember { mutableStateOf(false) }
    LaunchedEffect(hasCameraPermission) {
        if (hasCameraPermission && !hasAutoStarted) {
            hasAutoStarted = true
            viewModel.startDetection()
        }
    }

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
            if (hasCameraPermission) {
                // ── 相机实时预览 + 检测引擎 ──
                CameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onFrame = { imageProxy -> viewModel.onCameraFrame(imageProxy) }
                )

                // ── 检测信息叠加层（坐姿/专注度/时长） ──
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
                            viewModel.stopDetection()
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

                // 自动消失提醒
                if (uiState.alertMessage != null) {
                    LaunchedEffect(uiState.alertMessage) {
                        kotlinx.coroutines.delay(4000)
                        viewModel.onAlertDismissed()
                    }
                }
            } else {
                // ── 权限未授予：提示 ──
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📷",
                        style = MaterialTheme.typography.displayLarge
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "需要相机权限进行坐姿检测",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "易达熊使用前置摄像头实时检测坐姿和专注度\n不会录制或上传任何视频",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("授予相机权限")
                    }
                }
            }

            // ── 模型加载中提示 ──
            if (uiState.isDetecting && !uiState.isModelReady) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("正在加载检测模型...")
                        }
                    }
                }
            }
        }
    }
}
