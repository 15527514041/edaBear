package com.yidaxiong.app.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

/**
 * 权限申请辅助类
 *
 * 易达熊所需权限：
 * - CAMERA：坐姿/分心检测
 * - SYSTEM_ALERT_WINDOW：悬浮窗提醒
 */
object PermissionHelper {

    val REQUIRED_PERMISSIONS = arrayOf(
        Manifest.permission.CAMERA
    )

    /**
     * 检查所有必需权限是否已授权
     */
    fun hasAllPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(context, it) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * 检查是否需要显示权限说明
     */
    fun shouldShowPermissionRationale(activity: ComponentActivity): Boolean {
        return REQUIRED_PERMISSIONS.any {
            activity.shouldShowRequestPermissionRationale(it)
        }
    }
}
