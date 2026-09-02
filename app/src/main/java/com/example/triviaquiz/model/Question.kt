package com.example.triviaquiz.model

/**
 * Data model representing a single trivia question.
 * Contains the question text, correct answer, list of four shuffled answers,
 * and metadata (category, difficulty) for bookmarks and resume.
 */
data class Question(
    val text: String,
    val correctAnswer: String,
    val answers: List<String>,
    val category: String = "",
    val difficulty: String = ""
)
