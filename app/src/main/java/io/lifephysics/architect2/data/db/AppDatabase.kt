package io.lifephysics.architect2.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
 */
@Database(
    entities = [
        UserEntity::class,
        GoalEntity::class,
        TaskEntity::class
    ],
    version = 5,
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

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "life_architect_database"
                )
                    .fallbackToDestructiveMigration()
                    .addCallback(DISABLE_FOREIGN_KEYS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
