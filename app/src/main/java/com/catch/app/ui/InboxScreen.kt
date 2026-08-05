package com.catch.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.catch.app.R
import com.catch.app.data.local.CaptureEntity
import com.catch.app.data.local.CaptureState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun InboxScreen(
    onCaptureClick: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val captures by viewModel.captures.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Catch") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = onCaptureClick) {
                Icon(painterResource(R.drawable.ic_tile_mic), contentDescription = "Capture")
            }
        }
    ) { padding ->
        if (captures.isEmpty()) {
            EmptyState(padding)
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                items(captures, key = { it.id }) { capture ->
                    CaptureRow(capture)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(padding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Nothing captured yet.\nTap the mic — via the tile, or the button below.",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun CaptureRow(capture: CaptureEntity) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = capture.title ?: capture.rawTranscript,
                style = MaterialTheme.typography.bodyLarge
            )

            if (capture.title != null && capture.state != CaptureState.FAILED) {
                Text(
                    text = capture.rawTranscript,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                StatePill(capture.state)
                capture.confidence?.let {
                    Text(
                        text = "${(it * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatTimestamp(capture.capturedAt),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (capture.state == CaptureState.FAILED && capture.errorMessage != null) {
                Text(
                    text = capture.errorMessage,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun StatePill(state: CaptureState) {
    val (label, color) = when (state) {
        CaptureState.CAPTURED -> "Captured" to MaterialTheme.colorScheme.onSurfaceVariant
        CaptureState.STRUCTURING -> "Structuring…" to MaterialTheme.colorScheme.secondary
        CaptureState.AWAITING_CONFIRM -> "Ready to confirm" to MaterialTheme.colorScheme.primary
        CaptureState.FILING -> "Filing…" to MaterialTheme.colorScheme.secondary
        CaptureState.FILED -> "Filed" to MaterialTheme.colorScheme.primary
        CaptureState.FAILED -> "Failed" to MaterialTheme.colorScheme.error
        CaptureState.DISCARDED -> "Discarded" to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Surface(color = color.copy(alpha = 0.12f), shape = MaterialTheme.shapes.small) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

private val timestampFormatter = DateTimeFormatter.ofPattern("MMM d, HH:mm")

private fun formatTimestamp(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(timestampFormatter)
