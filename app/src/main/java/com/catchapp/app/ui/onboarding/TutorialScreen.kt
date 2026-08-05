package com.catchapp.app.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class TutorialPage(val title: String, val body: String)

private val pages = listOf(
    TutorialPage(
        title = "Talk, then forget about it",
        body = "Tap the mic, say what's on your mind for a few seconds, and stop. " +
            "Catch writes it down instantly — nothing to wait for, nothing to lose."
    ),
    TutorialPage(
        title = "Add the Quick Settings tile",
        body = "The fastest way in: swipe down twice from the top of your screen, " +
            "tap the pencil (edit) icon, then drag \"Catch\" into your tiles. " +
            "After that, one tap from the lock screen or the shade starts a " +
            "capture — no unlocking, no opening the app."
    ),
    TutorialPage(
        title = "Or just use the button",
        body = "Haven't set up the tile yet? The mic button on the inbox screen " +
            "does exactly the same thing."
    ),
    TutorialPage(
        title = "What happens after you talk",
        body = "Your words are saved instantly, then Gemini reads them in the " +
            "background and works out what kind of thing it is, a title, and a " +
            "due date if you mentioned one. Watch the label on each entry move " +
            "from Captured → Structuring → Ready to confirm."
    ),
    TutorialPage(
        title = "One thing not built yet",
        body = "Structured captures currently sit in the inbox for you to " +
            "review — actually confirming and filing them somewhere isn't " +
            "wired up yet. For now this is the raw capture-and-understand " +
            "loop, working end to end."
    )
)

@Composable
fun TutorialScreen(onDone: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDone) { Text("Skip") }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    repeat(pages.size) { index ->
                        val active = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .size(if (active) 8.dp else 6.dp)
                                .background(
                                    color = if (active) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outlineVariant
                                    },
                                    shape = CircleShape
                                )
                        )
                    }
                }

                val isLastPage = pagerState.currentPage == pages.lastIndex
                Button(onClick = {
                    if (isLastPage) {
                        onDone()
                    } else {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    }
                }) {
                    Text(if (isLastPage) "Get started" else "Next")
                }
            }
        }
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) { page ->
            val item = pages[page]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Text(item.title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(16.dp))
                Text(item.body, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
