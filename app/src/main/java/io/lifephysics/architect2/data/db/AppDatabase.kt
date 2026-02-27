package io.lifephysics.architect2.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import io.lifephysics.architect2.data.db.dao.GoalDao
import io.lifephysics.architect2.data.db.dao.TaskDao
import io.lifephysics.architect2.data.db.dao.UserDao
import io.lifephysics.architect2.data.db.entity.GoalEntity
import io.lifephysics.architect2.data.db.entity.TaskEntity
import io.lifephysics.architect2.data.db.entity.UserEntity

/**
 * Version history:
 *   1 — Initial schema
 *   2 — Added task completion fields
 *   3 — Added OwnedAvatarEntity; replaced ownedAvatarIds/selectedAvatarId with equippedAvatarId
 *   4 — Replaced OwnedAvatarEntity with OwnedPartEntity; replaced equippedAvatarId with avatarConfig string
 *   5 — Removed OwnedPartEntity and coins column (shop system decommissioned)
 *   6 — Added streak and weekly goal columns to users table
 *   7 — Replaced weekly goal columns with weekly_streak_claimed; added monthly_milestone_claimed
 */
@Database(
    entities = [
        UserEntity::class,
        GoalEntity::class,
        TaskEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun goalDao(): GoalDao
    abstract fun taskDao(): TaskDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val DISABLE_FOREIGN_KEYS = object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("PRAGMA foreign_keys = OFF")
            }
        }

        /** Migration v5 → v6: adds streak and weekly goal columns. */
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE users ADD COLUMN daily_streak INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE users ADD COLUMN last_completion_day INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE users ADD COLUMN tasks_completed_today INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE users ADD COLUMN today_reset_day INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE users ADD COLUMN weekly_goal_target INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE users ADD COLUMN weekly_goal_progress INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE users ADD COLUMN weekly_goal_week INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE users ADD COLUMN weekly_goal_claimed INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE users ADD COLUMN monthly_milestone_claimed INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** Migration v6 → v7: replaces weekly goal columns with weekly_streak_claimed. */
        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add the new simplified column
                database.execSQL("ALTER TABLE users ADD COLUMN weekly_streak_claimed INTEGER NOT NULL DEFAULT 0")
                // Drop the old weekly goal columns (SQLite requires recreating the table)
                database.execSQL("""
                    CREATE TABLE users_new (
                        google_id TEXT NOT NULL PRIMARY KEY,
                        email TEXT NOT NULL DEFAULT '',
                        name TEXT NOT NULL DEFAULT 'Adventurer',
                        picture_url TEXT,
                        level INTEGER NOT NULL DEFAULT 1,
                        xp INTEGER NOT NULL DEFAULT 0,
                        total_xp INTEGER NOT NULL DEFAULT 0,
                        avatar_config TEXT,
                        theme_preference TEXT NOT NULL DEFAULT 'SYSTEM',
                        daily_streak INTEGER NOT NULL DEFAULT 0,
                        last_completion_day INTEGER NOT NULL DEFAULT 0,
                        tasks_completed_today INTEGER NOT NULL DEFAULT 0,
                        today_reset_day INTEGER NOT NULL DEFAULT 0,
                        weekly_streak_claimed INTEGER NOT NULL DEFAULT 0,
                        monthly_milestone_claimed INTEGER NOT NULL DEFAULT 0
                    )
                """.trimIndent())
                database.execSQL("""
                    INSERT INTO users_new (
                        google_id, email, name, picture_url, level, xp, total_xp,
                        avatar_config, theme_preference, daily_streak, last_completion_day,
                        tasks_completed_today, today_reset_day, weekly_streak_claimed,
                        monthly_milestone_claimed
                    )
                    SELECT
                        google_id, email, name, picture_url, level, xp, total_xp,
                        avatar_config, theme_preference, daily_streak, last_completion_day,
                        tasks_completed_today, today_reset_day, 0,
                        monthly_milestone_claimed
                    FROM users
                """.trimIndent())
                database.execSQL("DROP TABLE users")
                database.execSQL("ALTER TABLE users_new RENAME TO users")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "life_architect_database"
                )
                    .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration()
                    .addCallback(DISABLE_FOREIGN_KEYS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
