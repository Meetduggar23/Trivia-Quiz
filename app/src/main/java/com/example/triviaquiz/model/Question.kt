package com.example.triviaquiz.model

data class Question(
    val text: String,
    val correctAnswer: String,
    val answers: List<String>,
    val category: String = "",
    val difficulty: String = ""
)
