package com.catchapp.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.catchapp.app.data.local.CaptureState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptureDetailScreen(
    onBack: () -> Unit,
    viewModel: CaptureDetailViewModel = hiltViewModel()
) {
    val capture by viewModel.capture.collectAsState()
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Capture") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Text("←", style = MaterialTheme.typography.titleLarge)
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Text("🗑", style = MaterialTheme.typography.titleLarge)
                    }
                }
            )
        }
    ) { padding ->
        val current = capture

        if (current == null) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text("Not found — probably already deleted.")
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            StatePill(current.state)

            if (!current.title.isNullOrBlank()) {
                Text(current.title, style = MaterialTheme.typography.headlineSmall)
            }

            if (!current.body.isNullOrBlank()) {
                Text(current.body, style = MaterialTheme.typography.bodyLarge)
            }

            DetailRow("Raw transcript", current.rawTranscript)
            current.dueIso?.let { DetailRow("Due", it) }
            current.project?.let { DetailRow("Project", it) }
            if (current.tags.isNotEmpty()) DetailRow("Tags", current.tags.joinToString(", "))
            if (current.people.isNotEmpty()) DetailRow("People", current.people.joinToString(", "))
            current.confidence?.let { DetailRow("Confidence", "${(it * 100).toInt()}%") }

            if (current.state == CaptureState.FAILED && !current.errorMessage.isNullOrBlank()) {
                Text(
                    current.errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            when (current.state) {
                CaptureState.AWAITING_CONFIRM -> {
                    Button(
                        onClick = { viewModel.confirm(); onBack() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Confirm") }
                    OutlinedButton(
                        onClick = { viewModel.discard(); onBack() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Discard") }
                }

                CaptureState.FAILED -> {
                    Button(onClick = viewModel::retry, modifier = Modifier.fillMaxWidth()) {
                        Text("Retry")
                    }
                }

                CaptureState.CAPTURED, CaptureState.STRUCTURING -> {
                    Text(
                        "Still working on this one…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                CaptureState.FILED, CaptureState.DISCARDED, CaptureState.FILING -> Unit
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete this capture?") },
            text = { Text("This can't be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete()
                    onBack()
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
