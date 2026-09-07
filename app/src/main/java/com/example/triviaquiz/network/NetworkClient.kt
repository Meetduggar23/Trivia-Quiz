package com.example.triviaquiz.network

import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * Network client that performs raw HTTP requests using ONLY HttpURLConnection.
 * No third-party networking libraries (Retrofit, OkHttp, etc.) are used.
 * This demonstrates fundamental Android networking concepts.
 */
object NetworkClient {

    // Base URL for Open Trivia DB API
    private const val BASE_URL = "https://opentdb.com"

    // Timeout values in milliseconds
    private const val CONNECT_TIMEOUT = 15000
    private const val READ_TIMEOUT = 15000

    /**
     * Performs a GET request to the Open Trivia DB API and returns the response as a string.
     * Demonstrates: HttpURLConnection, InputStream, InputStreamReader, BufferedReader, StringBuilder.
     *
     * @param queryPath The API query path (e.g., "/api.php?amount=10&type=multiple")
     * @return NetworkResult containing the JSON response string or an error message
     */
    fun get(queryPath: String): NetworkResult<String> {
        var connection: HttpURLConnection? = null
        var reader: BufferedReader? = null
        var inputStream: InputStream? = null

        try {
            val url = URL(BASE_URL + queryPath)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT
            connection.readTimeout = READ_TIMEOUT
            connection.connect()

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return NetworkResult.Error("HTTP error: $responseCode")
            }

            inputStream = connection.inputStream
            reader = BufferedReader(InputStreamReader(inputStream))
            val response = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }

            val responseString = response.toString()
            if (responseString.isEmpty()) {
                return NetworkResult.Error("Empty response from server")
            }
            return NetworkResult.Success(responseString)
        } catch (e: Exception) {
            return NetworkResult.Error("Network error: ${e.message ?: "Unknown error"}")
        } finally {
            try { reader?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }
}
