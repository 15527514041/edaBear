package com.yidaxiong.app.ui.detection.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 检测信息叠加层
 *
 * 显示坐姿状态、专注度、学习时长。
 */
@Composable
fun DetectionOverlay(
    postureStatus: String,
    focusStatus: String,
    elapsedTime: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // 坐姿状态
        StatusChip(label = "坐姿", status = postureStatus)
        // 学习时长
        StatusChip(label = "时长", status = elapsedTime)
        // 专注度
        StatusChip(label = "专注", status = focusStatus)
    }
}

@Composable
private fun StatusChip(
    label: String,
    status: String
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
