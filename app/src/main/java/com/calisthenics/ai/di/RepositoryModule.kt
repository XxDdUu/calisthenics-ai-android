package com.calisthenics.ai.di

import com.calisthenics.ai.data.repository.AnalysisRepositoryImpl
import com.calisthenics.ai.data.repository.AdminRepositoryImpl
import com.calisthenics.ai.data.repository.AuthRepositoryImpl
import com.calisthenics.ai.data.repository.ExerciseRepositoryImpl
import com.calisthenics.ai.data.repository.WorkoutPlanRepositoryImpl
import com.calisthenics.ai.data.repository.WorkoutSessionRepositoryImpl
import com.calisthenics.ai.domain.repository.AnalysisRepository
import com.calisthenics.ai.domain.repository.AdminRepository
import com.calisthenics.ai.domain.repository.AuthRepository
import com.calisthenics.ai.domain.repository.ExerciseRepository
import com.calisthenics.ai.domain.repository.WorkoutPlanRepository
import com.calisthenics.ai.domain.repository.WorkoutSessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAdminRepository(
        impl: AdminRepositoryImpl
    ): AdminRepository

    @Binds
    @Singleton
    abstract fun bindExerciseRepository(
        impl: ExerciseRepositoryImpl
    ): ExerciseRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutPlanRepository(
        impl: WorkoutPlanRepositoryImpl
    ): WorkoutPlanRepository

    @Binds
    @Singleton
    abstract fun bindWorkoutSessionRepository(
        impl: WorkoutSessionRepositoryImpl
    ): WorkoutSessionRepository

    @Binds
    @Singleton
    abstract fun bindAnalysisRepository(
        impl: AnalysisRepositoryImpl
    ): AnalysisRepository

    @Binds
    @Singleton
    abstract fun bindLiveAnalysisRepository(
        impl: com.calisthenics.ai.data.repository.LiveAnalysisRepositoryImpl
    ): com.calisthenics.ai.domain.repository.LiveAnalysisRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        impl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        impl: com.calisthenics.ai.data.repository.ChatRepositoryImpl
    ): com.calisthenics.ai.domain.repository.ChatRepository

    @Binds
    @Singleton
    abstract fun bindCoachClientRepository(
        impl: com.calisthenics.ai.data.repository.CoachClientRepositoryImpl
    ): com.calisthenics.ai.domain.repository.CoachClientRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: com.calisthenics.ai.data.repository.ProfileRepositoryImpl
    ): com.calisthenics.ai.domain.repository.ProfileRepository

    @Binds
    @Singleton
    abstract fun bindAnalyticsRepository(
        impl: com.calisthenics.ai.data.repository.AnalyticsRepositoryImpl
    ): com.calisthenics.ai.domain.repository.AnalyticsRepository

    @Binds
    @Singleton
    abstract fun bindBodyMetricsRepository(
        impl: com.calisthenics.ai.data.repository.BodyMetricsRepositoryImpl
    ): com.calisthenics.ai.domain.repository.BodyMetricsRepository
}
