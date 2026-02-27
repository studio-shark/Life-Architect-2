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
 */
@Database(
    entities = [
        UserEntity::class,
        GoalEntity::class,
        TaskEntity::class
    ],
    version = 6,
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

        /** Migration from v5 to v6: adds streak and weekly goal columns to the users table. */
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "life_architect_database"
                )
                    .addMigrations(MIGRATION_5_6)
                    .addCallback(DISABLE_FOREIGN_KEYS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
