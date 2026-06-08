package com.yidaxiong.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * 易达熊主题
 *
 * 温暖、明亮的儿童友好配色，固定浅色模式（不考虑深色模式）。
 */

private val YiDaXiongColorScheme = lightColorScheme(
    primary = Orange500,
    onPrimary = PureWhite,
    primaryContainer = Orange300,
    onPrimaryContainer = Orange700,
    secondary = Teal400,
    onSecondary = PureWhite,
    secondaryContainer = Teal300,
    onSecondaryContainer = Teal700,
    background = WarmWhite,
    onBackground = DarkText,
    surface = PureWhite,
    onSurface = DarkText,
    surfaceVariant = LightGray,
    onSurfaceVariant = MediumGray,
    error = ErrorRed,
    onError = PureWhite
)

@Composable
fun YiDaXiongTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = YiDaXiongColorScheme,
        typography = YiDaXiongTypography,
        content = content
    )
}
