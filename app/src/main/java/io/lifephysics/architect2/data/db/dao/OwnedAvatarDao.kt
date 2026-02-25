package io.lifephysics.architect2.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.lifephysics.architect2.data.db.entity.OwnedAvatarEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the [OwnedAvatarEntity] table.
 *
 * All queries are scoped to a specific [userId] so this DAO is ready for
 * multi-user support without any changes.
 */
@Dao
interface OwnedAvatarDao {

    /**
     * Observes all avatar IDs owned by the given user as a reactive Flow.
     * The UI will automatically update when a new avatar is purchased.
     */
    @Query("SELECT avatar_id FROM owned_avatars WHERE user_id = :userId")
    fun observeOwnedAvatarIds(userId: String): Flow<List<Int>>

    /**
     * Inserts a new ownership record. IGNORE on conflict prevents crashes
     * if the user somehow triggers a double-purchase.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOwnedAvatar(entity: OwnedAvatarEntity)

    /**
     * Checks whether a specific avatar is already owned by the user.
     * Returns 1 if owned, 0 if not.
     */
    @Query("SELECT COUNT(*) FROM owned_avatars WHERE user_id = :userId AND avatar_id = :avatarId")
    suspend fun isAvatarOwned(userId: String, avatarId: Int): Int
}
