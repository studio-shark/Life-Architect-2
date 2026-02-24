package io.lifephysics.architect2.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import io.lifephysics.architect2.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the [UserEntity] table.
 */
@Dao
interface UserDao {

    /**
     * Inserts or updates a user in the database. If the user already exists, it will be replaced.
     *
     * @param user The user entity to insert or update.
     */
    @Upsert
    suspend fun upsertUser(user: UserEntity)

    /**
     * Observes a single user by their unique Google ID.
     *
     * @param googleId The Google ID of the user to observe.
     * @return A [Flow] that emits the [UserEntity] whenever it changes.
     */
    @Query("SELECT * FROM users WHERE google_id = :googleId LIMIT 1")
    fun observeUser(googleId: String): Flow<UserEntity?>

    /**
     * Observes the offline-first local user.
     * This is a convenience method used by [MainViewModel] before Google Sign-In is wired up.
     * It queries for the singleton user whose [UserEntity.googleId] is "local_user".
     *
     * @return A [Flow] that emits the local [UserEntity] whenever it changes.
     */
    @Query("SELECT * FROM users WHERE google_id = 'local_user' LIMIT 1")
    fun getLocalUser(): Flow<UserEntity?>

    /**
     * Updates the theme preference for the local offline-first user.
     * Accepts a string value matching the [io.lifephysics.architect2.data.Theme] enum name
     * ("LIGHT", "DARK", or "SYSTEM").
     *
     * @param theme The name of the theme to persist.
     */
    @Query("UPDATE users SET theme_preference = :theme WHERE google_id = 'local_user'")
    suspend fun updateUserTheme(theme: String)
}
