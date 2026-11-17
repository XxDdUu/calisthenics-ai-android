package com.calisthenics.ai.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        CachedWorkoutPlanEntity::class,
        CachedWorkoutPlanDayEntity::class,
        CachedWorkoutPlanExerciseEntity::class,
        CachedActiveWorkoutSessionEntity::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutScheduleDao(): WorkoutScheduleDao
    abstract fun workoutSessionDao(): WorkoutSessionDao

    companion object {
        const val LEGACY_UNOWNED_USER_ID = -1L

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE cached_workout_plan_days ADD COLUMN description TEXT")
                db.execSQL(
                    "ALTER TABLE cached_workout_plan_days ADD COLUMN isRestDay INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Preserve existing v2 cached rows under LEGACY_UNOWNED_USER_ID (-1) so upgrades
                // are non-destructive and pre-upgrade unowned rows are never exposed to signed-in users.
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_workout_plans_v3` (
                        `ownerUserId` INTEGER NOT NULL,
                        `id` INTEGER NOT NULL,
                        `name` TEXT NOT NULL,
                        `splitType` TEXT NOT NULL,
                        `isCoachAssigned` INTEGER NOT NULL,
                        `coachName` TEXT,
                        `assignmentId` INTEGER,
                        `isActive` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`ownerUserId`, `id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `cached_workout_plans_v3` (
                        `ownerUserId`, `id`, `name`, `splitType`, `isCoachAssigned`, `coachName`,
                        `assignmentId`, `isActive`, `updatedAt`
                    )
                    SELECT
                        $LEGACY_UNOWNED_USER_ID, `id`, `name`, `splitType`, `isCoachAssigned`, `coachName`,
                        `assignmentId`, `isActive`, `updatedAt`
                    FROM `cached_workout_plans`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `cached_workout_plans`")
                db.execSQL("ALTER TABLE `cached_workout_plans_v3` RENAME TO `cached_workout_plans`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_workout_plans_ownerUserId` ON `cached_workout_plans` (`ownerUserId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_workout_plans_ownerUserId_isActive` ON `cached_workout_plans` (`ownerUserId`, `isActive`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_workout_plan_days_v3` (
                        `ownerUserId` INTEGER NOT NULL,
                        `id` INTEGER NOT NULL,
                        `planId` INTEGER NOT NULL,
                        `dayOfWeek` TEXT NOT NULL,
                        `sessionType` TEXT NOT NULL,
                        `name` TEXT,
                        `description` TEXT,
                        `isRestDay` INTEGER NOT NULL,
                        PRIMARY KEY(`ownerUserId`, `planId`, `dayOfWeek`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `cached_workout_plan_days_v3` (
                        `ownerUserId`, `id`, `planId`, `dayOfWeek`, `sessionType`, `name`,
                        `description`, `isRestDay`
                    )
                    SELECT
                        $LEGACY_UNOWNED_USER_ID, `id`, `planId`, `dayOfWeek`, `sessionType`, `name`,
                        `description`, `isRestDay`
                    FROM `cached_workout_plan_days`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `cached_workout_plan_days`")
                db.execSQL("ALTER TABLE `cached_workout_plan_days_v3` RENAME TO `cached_workout_plan_days`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_workout_plan_days_ownerUserId_planId` ON `cached_workout_plan_days` (`ownerUserId`, `planId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_workout_plan_days_ownerUserId_planId_id` ON `cached_workout_plan_days` (`ownerUserId`, `planId`, `id`)")

                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_workout_plan_exercises_v3` (
                        `ownerUserId` INTEGER NOT NULL,
                        `id` INTEGER NOT NULL,
                        `planId` INTEGER NOT NULL,
                        `dayId` INTEGER NOT NULL,
                        `exerciseId` INTEGER NOT NULL,
                        `exerciseName` TEXT NOT NULL,
                        `category` TEXT NOT NULL,
                        `difficulty` TEXT NOT NULL,
                        `muscleGroup` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `sets` INTEGER NOT NULL,
                        `reps` INTEGER,
                        `durationSeconds` INTEGER,
                        `restSeconds` INTEGER NOT NULL,
                        `displayOrder` INTEGER NOT NULL,
                        `notes` TEXT,
                        PRIMARY KEY(`ownerUserId`, `planId`, `dayId`, `id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT OR REPLACE INTO `cached_workout_plan_exercises_v3` (
                        `ownerUserId`, `id`, `planId`, `dayId`, `exerciseId`, `exerciseName`,
                        `category`, `difficulty`, `muscleGroup`, `description`, `sets`, `reps`,
                        `durationSeconds`, `restSeconds`, `displayOrder`, `notes`
                    )
                    SELECT
                        $LEGACY_UNOWNED_USER_ID, `id`, `planId`, `dayId`, `exerciseId`, `exerciseName`,
                        `category`, `difficulty`, `muscleGroup`, `description`, `sets`, `reps`,
                        `durationSeconds`, `restSeconds`, `displayOrder`, `notes`
                    FROM `cached_workout_plan_exercises`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `cached_workout_plan_exercises`")
                db.execSQL("ALTER TABLE `cached_workout_plan_exercises_v3` RENAME TO `cached_workout_plan_exercises`")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_workout_plan_exercises_ownerUserId_planId` ON `cached_workout_plan_exercises` (`ownerUserId`, `planId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_workout_plan_exercises_ownerUserId_dayId` ON `cached_workout_plan_exercises` (`ownerUserId`, `dayId`)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_cached_workout_plan_exercises_ownerUserId_planId_dayId` ON `cached_workout_plan_exercises` (`ownerUserId`, `planId`, `dayId`)")
            }
        }

        val MIGRATION_3_4: Migration = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_active_workout_sessions` (
                        `ownerUserId` INTEGER NOT NULL,
                        `sessionId` INTEGER NOT NULL,
                        `sessionJson` TEXT NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`ownerUserId`, `sessionId`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
