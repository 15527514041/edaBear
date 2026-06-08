package com.yidaxiong.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.yidaxiong.app.ui.navigation.YiDaXiongNavHost
import com.yidaxiong.app.ui.theme.YiDaXiongTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 易达熊 — 单 Activity 入口
 *
 * 平板横屏模式，通过 Navigation Compose 管理页面跳转。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YiDaXiongTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    YiDaXiongNavHost()
                }
            }
        }
    }
}
