package com.example.triviaquiz.model

data class QuizState(
    val questionIndex: Int,
    var selectedAnswer: String? = null,
    var isAnswered: Boolean = false,
    var isCorrect: Boolean = false,
    var isWrong: Boolean = false,
    var isSkipped: Boolean = false,
    var isFlagged: Boolean = false,
    var eliminatedAnswers: List<String> = emptyList(),
    var isFiftyFiftyUsed: Boolean = false
)
