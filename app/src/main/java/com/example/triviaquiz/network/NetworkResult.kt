package com.example.triviaquiz.network

/**
 * Sealed class representing the result of a network operation.
 * Demonstrates type-safe error handling without exceptions.
 */
sealed class NetworkResult<out T> {
    /** Represents a successful network operation with data. */
    data class Success<out T>(val data: T) : NetworkResult<T>()

    /** Represents a failed network operation with an error message. */
    data class Error(val message: String) : NetworkResult<Nothing>()
}
