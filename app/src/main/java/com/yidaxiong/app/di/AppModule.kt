package com.yidaxiong.app.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * 全局依赖注入模块
 *
 * 提供 Application 级别的单例依赖。
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule
