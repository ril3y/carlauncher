package io.battlewithbytes.carlauncher

import android.content.Context
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent

/**
 * Handles media button events from head unit
 */
class MediaButtonHandler(context: Context) {
    private val TAG = "MediaButtonHandler"

    private val mediaSession = MediaSessionCompat(context, "BattleWithBytesLauncher").apply {
        setFlags(
            MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
            MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS
        )

        setCallback(object : MediaSessionCompat.Callback() {
            override fun onMediaButtonEvent(mediaButtonEvent: android.content.Intent?): Boolean {
                Log.d(TAG, "Media button event received: $mediaButtonEvent")

                mediaButtonEvent?.let { intent ->
                    val event = intent.getParcelableExtra<KeyEvent>(android.content.Intent.EXTRA_KEY_EVENT)
                    event?.let { keyEvent ->
                        if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                            handleMediaButton(keyEvent.keyCode)
                        }
                    }
                }
                return true
            }

            override fun onPlay() {
                Log.d(TAG, "onPlay()")
                handleMediaButton(KeyEvent.KEYCODE_MEDIA_PLAY)
            }

            override fun onPause() {
                Log.d(TAG, "onPause()")
                handleMediaButton(KeyEvent.KEYCODE_MEDIA_PAUSE)
            }

            override fun onSkipToNext() {
                Log.d(TAG, "onSkipToNext()")
                handleMediaButton(KeyEvent.KEYCODE_MEDIA_NEXT)
            }

            override fun onSkipToPrevious() {
                Log.d(TAG, "onSkipToPrevious()")
                handleMediaButton(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            }

            override fun onStop() {
                Log.d(TAG, "onStop()")
            }
        })

        // Set initial playback state
        setPlaybackState(
            PlaybackStateCompat.Builder()
                .setState(PlaybackStateCompat.STATE_PAUSED, 0, 1.0f)
                .setActions(
                    PlaybackStateCompat.ACTION_PLAY or
                    PlaybackStateCompat.ACTION_PAUSE or
                    PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                    PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
                    PlaybackStateCompat.ACTION_STOP
                )
                .build()
        )

        isActive = true
    }

    private fun handleMediaButton(keyCode: Int) {
        Log.d(TAG, "Media button pressed: keyCode=$keyCode")
        when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY,
            KeyEvent.KEYCODE_MEDIA_PAUSE,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                Log.d(TAG, "PLAY/PAUSE button")
                // TODO: Handle play/pause
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                Log.d(TAG, "NEXT track button")
                // TODO: Handle next track
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                Log.d(TAG, "PREVIOUS track button")
                // TODO: Handle previous track
            }
        }
    }

    fun release() {
        mediaSession.isActive = false
        mediaSession.release()
    }
}
