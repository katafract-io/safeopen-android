package com.katafract.safeopen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.katafract.safeopen.ui.HistoryScreen
import com.katafract.safeopen.ui.ResultScreen
import com.katafract.safeopen.ui.ScannerScreen
import com.katafract.safeopen.ui.theme.SafeOpenTheme
import com.katafract.safeopen.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // ── Splash screen (Android 12+ API, back-compat to API 23) ─────
        // Held until first Compose frame attaches so the visual handoff
        // from splash → app is seamless rather than a white flash.
        val splash: SplashScreen = installSplashScreen()
        var keepSplash = true
        splash.setKeepOnScreenCondition { keepSplash }

        // ── Edge-to-edge ──────────────────────────────────────────────
        // Must run before super.onCreate() / setContent so window insets
        // resolve correctly on first frame. Required on targetSdk 35;
        // without it the camera preview can render behind opaque system
        // bars and look "stubbed".
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
            navigationBarStyle = SystemBarStyle.auto(
                lightScrim = android.graphics.Color.TRANSPARENT,
                darkScrim = android.graphics.Color.TRANSPARENT,
            ),
        )

        super.onCreate(savedInstanceState)

        // Handle intents (SEND, VIEW)
        handleIntent(intent)

        setContent {
            SafeOpenTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding(),
                ) {
                    MainScreen(viewModel)
                }
            }
        }

        // Release splash on the next frame after Compose attaches.
        window.decorView.post { keepSplash = false }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                viewModel.onShareReceived(text)
            }
            Intent.ACTION_VIEW -> {
                val uri = intent.data ?: return
                viewModel.onShareReceived(uri.toString())
            }
        }
    }
}

@Composable
private fun MainScreen(viewModel: MainViewModel) {
    val screen by viewModel.screen.collectAsState()
    val currentResult by viewModel.currentResult.collectAsState()
    val history by viewModel.historyFlow.collectAsState(emptyList())

    // Smooth transition between screens — slide-up + fade for the
    // result reveal, plain fade for back-navigation.
    AnimatedContent(
        targetState = screen,
        transitionSpec = {
            val enter = if (targetState == MainViewModel.Screen.RESULT) {
                slideInVertically(
                    animationSpec = tween(280),
                    initialOffsetY = { it / 6 },
                ) + fadeIn(animationSpec = tween(280))
            } else {
                fadeIn(animationSpec = tween(220))
            }
            val exit = fadeOut(animationSpec = tween(180))
            enter togetherWith exit
        },
        label = "screen-switch",
    ) { current ->
        when (current) {
            MainViewModel.Screen.SCANNER -> {
                ScannerScreen(
                    viewModel = viewModel,
                    onNavigateToHistory = { viewModel.navigateToHistory() },
                )
            }

            MainViewModel.Screen.RESULT -> {
                if (currentResult != null) {
                    ResultScreen(
                        result = currentResult!!,
                        onNavigateBack = { viewModel.navigateToScanner() },
                        onReInspect = { result -> viewModel.reInspectPayload(result) },
                    )
                }
            }

            MainViewModel.Screen.HISTORY -> {
                HistoryScreen(
                    history = history,
                    onNavigateBack = { viewModel.navigateToScanner() },
                    onSelectResult = { result ->
                        viewModel.reInspectPayload(result)
                    },
                    onClearHistory = { viewModel.clearHistory() },
                    viewModel = viewModel,
                )
            }
        }
    }
}
