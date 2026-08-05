package com.catchapp.app.capture

sealed interface CaptureUiState {
    data object RequestingPermission : CaptureUiState
    /** Mic requested but not yet actually capturing — see onReadyForSpeech. */
    data object Preparing : CaptureUiState
    data class Listening(val partialText: String) : CaptureUiState
    data object Saving : CaptureUiState
    data class Error(val message: String) : CaptureUiState
}
