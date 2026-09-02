package com.example.triviaquiz.data

import android.text.Html
import com.example.triviaquiz.model.Question
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses JSON responses from Open Trivia DB using ONLY org.json library.
 * Demonstrates: JSONObject, JSONArray, nested JSONArray parsing, and HTML entity decoding.
 */
object JsonParser {

    /**
     * Parses the complete API response JSON string into a list of Question objects.
     */
    fun parseQuestions(json: String): List<Question> {
        // Step 1: Create a JSONObject from the complete response string
        val jsonObject = JSONObject(json)

        // Step 2: Read the response_code field — must be 0 for success
        val responseCode = jsonObject.getInt("response_code")
        if (responseCode != 0) {
            throw Exception("API error: response_code = $responseCode")
        }

        // Step 3: Read the "results" JSON array containing all questions
        val resultsArray: JSONArray = jsonObject.getJSONArray("results")

        // Step 4: Loop through each item in the results array
        val questions = mutableListOf<Question>()

        for (i in 0 until resultsArray.length()) {
            val questionObject: JSONObject = resultsArray.getJSONObject(i)

            val questionText = decodeHtml(questionObject.getString("question"))
            val correctAnswer = decodeHtml(questionObject.getString("correct_answer"))
            val category = decodeHtml(questionObject.optString("category", ""))
            val difficulty = decodeHtml(questionObject.optString("difficulty", ""))

            // Read the nested "incorrect_answers" JSON array
            val incorrectAnswersArray: JSONArray = questionObject.getJSONArray("incorrect_answers")

            val answers = mutableListOf<String>()
            for (j in 0 until incorrectAnswersArray.length()) {
                answers.add(decodeHtml(incorrectAnswersArray.getString(j)))
            }

            // Add correct answer and shuffle
            answers.add(correctAnswer)
            answers.shuffle()

            questions.add(
                Question(
                    text = questionText,
                    correctAnswer = correctAnswer,
                    answers = answers,
                    category = category,
                    difficulty = difficulty
                )
            )
        }

        return questions
    }

    private fun decodeHtml(html: String): String {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html).toString()
        }
    }
}
