package com.yidaxiong.app.ui.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * 今日学习状态面板
 */
@Composable
fun StatusPanel(
    postureStatus: String,
    focusStatus: String,
    todayStars: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 坐姿状态
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🪑",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "坐姿",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = postureStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }

            // 专注度状态
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "🎯",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "专注度",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = focusStatus,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
