package io.lifephysics.architect2.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import java.util.UUID

/**
 * Represents a single task or "quest" in the database.
 *
 * Tasks belong to a goal and a user. The [isCompleted] boolean is a
 * computed convenience field kept in sync with [status] so the UI layer
 * can use a simple boolean check without parsing strings.
 *
 * @property id The unique identifier for the task.
 * @property goalId The ID of the goal this task belongs to.
 * @property userId The ID of the user who owns this task.
 * @property title The title of the task.
 * @property description A detailed description of the task.
 * @property category The category of the task (e.g., Habits, Energy).
 * @property status The current status of the task (e.g., pending, completed).
 * @property isCompleted Convenience boolean, true when status == "completed".
 * @property difficulty The difficulty level of the task, used for calculating XP.
 * @property prerequisites A list of sub-tasks that must be completed before this task.
 * @property createdAt The timestamp when the task was created.
 * @property completedAt The timestamp when the task was completed.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goal_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["google_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TaskEntity(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),

    @ColumnInfo(name = "goal_id", index = true)
    val goalId: String = "",

    @ColumnInfo(name = "user_id", index = true)
    val userId: String = "local_user",

    val title: String,

    val description: String = "",

    val category: String = "",

    val status: String = "pending",

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false, // Kept in sync with status by the ViewModel

    val difficulty: String,

    val prerequisites: List<Prerequisite> = emptyList(),

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "completed_at")
    val completedAt: Long? = null
)

/**
 * Represents a single prerequisite (sub-task) for a parent TaskEntity.
 * This is an embedded data class, not a separate table.
 */
data class Prerequisite(
    val id: String = UUID.randomUUID().toString(),
    val label: String,
    val completed: Boolean = false,
    val completedAt: Long? = null
)
