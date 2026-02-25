package io.lifephysics.architect2

import android.app.Application
import io.lifephysics.architect2.data.AppRepository
import io.lifephysics.architect2.data.db.AppDatabase

/**
 * Custom Application class for Life Architect 2.
 *
 * This class serves as the entry point for the app and holds singleton instances
 * of the database and repository, making them accessible to the rest of the app.
 */
class LifeArchitectApplication : Application() {

    /**
     * Lazily initialized instance of the Room database.
     * Uses the singleton factory in [AppDatabase] so only one connection is ever opened.
     */
    private val database by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    /**
     * Lazily initialized instance of the [AppRepository].
     * It depends on the database instance to get its DAOs.
     */
    val repository by lazy {
        AppRepository(
            userDao = database.userDao(),
            goalDao = database.goalDao(),
            taskDao = database.taskDao(),
            ownedAvatarDao = database.ownedAvatarDao()
        )
    }
}
