package io.lifephysics.architect2.data

import io.lifephysics.architect2.data.db.dao.GoalDao
import io.lifephysics.architect2.data.db.dao.OwnedAvatarDao
import io.lifephysics.architect2.data.db.dao.TaskDao
import io.lifephysics.architect2.data.db.dao.UserDao
import io.lifephysics.architect2.data.db.entity.GoalEntity
import io.lifephysics.architect2.data.db.entity.OwnedAvatarEntity
import io.lifephysics.architect2.data.db.entity.TaskEntity
import io.lifephysics.architect2.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

/**
 * Repository module for handling data operations.
 * Single source of truth for all application data.
 */
class AppRepository(
    private val userDao: UserDao,
    private val goalDao: GoalDao,
    private val taskDao: TaskDao,
    private val ownedAvatarDao: OwnedAvatarDao
) {

    // --- User ---

    fun observeUser(googleId: String): Flow<UserEntity?> = userDao.observeUser(googleId)
    fun getUser(): Flow<UserEntity?> = userDao.getLocalUser()
    suspend fun upsertUser(user: UserEntity) = userDao.upsertUser(user)
    suspend fun updateUser(user: UserEntity) = upsertUser(user)
    suspend fun updateUserTheme(theme: Theme) = userDao.updateUserTheme(theme.name)

    // --- Goals ---

    fun observeGoalsForUser(userId: String): Flow<List<GoalEntity>> = goalDao.observeGoalsForUser(userId)
    suspend fun upsertGoal(goal: GoalEntity) = goalDao.upsertGoal(goal)
    suspend fun deleteGoal(goalId: String) = goalDao.deleteGoalById(goalId)

    // --- Tasks ---

    fun observeTasksForUser(userId: String): Flow<List<TaskEntity>> = taskDao.observeTasksForUser(userId)
    fun observePendingTasksForUser(userId: String): Flow<List<TaskEntity>> = taskDao.observePendingTasksForUser(userId)
    fun getCompletedTasks(): Flow<List<TaskEntity>> = taskDao.observeCompletedTasksForUser("local_user")
    fun getAllTasks(): Flow<List<TaskEntity>> = observeTasksForUser("local_user")
    suspend fun upsertTask(task: TaskEntity) = taskDao.upsertTask(task)
    suspend fun insertTask(task: TaskEntity) = upsertTask(task)
    suspend fun updateTask(task: TaskEntity) = upsertTask(task)
    suspend fun deleteTask(taskId: String) = taskDao.deleteTaskById(taskId)

    // --- Avatar Shop ---

    /**
     * Observes the list of avatar IDs owned by the local user as a reactive Flow.
     * The Shop screen will automatically update when a new avatar is purchased.
     */
    fun getOwnedAvatarIds(): Flow<List<Int>> =
        ownedAvatarDao.observeOwnedAvatarIds("local_user")

    /**
     * Purchases an avatar for the local user.
     *
     * This is an atomic operation:
     *   1. Deducts the avatar's price from the user's coin balance.
     *   2. Inserts an ownership record into [OwnedAvatarEntity].
     *
     * The caller (ViewModel) is responsible for verifying the user has sufficient
     * coins before calling this method.
     *
     * @param avatarId The ID of the avatar to purchase.
     * @param price    The coin cost to deduct from the user's balance.
     */
    suspend fun purchaseAvatar(avatarId: Int, price: Int) {
        val user = getUser().first() ?: return
        val updatedUser = user.copy(coins = user.coins - price)
        upsertUser(updatedUser)
        ownedAvatarDao.insertOwnedAvatar(
            OwnedAvatarEntity(userId = "local_user", avatarId = avatarId)
        )
    }

    /**
     * Sets the given avatar as the user's currently equipped avatar.
     *
     * @param avatarId The ID of the avatar to equip.
     */
    suspend fun equipAvatar(avatarId: Int) {
        val user = getUser().first() ?: return
        upsertUser(user.copy(equippedAvatarId = avatarId))
    }
}
