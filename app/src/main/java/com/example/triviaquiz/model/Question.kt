package com.example.triviaquiz.model

/**
 * Data model representing a single trivia question.
 * Contains the question text, the correct answer, and a list of exactly four shuffled answers.
 */
data class Question(
    val text: String,
    val correctAnswer: String,
    val answers: List<String>
)
