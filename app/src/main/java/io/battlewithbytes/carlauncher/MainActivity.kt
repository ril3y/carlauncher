package io.battlewithbytes.carlauncher

import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import io.battlewithbytes.carlauncher.ui.UnifiedTeslaView
import io.battlewithbytes.carlauncher.ui.theme.BattleWithBytesCarLauncherTheme

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"
    private lateinit var mediaButtonHandler: MediaButtonHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hide system UI for true fullscreen launcher experience
        hideSystemUI()

        Log.d(TAG, "BattleWithBytes Launcher started - Unified Tesla UI")

        // Initialize media button handler
        mediaButtonHandler = MediaButtonHandler(this)

        setContent {
            BattleWithBytesCarLauncherTheme {
                UnifiedTeslaView()
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let {
            Log.d(TAG, "Key down: keyCode=$keyCode, scanCode=${it.scanCode}, action=${it.action}")
            if (ButtonEventHandler.handleKeyEvent(keyCode, it)) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        event?.let {
            Log.d(TAG, "Key up: keyCode=$keyCode")
        }
        return super.onKeyUp(keyCode, event)
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaButtonHandler.release()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        Log.d(TAG, "Back button pressed - ignoring to keep launcher active")
        // Disable back button to prevent exiting launcher
    }

    private fun hideSystemUI() {
        // Hide both status bar and navigation bar for fullscreen launcher
        window.insetsController?.let { controller ->
            controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
            controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Re-hide system UI when window regains focus
            hideSystemUI()
        }
    }
}
