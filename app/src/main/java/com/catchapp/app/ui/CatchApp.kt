package com.catchapp.app.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.catchapp.app.capture.CaptureActivity
import com.catchapp.app.ui.onboarding.OnboardingScreen
import com.catchapp.app.ui.onboarding.OnboardingViewModel
import com.catchapp.app.ui.onboarding.TutorialScreen

private object Routes {
    const val TUTORIAL = "tutorial"
    const val ONBOARDING = "onboarding"
    const val INBOX = "inbox"
    const val CAPTURE_DETAIL = "capture/{captureId}"

    fun captureDetail(id: Long) = "capture/$id"
}

/**
 * The single-Activity Compose nav graph. First run: Tutorial -> Onboarding ->
 * Inbox. Every later launch skips straight to Inbox, which can jump back
 * into either screen via its settings/help icons, or into a capture's detail
 * screen to confirm/discard/delete/retry it.
 */
@Composable
fun CatchApp() {
    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val context = LocalContext.current

    val startDestination = remember {
        if (onboardingViewModel.hasKey()) Routes.INBOX else Routes.TUTORIAL
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.TUTORIAL) {
            TutorialScreen(
                onDone = {
                    // Re-checked at click time, not just at cold start, so this
                    // also does the right thing when revisited from the inbox's
                    // help icon after a key is already set.
                    val next = if (onboardingViewModel.hasKey()) Routes.INBOX else Routes.ONBOARDING
                    navController.navigate(next) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onSaved = {
                    navController.navigate(Routes.INBOX) { popUpTo(0) { inclusive = true } }
                },
                onSkip = {
                    navController.navigate(Routes.INBOX) { popUpTo(0) { inclusive = true } }
                }
            )
        }

        composable(Routes.INBOX) {
            InboxScreen(
                onCaptureClick = { context.startActivity(Intent(context, CaptureActivity::class.java)) },
                onOpenSettings = { navController.navigate(Routes.ONBOARDING) },
                onOpenTutorial = { navController.navigate(Routes.TUTORIAL) },
                onOpenCapture = { id -> navController.navigate(Routes.captureDetail(id)) }
            )
        }

        composable(
            route = Routes.CAPTURE_DETAIL,
            arguments = listOf(navArgument("captureId") { type = NavType.LongType })
        ) {
            CaptureDetailScreen(onBack = { navController.popBackStack() })
        }
    }
}
