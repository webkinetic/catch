package com.catchapp.app.capture

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Deliberately minimal — this screen exists for ~1-10 seconds. Partial
 * results stream in live so the user knows it's actually listening (brief:
 * "reassures the user it's listening, and it's free").
 */
@Composable
fun CaptureScreen(state: CaptureUiState) {
    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            Crossfade(targetState = state, label = "capture-state") { s ->
                when (s) {
                    is CaptureUiState.RequestingPermission -> StatusText("Waiting for microphone permission…")

                    is CaptureUiState.Listening -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(20.dp),
                        modifier = Modifier.padding(horizontal = 32.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(
                            text = "Listening…",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        if (s.partialText.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(
                                    text = s.partialText,
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    is CaptureUiState.Saving -> StatusText("Got it.")

                    is CaptureUiState.Error -> StatusText(s.message)
                }
            }
        }
    }
}

@Composable
private fun StatusText(text: String) {
    Text(text = text, style = MaterialTheme.typography.bodyLarge)
}
