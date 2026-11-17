package com.calisthenics.ai.di

import android.content.Context
import androidx.room.Room
import com.calisthenics.ai.data.local.database.AppDatabase
import com.calisthenics.ai.data.local.database.WorkoutScheduleDao
import com.calisthenics.ai.data.local.database.WorkoutSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "calisthenics_ai.db"
        )
            .addMigrations(
                AppDatabase.MIGRATION_1_2,
                AppDatabase.MIGRATION_2_3,
                AppDatabase.MIGRATION_3_4
            )
            .build()
    }

    @Provides
    @Singleton
    fun provideWorkoutScheduleDao(
        database: AppDatabase
    ): WorkoutScheduleDao {
        return database.workoutScheduleDao()
    }

    @Provides
    @Singleton
    fun provideWorkoutSessionDao(
        database: AppDatabase
    ): WorkoutSessionDao {
        return database.workoutSessionDao()
    }
}
