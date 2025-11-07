package io.battlewithbytes.carlauncher

import android.util.Log
import android.view.KeyEvent

/**
 * Handler for physical button events from head unit
 */
object ButtonEventHandler {
    private const val TAG = "ButtonEvents"

    /**
     * Process key event and return true if handled
     */
    fun handleKeyEvent(keyCode: Int, event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }

        Log.d(TAG, "Button pressed: keyCode=$keyCode, event=$event")

        return when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                Log.d(TAG, "BACK button pressed")
                // Don't consume - let system handle
                false
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                Log.d(TAG, "PLAY/PAUSE button pressed")
                true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                Log.d(TAG, "NEXT TRACK button pressed")
                true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                Log.d(TAG, "PREVIOUS TRACK button pressed")
                true
            }
            KeyEvent.KEYCODE_MUSIC,
            KeyEvent.KEYCODE_APP_SWITCH -> {
                Log.d(TAG, "MUSIC/APP button pressed")
                true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER -> {
                Log.d(TAG, "CIRCLE/CENTER button pressed")
                true
            }
            KeyEvent.KEYCODE_N,
            KeyEvent.KEYCODE_NAVIGATE_NEXT,
            KeyEvent.KEYCODE_NAVIGATE_PREVIOUS -> {
                Log.d(TAG, "NAV button pressed")
                true
            }
            KeyEvent.KEYCODE_VOLUME_UP -> {
                Log.d(TAG, "VOLUME UP button pressed")
                false // Let system handle volume
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                Log.d(TAG, "VOLUME DOWN button pressed")
                false // Let system handle volume
            }
            else -> {
                Log.d(TAG, "Unknown button: keyCode=$keyCode")
                false
            }
        }
    }
}
