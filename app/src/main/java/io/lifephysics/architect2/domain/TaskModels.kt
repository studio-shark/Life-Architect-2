package io.lifephysics.architect2.domain

enum class TaskCategory {
    HABITS, ENERGY, DESIRE, CHOICES, TIME
}

enum class TaskStatus {
    PENDING, COMPLETED
}

enum class TaskDifficulty(val xpValue: Int) {
    EASY_START(100),
    SOME_WEIGHT(250),
    HEAVY_WEIGHT(500)
}