package com.toolsboox.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Hilt module that enables HiltWorkerFactory for WorkManager.
 *
 * BaseApplication implements Configuration.Provider and injects
 * HiltWorkerFactory, which allows @HiltWorker-annotated workers
 * to participate in Hilt DI. This module is installed in the
 * SingletonComponent so it is available application-wide.
 *
 * Note: CalendarSyncWorker constructs its own deps internally
 * (because the app's DI is ActivityComponent-scoped), so this
 * module exists primarily to satisfy the Configuration.Provider
 * contract and enable future workers that may use @HiltWorker.
 */
@Module
@InstallIn(SingletonComponent::class)
object WorkerModule
