package com.example.triviaquiz.model

/**
 * Represents the session state for a single question during the quiz.
 * This is separate from the API Question model to support Previous/Next/Navigator navigation
 * and prevent double-counting of scores.
 *
 * Each question tracks:
 * - Whether it has been answered
 * - What answer was selected
 * - Whether the answer was correct or wrong
 * - Whether it was skipped
 * - Whether it was flagged for review
 * - Whether it is bookmarked (permanent, survives quiz end)
 * - Which answers were eliminated by 50/50
 * - Whether 50/50 was used on this question
 */
data class QuizState(
    val questionIndex: Int,
    var selectedAnswer: String? = null,
    var isAnswered: Boolean = false,
    var isCorrect: Boolean = false,
    var isWrong: Boolean = false,
    var isSkipped: Boolean = false,
    var isFlagged: Boolean = false,
    var isBookmarked: Boolean = false,
    var eliminatedAnswers: List<String> = emptyList(),
    var isFiftyFiftyUsed: Boolean = false
)
