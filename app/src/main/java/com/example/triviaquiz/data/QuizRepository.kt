package com.example.triviaquiz.data

import com.example.triviaquiz.model.Question
import com.example.triviaquiz.network.NetworkClient
import com.example.triviaquiz.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuizRepository {

    fun buildQueryPath(amount: Int, category: Int?, difficulty: String?): String {
        val params = mutableListOf("amount=$amount", "type=multiple")
        category?.let { params.add("category=$it") }
        difficulty?.let { params.add("difficulty=$it") }
        return "/api.php?${params.joinToString("&")}"
    }

    suspend fun fetchQuestions(
        amount: Int = 10,
        category: Int? = null,
        difficulty: String? = null
    ): NetworkResult<List<Question>> {
        return withContext(Dispatchers.IO) {
            val queryPath = buildQueryPath(amount, category, difficulty)

            when (val networkResult = NetworkClient.get(queryPath)) {
                is NetworkResult.Success -> {
                    try {
                        val questions = JsonParser.parseQuestions(networkResult.data)

                        if (questions.isEmpty()) {
                            NetworkResult.Error("No questions received from the API")
                        } else {
                            NetworkResult.Success(questions)
                        }
                    } catch (e: Exception) {
                        NetworkResult.Error("Failed to parse questions: ${e.message}")
                    }
                }
                is NetworkResult.Error -> {
                    NetworkResult.Error(networkResult.message)
                }
            }
        }
    }
}
