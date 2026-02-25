package io.lifephysics.architect2.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a single user in the database.
 *
 * @property googleId       Unique identifier. Defaults to "local_user" for offline-first use.
 * @property email          The user's email address.
 * @property name           The user's display name.
 * @property pictureUrl     URL for the user's profile picture.
 * @property level          Current level in the gamification system.
 * @property xp             Current XP within the current level.
 * @property totalXp        All-time total XP.
 * @property coins          Current in-app coin balance.
 * @property equippedAvatarId The ID of the avatar currently equipped. Defaults to 1 (free avatar).
 * @property themePreference  The user's chosen app theme: "LIGHT", "DARK", or "SYSTEM".
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey
    @ColumnInfo(name = "google_id")
    val googleId: String = "local_user",

    val email: String = "",
    val name: String = "Adventurer",

    @ColumnInfo(name = "picture_url")
    val pictureUrl: String? = null,

    val level: Int = 1,
    val xp: Int = 0,

    @ColumnInfo(name = "total_xp")
    val totalXp: Int = 0,

    val coins: Int = 0,

    /**
     * The ID of the avatar currently equipped by the user.
     * Defaults to 1, which is the free "Wandering Fox" avatar.
     * Ownership is tracked separately in the [OwnedAvatarEntity] table.
     */
    @ColumnInfo(name = "equipped_avatar_id")
    val equippedAvatarId: Int = 1,

    @ColumnInfo(name = "theme_preference")
    val themePreference: String = "SYSTEM"
)
