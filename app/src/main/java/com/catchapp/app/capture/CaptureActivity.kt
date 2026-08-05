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
     * Tries the on-device recognizer first (offline, per hard rule #5).
     * Whichever engine ends up running, a well-documented Android quirk can
     * still bite: partial results stream real text throughout, but the
     * *final* pass rejects it and reports ERROR_NO_MATCH anyway — the two
     * passes aren't always the same model. [onEndOfSpeech] nudging the
     * engine to finalize, and [onError] retrying once, both exist to work
     * around that rather than trust either pass blindly.
     */
    private fun startListening(useOnDevice: Boolean = true) {
        uiState = CaptureUiState.Listening(partialText = "")

        val recognizer = if (useOnDevice &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(this)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(this)
        } else {
            SpeechRecognizer.createSpeechRecognizer(this)
        }
        speechRecognizer?.destroy()
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(listener)

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            // Hints the engine to resolve on-device rather than doing a
            // cloud round-trip for the final pass — a likely source of the
            // partial-good/final-rejected mismatch on the standard recognizer.
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            // Default silence timeout (~2s) cuts off rambling capture. Bumped
            // per the brief so thinking pauses don't truncate the thought.
            // Possibly-complete is intentionally shorter than complete — it's
            // a soft "maybe done" hint, not another hard cutoff.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500)
        }
        recognizer.startListening(intent)
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) = Unit
        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() {
            // Some engines will happily keep listening past this point and
            // only reject the final transcript later; explicitly telling it
            // to stop here prompts it to finalize against what it already
            // has, instead of whatever produces the partial/final mismatch.
            speechRecognizer?.stopListening()
        }
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

            // Retry once regardless of which engine just failed — on-device
            // falls back to standard; standard just gets a fresh attempt.
            // (A first version of this only retried when on-device had run,
            // which meant devices that go straight to the standard engine
            // never got a retry at all.)
            if (isEndpointingError && !hasFallenBackToStandardRecognizer) {
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
