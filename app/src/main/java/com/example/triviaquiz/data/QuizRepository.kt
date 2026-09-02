package com.example.triviaquiz.data

import com.example.triviaquiz.model.Question
import com.example.triviaquiz.network.NetworkClient
import com.example.triviaquiz.network.NetworkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository that serves as the single source for quiz data.
 * Demonstrates: suspend functions, withContext(Dispatchers.IO), and coroutine-based networking.
 *
 * Flow: Activity → Repository → NetworkClient → HttpURLConnection → JSON → JsonParser → Question objects
 */
class QuizRepository {

    /**
     * Builds the Open Trivia DB API query path based on user selections.
     *
     * @param amount Number of questions (5, 10, 15, or 20)
     * @param category Category ID (null for "Any Category")
     * @param difficulty Difficulty level (null for "Any Difficulty", or "easy"/"medium"/"hard")
     * @return The complete query path string
     */
    fun buildQueryPath(amount: Int, category: Int?, difficulty: String?): String {
        val params = mutableListOf("amount=$amount", "type=multiple")
        category?.let { params.add("category=$it") }
        difficulty?.let { params.add("difficulty=$it") }
        return "/api.php?${params.joinToString("&")}"
    }

    /**
     * Fetches trivia questions from the Open Trivia DB API.
     * Runs network operations on a background thread using Dispatchers.IO.
     *
     * Demonstrates:
     * - suspend function
     * - withContext(Dispatchers.IO) for background network work
     * - Coordinating NetworkClient and JsonParser
     * - Converting errors into NetworkResult.Error
     *
     * @param amount Number of questions to fetch
     * @param category Category ID (null for any)
     * @param difficulty Difficulty level (null for any)
     * @return NetworkResult containing a list of Questions or an error message
     */
    suspend fun fetchQuestions(
        amount: Int = 10,
        category: Int? = null,
        difficulty: String? = null
    ): NetworkResult<List<Question>> {
        // Perform network work on the IO thread — not the main thread
        return withContext(Dispatchers.IO) {
            // Step 1: Build the API query path dynamically
            val queryPath = buildQueryPath(amount, category, difficulty)

            // Step 2: Make the HTTP request using the raw network client
            when (val networkResult = NetworkClient.get(queryPath)) {
                is NetworkResult.Success -> {
                    try {
                        // Step 3: Parse the JSON response into Question objects
                        val questions = JsonParser.parseQuestions(networkResult.data)

                        // Step 4: Validate that we received questions
                        if (questions.isEmpty()) {
                            NetworkResult.Error("No questions received from the API")
                        } else {
                            NetworkResult.Success(questions)
                        }
                    } catch (e: Exception) {
                        // Handle JSON parsing errors
                        NetworkResult.Error("Failed to parse questions: ${e.message}")
                    }
                }
                is NetworkResult.Error -> {
                    // Pass through network errors
                    NetworkResult.Error(networkResult.message)
                }
            }
        }
    }
}
