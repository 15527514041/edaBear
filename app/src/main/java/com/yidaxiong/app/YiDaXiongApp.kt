package com.yidaxiong.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * 易达熊 — AI 儿童陪读应用
 *
 * 专为低配安卓平板优化，纯离线坐姿检测与分心检测。
 */
@HiltAndroidApp
class YiDaXiongApp : Application() {
    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: YiDaXiongApp
            private set
    }
}
