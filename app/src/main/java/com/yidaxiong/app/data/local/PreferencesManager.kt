package com.yidaxiong.app.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "yidaxiong_prefs")

/**
 * 偏好存储管理
 *
 * 使用 DataStore 存储本地偏好设置。
 * MVP 阶段主要存储：星星数量、用户信息等。
 */
@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private val KEY_TODAY_STARS = intPreferencesKey("today_stars")
        private val KEY_NICKNAME = stringPreferencesKey("nickname")
        private val KEY_TOTAL_STARS = intPreferencesKey("total_stars")
    }

    val todayStars: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_TODAY_STARS] ?: 0
    }

    val nickname: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_NICKNAME] ?: "小朋友"
    }

    suspend fun setTodayStars(count: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TODAY_STARS] = count
        }
    }

    suspend fun setNickname(name: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NICKNAME] = name
        }
    }
}
