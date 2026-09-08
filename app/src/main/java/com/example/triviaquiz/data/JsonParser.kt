package com.example.triviaquiz.data

import android.text.Html
import com.example.triviaquiz.model.Question
import org.json.JSONArray
import org.json.JSONObject

object JsonParser {

    fun parseQuestions(json: String): List<Question> {
        val jsonObject = JSONObject(json)

        val responseCode = jsonObject.getInt("response_code")
        if (responseCode != 0) {
            throw Exception("API error: response_code = $responseCode")
        }

        val resultsArray: JSONArray = jsonObject.getJSONArray("results")

        val questions = mutableListOf<Question>()

        for (i in 0 until resultsArray.length()) {
            val questionObject: JSONObject = resultsArray.getJSONObject(i)

            val questionText = decodeHtml(questionObject.getString("question"))
            val correctAnswer = decodeHtml(questionObject.getString("correct_answer"))
            val category = decodeHtml(questionObject.optString("category", ""))
            val difficulty = decodeHtml(questionObject.optString("difficulty", ""))

            val incorrectAnswersArray: JSONArray = questionObject.getJSONArray("incorrect_answers")

            val answers = mutableListOf<String>()
            for (j in 0 until incorrectAnswersArray.length()) {
                answers.add(decodeHtml(incorrectAnswersArray.getString(j)))
            }

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
