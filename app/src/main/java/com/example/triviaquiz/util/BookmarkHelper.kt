package com.example.triviaquiz.util

import com.example.triviaquiz.data.QuizPreferences
import com.example.triviaquiz.model.Question
import org.json.JSONArray
import org.json.JSONObject

/**
 * Helper for managing bookmark operations with QuizPreferences.
 */
class BookmarkHelper(private val prefs: QuizPreferences) {

    fun toggleBookmark(question: Question): Boolean {
        val isCurrentlyBookmarked = prefs.isBookmarked(question.text)
        if (isCurrentlyBookmarked) {
            prefs.removeBookmark(question.text)
            return false
        } else {
            prefs.saveBookmark(
                questionText = question.text,
                correctAnswer = question.correctAnswer,
                category = question.category,
                difficulty = question.difficulty,
                answers = question.answers
            )
            return true
        }
    }

    fun isBookmarked(questionText: String): Boolean = prefs.isBookmarked(questionText)

    fun getBookmarkedQuestions(): List<BookmarkItem> {
        return prefs.getBookmarks().mapNotNull { json ->
            try {
                val obj = JSONObject(json)
                BookmarkItem(
                    questionText = obj.getString("questionText"),
                    correctAnswer = obj.getString("correctAnswer"),
                    category = obj.optString("category", ""),
                    difficulty = obj.optString("difficulty", ""),
                    answers = (0 until obj.getJSONArray("answers").length()).map {
                        obj.getJSONArray("answers").getString(it)
                    }
                )
            } catch (_: Exception) { null }
        }
    }

    fun clearBookmarks() = prefs.clearBookmarks()

    data class BookmarkItem(
        val questionText: String,
        val correctAnswer: String,
        val category: String,
        val difficulty: String,
        val answers: List<String>
    )
}
