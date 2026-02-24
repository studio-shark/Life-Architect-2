package io.lifephysics.architect2.data

import io.lifephysics.architect2.data.db.dao.GoalDao
import io.lifephysics.architect2.data.db.dao.TaskDao
import io.lifephysics.architect2.data.db.dao.UserDao
import io.lifephysics.architect2.data.db.entity.GoalEntity
import io.lifephysics.architect2.data.db.entity.TaskEntity
import io.lifephysics.architect2.data.db.entity.UserEntity
import kotlinx.coroutines.flow.Flow

/**
 * Repository module for handling data operations.
 *
 * This class abstracts access to multiple data sources (in this case, only the Room DAOs)
 * and provides a clean API for the rest of the application to access data.
 * It is the single source of truth for all application data.
 */
class AppRepository(
    private val userDao: UserDao,
    private val goalDao: GoalDao,
    private val taskDao: TaskDao
) {

    // --- User Functions ---

    fun observeUser(googleId: String): Flow<UserEntity?> = userDao.observeUser(googleId)

    suspend fun upsertUser(user: UserEntity) = userDao.upsertUser(user)

    // --- User convenience wrappers used by MainViewModel (offline-first) ---

    /**
     * Observes the local offline-first user. Delegates to [UserDao.getLocalUser].
     * Used by [MainViewModel] before Google Sign-In is wired up.
     */
    fun getUser(): Flow<UserEntity?> = userDao.getLocalUser()

    /**
     * Updates the local user. Delegates to [upsertUser].
     */
    suspend fun updateUser(user: UserEntity) = upsertUser(user)

    // --- Goal Functions ---

    fun observeGoalsForUser(userId: String): Flow<List<GoalEntity>> = goalDao.observeGoalsForUser(userId)

    suspend fun upsertGoal(goal: GoalEntity) = goalDao.upsertGoal(goal)

    suspend fun deleteGoal(goalId: String) = goalDao.deleteGoalById(goalId)

    // --- Task Functions ---

    fun observeTasksForUser(userId: String): Flow<List<TaskEntity>> = taskDao.observeTasksForUser(userId)

    fun observePendingTasksForUser(userId: String): Flow<List<TaskEntity>> = taskDao.observePendingTasksForUser(userId)

    suspend fun upsertTask(task: TaskEntity) = taskDao.upsertTask(task)

    suspend fun deleteTask(taskId: String) = taskDao.deleteTaskById(taskId)

    // --- Task convenience wrappers used by MainViewModel ---

    /**
     * Returns all tasks for the local offline-first user. Delegates to [observeTasksForUser].
     */
    fun getAllTasks(): Flow<List<TaskEntity>> = observeTasksForUser("local_user")

    /**
     * Inserts or updates a task. Delegates to [upsertTask].
     */
    suspend fun insertTask(task: TaskEntity) = upsertTask(task)

    /**
     * Updates an existing task. Delegates to [upsertTask].
     */
    suspend fun updateTask(task: TaskEntity) = upsertTask(task)
}
