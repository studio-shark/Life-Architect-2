package io.lifephysics.architect2

import android.app.Application
import io.lifephysics.architect2.data.AppRepository
import io.lifephysics.architect2.data.db.AppDatabase
import io.lifephysics.architect2.data.db.entity.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Custom Application class for Life Architect 2.
 *
 * Holds singleton instances of the database and repository, and ensures
 * the local user record is seeded into the database on first launch.
 */
class LifeArchitectApplication : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val database by lazy {
        AppDatabase.getDatabase(applicationContext)
    }

    val repository by lazy {
        AppRepository(
            userDao = database.userDao(),
            goalDao = database.goalDao(),
            taskDao = database.taskDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Ensure the local user row exists before any ViewModel tries to read it.
        // This is idempotent — upsertUser will update if the row already exists,
        // or insert a fresh default user if it does not.
        applicationScope.launch {
            val existing = repository.getUser().first()
            if (existing == null) {
                repository.upsertUser(UserEntity(googleId = "local_user"))
            }
        }
    }
}
