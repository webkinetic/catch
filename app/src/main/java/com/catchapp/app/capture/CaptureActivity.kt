package com.catchapp.app.capture

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.catchapp.app.BuildConfig
import com.catchapp.app.data.CaptureRepository
import com.catchapp.app.ui.theme.CatchTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Transparent trampoline: listens, writes CAPTURED to Room the instant
 * recognition ends, and finishes. Nothing in this file may await a network
 * call — that's what StructureCaptureWorker is for (hard rule #1 and #4).
 */
@AndroidEntryPoint
class CaptureActivity : ComponentActivity() {

    @Inject
    lateinit var captureRepository: CaptureRepository

    private var speechRecognizer: SpeechRecognizer? = null
    private var uiState by mutableStateOf<CaptureUiState>(CaptureUiState.RequestingPermission)
    private var usingOnDeviceRecognizer = false
    private var hasFallenBackToStandardRecognizer = false

    private val requestAudioPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startListening()
            } else {
                uiState = CaptureUiState.Error("Microphone permission is required.")
                finishAfterDelay()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CatchTheme {
                CaptureScreen(uiState)
            }
        }

        if (hasAudioPermission()) {
            startListening()
        } else {
            requestAudioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun hasAudioPermission() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Tries the on-device recognizer first (offline, per hard rule #5), but
     * its endpointing is a different engine that often just ignores the
     * SPEECH_INPUT_* silence extras below — on some devices that means it
     * decides "done" almost instantly and reports ERROR_NO_MATCH before the
     * user has really started talking. If that happens, [onError] below
     * falls back to the standard recognizer once, which reliably honours
     * these extras.
     */
    private fun startListening(useOnDevice: Boolean = true) {
        uiState = CaptureUiState.Listening(partialText = "")
        usingOnDeviceRecognizer = useOnDevice

        val recognizer = if (useOnDevice &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        } else {
            usingOnDeviceRecognizer = false
            SpeechRecognizer.createSpeechRecognizer(this)
        }
        speechRecognizer?.destroy()
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(listener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Default silence timeout (~2s) cuts off rambling capture. Bumped
            // per the brief so thinking pauses don't truncate the thought.
            // All three matter — engines that honour any of these tend to
            // require all three set consistently, not just the first two.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500)
        }
        recognizer.startListening(intent)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onPartialResults(partialResults: Bundle) {
            val text = partialResults
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            uiState = CaptureUiState.Listening(partialText = text)
        }

        override fun onResults(results: Bundle) {
            // Measurement point for hard rule #4: everything from here to
            // finish() is local DB + UI, never network.
            val speechEndedAtMs = SystemClock.elapsedRealtime()

            val transcript = results
                .getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
                .orEmpty()

            if (transcript.isEmpty()) {
                uiState = CaptureUiState.Error("Didn't catch that.")
                finishAfterDelay()
                return
            }

            uiState = CaptureUiState.Saving

            lifecycleScope.launch {
                captureRepository.captureTranscript(transcript)

                if (BuildConfig.DEBUG) {
                    val elapsedMs = SystemClock.elapsedRealtime() - speechEndedAtMs
                    // Elapsed time only — never the transcript itself (hard rule #2).
                    Log.d(PERF_TAG, "speech-end-to-dismiss: ${elapsedMs}ms")
                }

                finish()
            }
        }

        override fun onError(error: Int) {
            val isEndpointingError = error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

            if (usingOnDeviceRecognizer && isEndpointingError && !hasFallenBackToStandardRecognizer) {
                // The on-device engine gave up almost immediately — likely
                // ignoring the silence extras entirely on this device. Retry
                // once with the standard recognizer before showing an error.
                hasFallenBackToStandardRecognizer = true
                startListening(useOnDevice = false)
                return
            }

            uiState = CaptureUiState.Error(describeError(error))
            finishAfterDelay()
        }
    }

    private fun finishAfterDelay() {
        lifecycleScope.launch {
            delay(1100)
            finish()
        }
    }

    private fun describeError(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_NO_MATCH -> "Didn't catch that."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
        SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
            "Speech recognition needs a connection on this device."
        else -> "Couldn't capture that — try again."
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
        super.onDestroy()
    }

    private companion object {
        const val PERF_TAG = "Catch.Perf"
    }
}
