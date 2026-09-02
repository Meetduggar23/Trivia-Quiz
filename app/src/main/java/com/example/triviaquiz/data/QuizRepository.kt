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
     * Fetches trivia questions from the Open Trivia DB API.
     * Runs network operations on a background thread using Dispatchers.IO.
     *
     * Demonstrates:
     * - suspend function
     * - withContext(Dispatchers.IO) for background network work
     * - Coordinating NetworkClient and JsonParser
     * - Converting errors into NetworkResult.Error
     *
     * @return NetworkResult containing a list of Questions or an error message
     */
    suspend fun fetchQuestions(): NetworkResult<List<Question>> {
        // Perform network work on the IO thread — not the main thread
        return withContext(Dispatchers.IO) {
            // Step 1: Make the HTTP request using the raw network client
            when (val networkResult = NetworkClient.get()) {
                is NetworkResult.Success -> {
                    try {
                        // Step 2: Parse the JSON response into Question objects
                        val questions = JsonParser.parseQuestions(networkResult.data)

                        // Step 3: Validate that we received questions
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
