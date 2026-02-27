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
    val themePreference: String = "SYSTEM",

    // --- Streak & Goal Fields ---

    /** Number of consecutive days the user has completed at least one task. */
    @ColumnInfo(name = "daily_streak")
    val dailyStreak: Int = 0,

    /** The date (epoch day: millis / 86_400_000) of the last task completion, for streak tracking. */
    @ColumnInfo(name = "last_completion_day")
    val lastCompletionDay: Long = 0L,

    /** Number of tasks completed today, used for diminishing-returns XP calculation. */
    @ColumnInfo(name = "tasks_completed_today")
    val tasksCompletedToday: Int = 0,

    /** The epoch day when [tasksCompletedToday] was last reset. */
    @ColumnInfo(name = "today_reset_day")
    val todayResetDay: Long = 0L,

    /** The user's weekly task completion goal (e.g., 25 tasks). 0 = no goal set. */
    @ColumnInfo(name = "weekly_goal_target")
    val weeklyGoalTarget: Int = 0,

    /** Number of tasks completed in the current ISO week. */
    @ColumnInfo(name = "weekly_goal_progress")
    val weeklyGoalProgress: Int = 0,

    /** The ISO week number when [weeklyGoalProgress] was last reset. */
    @ColumnInfo(name = "weekly_goal_week")
    val weeklyGoalWeek: Int = 0,

    /** Whether the weekly goal XP payout has already been awarded this week. */
    @ColumnInfo(name = "weekly_goal_claimed")
    val weeklyGoalClaimed: Boolean = false,

    /** Whether the 30-day streak milestone XP has already been awarded this streak cycle. */
    @ColumnInfo(name = "monthly_milestone_claimed")
    val monthlyMilestoneClaimed: Boolean = false
)
