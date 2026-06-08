package com.yidaxiong.app.ui.navigation

/**
 * 易达熊导航路由定义
 */
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object Detection : Screen("detection")
    data object Report : Screen("report")
}
