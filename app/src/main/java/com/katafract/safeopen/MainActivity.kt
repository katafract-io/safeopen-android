package com.katafract.safeopen

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.katafract.safeopen.ui.HistoryScreen
import com.katafract.safeopen.ui.ResultScreen
import com.katafract.safeopen.ui.ScannerScreen
import com.katafract.safeopen.ui.theme.SafeOpenTheme
import com.katafract.safeopen.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Handle intents (SEND, VIEW)
        handleIntent(intent)

        setContent {
            SafeOpenTheme {
                MainScreen(viewModel)
            }
        }
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
    val history by viewModel.history.collectAsState()

    when (screen) {
        MainViewModel.Screen.SCANNER -> {
            ScannerScreen(
                viewModel = viewModel,
                onNavigateToHistory = { viewModel.navigateToHistory() }
            )
        }

        MainViewModel.Screen.RESULT -> {
            if (currentResult != null) {
                ResultScreen(
                    result = currentResult!!,
                    onNavigateBack = { viewModel.navigateToScanner() },
                    onReInspect = { result -> viewModel.reInspectPayload(result) }
                )
            }
        }

        MainViewModel.Screen.HISTORY -> {
            HistoryScreen(
                history = history,
                onNavigateBack = { viewModel.navigateToScanner() },
                onSelectResult = { result ->
                    // Load result and go to result screen
                    viewModel.reInspectPayload(result)
                },
                onClearHistory = { viewModel.clearHistory() }
            )
        }
    }
}
