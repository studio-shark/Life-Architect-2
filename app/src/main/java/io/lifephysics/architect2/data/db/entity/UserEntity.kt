package io.lifephysics.architect2.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single user in the database.
 *
 * This entity stores the user's profile information, primarily from Google Sign-In,
 * as well as their gamification progress within the app.
 *
 * Note: For the current offline-first build, [googleId] defaults to "local_user"
 * so the app works without a network connection. Google Sign-In can be wired in later.
 *
 * @property googleId The unique identifier from Google Sign-In. Acts as the primary key.
 * @property email The user's email address.
 * @property name The user's display name.
 * @property pictureUrl The URL for the user's profile picture.
 * @property level The user's current level in the gamification system.
 * @property xp The user's current experience points within the current level.
 * @property totalXp The user's all-time total experience points.
 * @property coins The user's current balance of in-app currency.
 * @property ownedAvatarIds A list of avatar IDs that the user has unlocked.
 * @property selectedAvatarId The ID of the avatar the user currently has equipped.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "google_id")
    val googleId: String = "local_user", // Default for offline-first use

    val email: String = "",

    val name: String = "Adventurer",

    @ColumnInfo(name = "picture_url")
    val pictureUrl: String? = null,

    val level: Int = 1,

    val xp: Int = 0,

    @ColumnInfo(name = "total_xp")
    val totalXp: Int = 0,

    val coins: Int = 0,

    @ColumnInfo(name = "owned_avatar_ids")
    val ownedAvatarIds: List<String> = listOf("default"),

    @ColumnInfo(name = "selected_avatar_id")
    val selectedAvatarId: String = "default"
)
