package com.example.triviaquiz.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

class QuizPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("trivia_quiz_prefs", Context.MODE_PRIVATE)

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
        history.add(0, entry.toString())
        if (history.size > 50) history.subList(50, history.size).clear()
        prefs.edit().putString("quiz_history", JSONArray(history).toString()).apply()

        updateBestScore(correct, totalQuestions)
        incrementQuizzesPlayed()
        addStats(totalQuestions, correct, wrong, skipped, bestStreak, category)
        addCategoryPerformance(category, correct, totalQuestions)
        addRecentPerformance(correct, totalQuestions)
        addXP(correct * 10 + if (correct == totalQuestions) 50 else 0)
    }

    fun getQuizHistory(): List<String> {
        return try {
            val json = prefs.getString("quiz_history", "[]") ?: "[]"
            val array = JSONArray(json)
            (0 until array.length()).map { array.getString(it) }
        } catch (_: Exception) { emptyList() }
    }

    fun clearHistory() {
        prefs.edit()
            .remove("quiz_history")
            .putInt("best_score", 0)
            .putInt("best_total", 0)
            .putInt("quizzes_played", 0)
            .putInt("total_questions", 0)
            .putInt("total_correct", 0)
            .putInt("total_wrong", 0)
            .putInt("total_skipped", 0)
            .putInt("total_best_streak", 0)
            .remove("category_performance")
            .remove("recent_performance")
            .apply()
    }

    private fun updateBestScore(correct: Int, total: Int) {
        val currentBest = prefs.getInt("best_score", 0)
        val currentBestTotal = prefs.getInt("best_total", 0)
        if (correct > currentBest || (correct == currentBest && total < currentBestTotal)) {
            prefs.edit().putInt("best_score", correct).putInt("best_total", total).apply()
        }
    }

    fun getBestScore(): Int = prefs.getInt("best_score", 0)
    fun getBestTotal(): Int = prefs.getInt("best_total", 0)

    private fun incrementQuizzesPlayed() {
        prefs.edit().putInt("quizzes_played", prefs.getInt("quizzes_played", 0) + 1).apply()
    }

    fun getQuizzesPlayed(): Int = prefs.getInt("quizzes_played", 0)

    private fun addStats(questions: Int, correct: Int, wrong: Int, skipped: Int, streak: Int, category: String) {
        val e = prefs.edit()
        e.putInt("total_questions", prefs.getInt("total_questions", 0) + questions)
        e.putInt("total_correct", prefs.getInt("total_correct", 0) + correct)
        e.putInt("total_wrong", prefs.getInt("total_wrong", 0) + wrong)
        e.putInt("total_skipped", prefs.getInt("total_skipped", 0) + skipped)
        val bestStreak = prefs.getInt("total_best_streak", 0)
        if (streak > bestStreak) e.putInt("total_best_streak", streak)
        e.apply()
    }

    fun getTotalQuestionsAnswered(): Int = prefs.getInt("total_questions", 0)
    fun getTotalCorrect(): Int = prefs.getInt("total_correct", 0)
    fun getTotalWrong(): Int = prefs.getInt("total_wrong", 0)
    fun getTotalSkipped(): Int = prefs.getInt("total_skipped", 0)
    fun getBestStreak(): Int = prefs.getInt("total_best_streak", 0)

    fun getAccuracy(): Int {
        val total = getTotalQuestionsAnswered()
        if (total == 0) return 0
        return (getTotalCorrect() * 100) / total
    }

    fun getAverageScore(): Float {
        val total = getTotalQuestionsAnswered()
        if (total == 0) return 0f
        return (getTotalCorrect().toFloat() / total) * 10f
    }

    private fun addCategoryPerformance(category: String, correct: Int, total: Int) {
        if (category == "Any Category") return
        val json = prefs.getString("category_performance", "{}") ?: "{}"
        val obj = JSONObject(json)
        val existing = if (obj.has(category)) obj.getJSONObject(category) else JSONObject()
        val prevCorrect = existing.optInt("correct", 0)
        val prevTotal = existing.optInt("total", 0)
        existing.put("correct", prevCorrect + correct)
        existing.put("total", prevTotal + total)
        obj.put(category, existing)
        prefs.edit().putString("category_performance", obj.toString()).apply()
    }

    fun getCategoryPerformance(): Map<String, Pair<Int, Int>> {
        return try {
            val json = prefs.getString("category_performance", "{}") ?: "{}"
            val obj = JSONObject(json)
            val result = mutableMapOf<String, Pair<Int, Int>>()
            for (key in obj.keys()) {
                val entry = obj.getJSONObject(key)
                result[key] = Pair(entry.getInt("correct"), entry.getInt("total"))
            }
            result
        } catch (_: Exception) { emptyMap() }
    }

    private fun addRecentPerformance(correct: Int, total: Int) {
        val json = prefs.getString("recent_performance", "[]") ?: "[]"
        val arr = JSONArray(json)
        arr.put(JSONObject().apply { put("correct", correct); put("total", total) })
        if (arr.length() > 10) {
            val newArr = JSONArray()
            for (i in arr.length() - 10 until arr.length()) newArr.put(arr.get(i))
            prefs.edit().putString("recent_performance", newArr.toString()).apply()
        } else {
            prefs.edit().putString("recent_performance", arr.toString()).apply()
        }
    }

    fun getRecentPerformance(): List<Pair<Int, Int>> {
        return try {
            val json = prefs.getString("recent_performance", "[]") ?: "[]"
            val arr = JSONArray(json)
            (0 until arr.length()).map {
                val obj = arr.getJSONObject(it)
                Pair(obj.getInt("correct"), obj.getInt("total"))
            }
        } catch (_: Exception) { emptyList() }
    }

    fun getDailyChallengeDate(): String = prefs.getString("daily_date", "") ?: ""

    fun setDailyChallengeComplete(score: Int, total: Int) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        prefs.edit()
            .putString("daily_date", today)
            .putInt("daily_score", score)
            .putInt("daily_total", total)
            .apply()
    }

    fun isDailyChallengeCompletedToday(): Boolean {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        return prefs.getString("daily_date", "") == today
    }

    fun getDailyScore(): Int = prefs.getInt("daily_score", 0)
    fun getDailyTotal(): Int = prefs.getInt("daily_total", 0)

    private fun addXP(amount: Int) {
        val current = prefs.getInt("xp", 0)
        prefs.edit().putInt("xp", current + amount).apply()
        checkLevelUp()
    }

    private fun checkLevelUp() {
        var xp = getXP()
        var level = getLevel()
        var required = level * 500
        while (xp >= required) {
            level++
            required = level * 500
        }
        if (level > getLevel()) {
            prefs.edit().putInt("level", level).apply()
        }
    }

    fun getXP(): Int = prefs.getInt("xp", 0)
    fun getLevel(): Int = prefs.getInt("level", 1)

    fun getXPForNextLevel(): Int = getLevel() * 500

    fun getXPProgress(): Int {
        val xp = getXP()
        val currentLevelXP = (getLevel() - 1) * 500
        val nextLevelXP = getLevel() * 500
        val progressXP = xp - currentLevelXP
        val needed = nextLevelXP - currentLevelXP
        return if (needed > 0) ((progressXP * 100) / needed).coerceIn(0, 100) else 100
    }

    fun saveQuizSession(
        questionsJson: String,
        statesJson: String,
        currentIndex: Int,
        streak: Int,
        bestStreak: Int,
        fiftyFiftyUsed: Boolean,
        skipUsed: Boolean,
        addTimeUsed: Boolean,
        category: String,
        difficulty: String,
        count: Int
    ) {
        prefs.edit()
            .putString("session_questions", questionsJson)
            .putString("session_states", statesJson)
            .putInt("session_index", currentIndex)
            .putInt("session_streak", streak)
            .putInt("session_best_streak", bestStreak)
            .putBoolean("session_fifty_fifty", fiftyFiftyUsed)
            .putBoolean("session_skip", skipUsed)
            .putBoolean("session_add_time", addTimeUsed)
            .putString("session_category", category)
            .putString("session_difficulty", difficulty)
            .putInt("session_count", count)
            .putBoolean("session_active", true)
            .apply()
    }

    fun hasActiveQuizSession(): Boolean = prefs.getBoolean("session_active", false)

    fun getQuizSession(): QuizSessionData? {
        if (!hasActiveQuizSession()) return null
        return QuizSessionData(
            questionsJson = prefs.getString("session_questions", "") ?: "",
            statesJson = prefs.getString("session_states", "") ?: "",
            currentIndex = prefs.getInt("session_index", 0),
            streak = prefs.getInt("session_streak", 0),
            bestStreak = prefs.getInt("session_best_streak", 0),
            fiftyFiftyUsed = prefs.getBoolean("session_fifty_fifty", false),
            skipUsed = prefs.getBoolean("session_skip", false),
            addTimeUsed = prefs.getBoolean("session_add_time", false),
            category = prefs.getString("session_category", "Any Category") ?: "Any Category",
            difficulty = prefs.getString("session_difficulty", "Any Difficulty") ?: "Any Difficulty",
            count = prefs.getInt("session_count", 10)
        )
    }

    fun clearQuizSession() {
        prefs.edit().putBoolean("session_active", false).apply()
    }

    data class QuizSessionData(
        val questionsJson: String,
        val statesJson: String,
        val currentIndex: Int,
        val streak: Int,
        val bestStreak: Int,
        val fiftyFiftyUsed: Boolean,
        val skipUsed: Boolean,
        val addTimeUsed: Boolean,
        val category: String,
        val difficulty: String,
        val count: Int
    )

    fun isSoundEnabled(): Boolean = prefs.getBoolean("setting_sound", true)
    fun setSoundEnabled(enabled: Boolean) { prefs.edit().putBoolean("setting_sound", enabled).apply() }

    fun isVibrationEnabled(): Boolean = prefs.getBoolean("setting_vibration", true)
    fun setVibrationEnabled(enabled: Boolean) { prefs.edit().putBoolean("setting_vibration", enabled).apply() }

    fun isTimerEnabled(): Boolean = prefs.getBoolean("setting_timer", true)
    fun setTimerEnabled(enabled: Boolean) { prefs.edit().putBoolean("setting_timer", enabled).apply() }

    fun getTimerDuration(): Int = prefs.getInt("setting_timer_duration", 20)
    fun setTimerDuration(seconds: Int) { prefs.edit().putInt("setting_timer_duration", seconds).apply() }

    fun resetAll() {
        prefs.edit().clear().apply()
    }
}
