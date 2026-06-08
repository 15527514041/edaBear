package com.yidaxiong.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 检测引擎依赖注入
 *
 * 所有检测引擎类（MediaPipeFaceMesh、PostureRuleEngine、AlertManager 等）
 * 均已通过 @Inject constructor + @Singleton 标注，Hilt 自动发现注入。
 * Phase 4-5 如有需要自定义绑定时，在此模块添加 @Provides。
 */
@Module
@InstallIn(SingletonComponent::class)
object DetectionModule
