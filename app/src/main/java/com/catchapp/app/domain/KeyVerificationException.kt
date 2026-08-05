package com.catchapp.app.domain

sealed class KeyVerificationException(message: String) : Exception(message) {
    data object InvalidKey :
        KeyVerificationException("That key was rejected — double check it's correct.")

    data object ModelUnavailable :
        KeyVerificationException("That key doesn't have access to this model yet.")

    data class Other(val code: Int) :
        KeyVerificationException("Gemini returned an unexpected error (HTTP $code).")

    data class NetworkError(override val cause: Throwable) :
        KeyVerificationException("Couldn't reach Gemini — check your connection.")
}
