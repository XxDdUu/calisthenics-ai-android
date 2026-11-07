package com.calisthenics.ai.di

import com.calisthenics.ai.ui.workoutsession.timer.SystemTimeSource
import com.calisthenics.ai.ui.workoutsession.timer.TimeSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class TimerModule {

    @Binds
    @Singleton
    abstract fun bindTimeSource(impl: SystemTimeSource): TimeSource
}

