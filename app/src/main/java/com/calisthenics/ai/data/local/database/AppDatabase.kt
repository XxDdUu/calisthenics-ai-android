package com.calisthenics.ai.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedWorkoutPlanEntity::class,
        CachedWorkoutPlanDayEntity::class,
        CachedWorkoutPlanExerciseEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutScheduleDao(): WorkoutScheduleDao

    companion object {
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_workout_plan_days ADD COLUMN description TEXT")
                db.execSQL(
                    "ALTER TABLE cached_workout_plan_days ADD COLUMN isRestDay INTEGER NOT NULL DEFAULT 0"
                )
            }
        }
    }
}
