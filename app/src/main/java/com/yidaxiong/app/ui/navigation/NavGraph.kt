package com.yidaxiong.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yidaxiong.app.ui.home.HomeScreen
import com.yidaxiong.app.ui.detection.DetectionScreen
import com.yidaxiong.app.ui.report.ReportScreen

/**
 * 易达熊导航图
 *
 * MVP 阶段包含三个页面：主页、检测页、报告页（占位）
 */
@Composable
fun YiDaXiongNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onStartLearning = {
                    navController.navigate(Screen.Detection.route)
                }
            )
        }
        composable(Screen.Detection.route) {
            DetectionScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        composable(Screen.Report.route) {
            ReportScreen(
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
