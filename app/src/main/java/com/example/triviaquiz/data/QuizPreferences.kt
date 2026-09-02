package com.example.triviaquiz.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * Manages local storage using SharedPreferences for:
 * - Quiz history
 * - Best score
 * - Quizzes played count
 * - Achievements
 * - Settings (sound, vibration, timer)
 */
class QuizPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("trivia_quiz_prefs", Context.MODE_PRIVATE)

    // ==================== QUIZ HISTORY ====================

    /**
     * Saves a completed quiz result to history.
     */
    fun saveQuizResult(
        category: String,
        difficulty: String,
        totalQuestions: Int,
        correct: Int,
        wrong: Int,
        skipped: Int,
        bestStreak: Int,
        flagged: Int,
        timestamp: Long = System.currentTimeMillis()
    ) {
        val history = getQuizHistory().toMutableList()
        val entry = JSONObject().apply {
            put("category", category)
            put("difficulty", difficulty)
            put("totalQuestions", totalQuestions)
            put("correct", correct)
            put("wrong", wrong)
            put("skipped", skipped)
            put("bestStreak", bestStreak)
            put("flagged", flagged)
            put("timestamp", timestamp)
        }
        history.add(0, entry.toString()) // Add to beginning (most recent first)

        // Keep only last 50 entries
        if (history.size > 50) {
            history.subList(50, history.size).clear()
        }

        prefs.edit().putString("quiz_history", JSONArray(history).toString()).apply()

        // Update best score and quiz count
        updateBestScore(correct, totalQuestions)
        incrementQuizzesPlayed()
    }

    /**
     * Returns the list of saved quiz results.
     */
    fun getQuizHistory(): List<String> {
        val json = prefs.getString("quiz_history", "[]") ?: "[]"
        val array = JSONArray(json)
        return (0 until array.length()).map { array.getString(it) }
    }

    /**
     * Clears all quiz history.
     */
    fun clearHistory() {
        prefs.edit().remove("quiz_history").apply()
        prefs.edit().putInt("best_score", 0).apply()
        prefs.edit().putInt("best_total", 0).apply()
        prefs.edit().putInt("quizzes_played", 0).apply()
    }

    // ==================== BEST SCORE ====================

    private fun updateBestScore(correct: Int, total: Int) {
        val currentBest = prefs.getInt("best_score", 0)
        val currentBestTotal = prefs.getInt("best_total", 0)
        if (correct > currentBest || (correct == currentBest && total < currentBestTotal)) {
            prefs.edit()
                .putInt("best_score", correct)
                .putInt("best_total", total)
                .apply()
        }
    }

    fun getBestScore(): Int = prefs.getInt("best_score", 0)
    fun getBestTotal(): Int = prefs.getInt("best_total", 0)

    // ==================== QUIZZES PLAYED ====================

    private fun incrementQuizzesPlayed() {
        val count = prefs.getInt("quizzes_played", 0)
        prefs.edit().putInt("quizzes_played", count + 1).apply()
    }

    fun getQuizzesPlayed(): Int = prefs.getInt("quizzes_played", 0)

    // ==================== ACHIEVEMENTS ====================

    private val achievementKeys = setOf(
        "first_quiz", "hot_streak", "perfect_score", "quiz_master", "fast_thinker"
    )

    fun unlockAchievement(key: String) {
        if (key in achievementKeys) {
            prefs.edit().putBoolean("achievement_$key", true).apply()
        }
    }

    fun isAchievementUnlocked(key: String): Boolean {
        return prefs.getBoolean("achievement_$key", false)
    }

    fun getUnlockedAchievements(): Set<String> {
        return achievementKeys.filter { prefs.getBoolean("achievement_$it", false) }.toSet()
    }

    // ==================== SETTINGS ====================

    fun isSoundEnabled(): Boolean = prefs.getBoolean("setting_sound", true)
    fun setSoundEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("setting_sound", enabled).apply()
    }

    fun isVibrationEnabled(): Boolean = prefs.getBoolean("setting_vibration", true)
    fun setVibrationEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("setting_vibration", enabled).apply()
    }

    fun isTimerEnabled(): Boolean = prefs.getBoolean("setting_timer", true)
    fun setTimerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("setting_timer", enabled).apply()
    }

    fun getTimerDuration(): Int = prefs.getInt("setting_timer_duration", 20)
    fun setTimerDuration(seconds: Int) {
        prefs.edit().putInt("setting_timer_duration", seconds).apply()
    }
}
