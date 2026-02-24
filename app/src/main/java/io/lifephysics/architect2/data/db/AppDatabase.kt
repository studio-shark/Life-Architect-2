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
 * The main Room database for the application.
 *
 * This class is the central access point to the persisted data. It lists all the
 * entities (tables) and provides abstract methods to get the DAOs.
 *
 * The singleton pattern in [getDatabase] ensures only one instance of the database
 * is ever created, preventing data corruption from multiple open connections.
 *
 * Foreign key constraints are disabled via a SQLite pragma for the offline-first build
 * so that tasks can be inserted without a matching goal or user row already existing.
 * Re-enable them once Google Sign-In and the Goals feature are fully wired up.
 */
@Database(
    entities = [UserEntity::class, GoalEntity::class, TaskEntity::class],
    version = 1,
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

        // Disables foreign key enforcement on every new database connection.
        // This allows tasks to be inserted without a matching goal/user row.
        private val DISABLE_FOREIGN_KEYS = object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("PRAGMA foreign_keys = OFF")
            }
        }

        /**
         * Returns the singleton instance of [AppDatabase], creating it if necessary.
         * The [synchronized] block ensures thread-safe creation on first access.
         *
         * @param context The application context used to build the database.
         */
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "life_architect_database"
                )
                    .addCallback(DISABLE_FOREIGN_KEYS)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
