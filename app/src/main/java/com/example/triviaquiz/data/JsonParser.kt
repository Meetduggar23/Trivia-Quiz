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
     *
     * Demonstrates:
     * - Creating a JSONObject from a string
     * - Reading integer values from a JSON object
     * - Reading a JSONArray from a JSON object
     * - Iterating through a JSONArray
     * - Reading nested JSONArrays (incorrect_answers)
     * - Reading string values from JSON objects
     *
     * @param json The raw JSON response string from the API
     * @return List of Question objects
     * @throws Exception if JSON is malformed or response code indicates an error
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
            // Step 5: Get each question as a JSONObject
            val questionObject: JSONObject = resultsArray.getJSONObject(i)

            // Step 6: Read the question text (may contain HTML entities)
            val questionText = decodeHtml(questionObject.getString("question"))

            // Step 7: Read the correct answer
            val correctAnswer = decodeHtml(questionObject.getString("correct_answer"))

            // Step 8: Read the nested "incorrect_answers" JSON array
            val incorrectAnswersArray: JSONArray = questionObject.getJSONArray("incorrect_answers")

            // Step 9: Loop through the incorrect answers and collect them
            val answers = mutableListOf<String>()
            for (j in 0 until incorrectAnswersArray.length()) {
                answers.add(decodeHtml(incorrectAnswersArray.getString(j)))
            }

            // Step 10: Add the correct answer to the list
            answers.add(correctAnswer)

            // Step 11: Shuffle all four answers so the correct answer isn't always last
            answers.shuffle()

            // Step 12: Create a Question object and add to the list
            questions.add(
                Question(
                    text = questionText,
                    correctAnswer = correctAnswer,
                    answers = answers
                )
            )
        }

        return questions
    }

    /**
     * Decodes HTML entities (like &quot; and &#039;) into readable characters.
     * Open Trivia DB returns text with HTML encoding.
     *
     * @param html The HTML-encoded string
     * @return The decoded plain text string
     */
    private fun decodeHtml(html: String): String {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString()
        } else {
            @Suppress("DEPRECATION")
            Html.fromHtml(html).toString()
        }
    }
}
