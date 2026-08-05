package com.catch.app.domain

/**
 * Deliberately don't propagate raw transport-layer exception messages up
 * through these — a body-mismatch or timeout error could echo request
 * content into a message string, and hard rule #2 is no transcript in logs
 * or crash reports, ever.
 */
sealed class StructuringException(message: String) : Exception(message) {
    data object MissingApiKey :
        StructuringException("No Gemini API key configured.")

    data object NoFunctionCallReturned :
        StructuringException("Gemini did not return a file_capture call.")

    data class NetworkError(val cause: Throwable) :
        StructuringException("Structuring request failed.")
}
