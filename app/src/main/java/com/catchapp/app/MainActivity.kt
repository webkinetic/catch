package com.catchapp.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.catchapp.app.capture.CaptureActivity
import com.catchapp.app.ui.InboxScreen
import com.catchapp.app.ui.theme.CatchTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CatchTheme {
                InboxScreen(
                    onCaptureClick = { startActivity(Intent(this, CaptureActivity::class.java)) }
                )
            }
        }
    }
}
