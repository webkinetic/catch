package com.catchapp.app.ui.onboarding

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Bring-your-own-key entry point. Doubles as both the first-run screen and
 * the "change my key" screen reached from the inbox's settings icon — same
 * UI either way, only the caller's navigation after Save/Skip differs.
 */
@Composable
fun OnboardingScreen(
    onSaved: () -> Unit,
    onSkip: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    var key by remember { mutableStateOf(viewModel.currentKey()) }
    val context = LocalContext.current

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Connect Gemini", style = MaterialTheme.typography.headlineSmall)
            Text(
                "Catch runs on your own free Gemini API key — your usage, your " +
                    "account, nothing routed through anyone else's server.",
                style = MaterialTheme.typography.bodyLarge
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("1. Open Google AI Studio and sign in", style = MaterialTheme.typography.bodyMedium)
                Text("2. Tap \"Create API key\"", style = MaterialTheme.typography.bodyMedium)
                Text("3. Copy it, then paste it below", style = MaterialTheme.typography.bodyMedium)
            }

            OutlinedButton(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://aistudio.google.com/apikey"))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Google AI Studio")
            }

            OutlinedTextField(
                value = key,
                onValueChange = { key = it },
                label = { Text("Gemini API key") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                "Stored encrypted on this device only (Android Keystore) — " +
                    "never sent anywhere but Google's API, never leaves this screen otherwise.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    viewModel.saveKey(key)
                    onSaved()
                },
                enabled = key.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save and continue")
            }

            TextButton(onClick = onSkip, modifier = Modifier.fillMaxWidth()) {
                Text("I'll do this later")
            }
        }
    }
}
