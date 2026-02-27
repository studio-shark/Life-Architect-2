package io.lifephysics.architect2.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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

    /**
     * Serialized [AvatarConfig] stored as a comma-separated string of part IDs.
     * Format: "backgroundId,headId,clothesId,eyebrowsId,eyesId,mouthId,accessoryId"
     * Example: "1,9,14,26,31,39,45"
     * Defaults to null — the ViewModel will call AvatarConfig() to get the default config.
     */
    @ColumnInfo(name = "avatar_config")
    val avatarConfig: String? = null,

    @ColumnInfo(name = "theme_preference")
    val themePreference: String = "SYSTEM"
)
