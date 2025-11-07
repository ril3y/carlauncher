package io.battlewithbytes.carlauncher.viewmodel

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

/**
 * Data class representing the current media playback state
 */
data class MediaPlaybackState(
    val trackTitle: String = "No media playing",
    val artistName: String = "Unknown Artist",
    val albumName: String = "",
    val albumArt: Bitmap? = null,
    val isPlaying: Boolean = false,
    val duration: Long = 0L,
    val position: Long = 0L,
    val hasActiveSession: Boolean = false,
    val lastPositionUpdateTime: Long = System.currentTimeMillis()
)

/**
 * ViewModel that monitors active media sessions (like Spotify, YouTube Music, etc.)
 * and exposes their playback state to the UI.
 *
 * Architecture:
 * - Uses MediaSessionManager to listen for active media sessions
 * - Creates MediaController instances for active sessions
 * - Maintains StateFlow for reactive UI updates
 * - Handles session lifecycle (active/inactive)
 *
 * Testing:
 * - Mock MediaSessionManager for unit tests
 * - Mock MediaController for playback control tests
 * - Use TestDispatcher for coroutine testing
 */
class MediaPlayerViewModel(
    private val context: Context
) : ViewModel() {

    private val TAG = "MediaPlayerViewModel"

    // MediaSessionManager for monitoring active sessions
    private val mediaSessionManager: MediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    // Current active media controller
    private var activeController: MediaController? = null

    // Exposed state for UI consumption
    private val _playbackState = MutableStateFlow(MediaPlaybackState())
    val playbackState: StateFlow<MediaPlaybackState> = _playbackState.asStateFlow()

    // Position update timer job
    private var positionUpdateJob: Job? = null

    // Listener for active session changes
    private val activeSessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
        Log.d(TAG, "Active sessions changed. Count: ${controllers?.size ?: 0}")
        controllers?.forEach { controller ->
            Log.d(TAG, "  Session: ${controller.packageName} - ${controller.sessionToken}")
        }
        handleActiveSessionsChanged(controllers)
    }

    // Callback for metadata changes
    private val metadataCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            Log.d(TAG, "Metadata changed")
            metadata?.let {
                updateMetadata(it)
            }
        }

        override fun onPlaybackStateChanged(state: PlaybackState?) {
            Log.d(TAG, "Playback state changed: state=${state?.state}, position=${state?.position}")
            state?.let {
                updatePlaybackState(it)
            }
        }

        override fun onSessionDestroyed() {
            Log.d(TAG, "Session destroyed")
            handleSessionDestroyed()
        }
    }

    init {
        // Register for active session changes
        // Note: Requires BIND_NOTIFICATION_LISTENER_SERVICE permission
        try {
            val notificationListener = ComponentName(context, context.packageName)
            mediaSessionManager.addOnActiveSessionsChangedListener(
                activeSessionsChangedListener,
                notificationListener
            )

            // Check for existing active sessions
            val activeSessions = mediaSessionManager.getActiveSessions(notificationListener)
            Log.d(TAG, "Initial active sessions: ${activeSessions.size}")
            handleActiveSessionsChanged(activeSessions)
        } catch (e: SecurityException) {
            Log.e(TAG, "Failed to register session listener. Missing notification listener permission?", e)
            Log.e(TAG, "Please enable notification access in Settings -> Apps -> Permissions")
        }
    }

    /**
     * Handles when the list of active media sessions changes
     */
    private fun handleActiveSessionsChanged(controllers: List<MediaController>?) {
        viewModelScope.launch {
            if (controllers.isNullOrEmpty()) {
                Log.d(TAG, "No active media sessions")
                clearActiveController()
                return@launch
            }

            // Prioritize sessions that are currently playing
            val playingController = controllers.firstOrNull {
                it.playbackState?.state == PlaybackState.STATE_PLAYING
            }

            // If none playing, take the first active session
            val newController = playingController ?: controllers.firstOrNull()

            newController?.let { controller ->
                Log.d(TAG, "Using media session from: ${controller.packageName}")
                setActiveController(controller)
            } ?: clearActiveController()
        }
    }

    /**
     * Sets a new active media controller and registers callbacks
     */
    private fun setActiveController(controller: MediaController) {
        // Unregister from previous controller
        activeController?.unregisterCallback(metadataCallback)

        // Set new controller
        activeController = controller

        // Register callback
        controller.registerCallback(metadataCallback)

        // Update current state
        controller.metadata?.let { updateMetadata(it) }
        controller.playbackState?.let { updatePlaybackState(it) }

        Log.d(TAG, "Active controller set: ${controller.packageName}")
    }

    /**
     * Clears the active controller and resets state
     */
    private fun clearActiveController() {
        activeController?.unregisterCallback(metadataCallback)
        activeController = null

        _playbackState.value = MediaPlaybackState()
        Log.d(TAG, "Active controller cleared")
    }

    /**
     * Updates metadata from the media session
     */
    private fun updateMetadata(metadata: MediaMetadata) {
        val trackTitle = metadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: "Unknown Track"
        val artistName = metadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: "Unknown Artist"
        val albumName = metadata.getString(MediaMetadata.METADATA_KEY_ALBUM)
            ?: ""
        val albumArt = metadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        val duration = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION)

        Log.d(TAG, "Metadata: $trackTitle - $artistName - $albumName")

        _playbackState.value = _playbackState.value.copy(
            trackTitle = trackTitle,
            artistName = artistName,
            albumName = albumName,
            albumArt = albumArt,
            duration = duration,
            hasActiveSession = true
        )
    }

    /**
     * Updates playback state from the media session
     */
    private fun updatePlaybackState(state: PlaybackState) {
        val isPlaying = state.state == PlaybackState.STATE_PLAYING
        val position = state.position

        Log.d(TAG, "Playback state: isPlaying=$isPlaying, position=$position")

        _playbackState.value = _playbackState.value.copy(
            isPlaying = isPlaying,
            position = position,
            hasActiveSession = true,
            lastPositionUpdateTime = System.currentTimeMillis()
        )

        // Start or stop position update timer based on playback state
        if (isPlaying) {
            startPositionUpdateTimer()
        } else {
            stopPositionUpdateTimer()
        }
    }

    /**
     * Starts a coroutine that updates the position every second while playing
     */
    private fun startPositionUpdateTimer() {
        // Cancel existing timer if running
        stopPositionUpdateTimer()

        positionUpdateJob = viewModelScope.launch {
            while (true) {
                delay(1000L) // Update every second

                val currentState = _playbackState.value
                if (currentState.isPlaying && currentState.hasActiveSession) {
                    // Increment by 1 second (1000ms) instead of calculating elapsed time
                    val estimatedPosition = (currentState.position + 1000L).coerceAtMost(currentState.duration)

                    _playbackState.value = currentState.copy(
                        position = estimatedPosition,
                        lastPositionUpdateTime = System.currentTimeMillis()
                    )

                    Log.d(TAG, "Position updated: $estimatedPosition / ${currentState.duration}")
                } else {
                    // Stop timer if not playing anymore
                    break
                }
            }
        }
    }

    /**
     * Stops the position update timer
     */
    private fun stopPositionUpdateTimer() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }

    /**
     * Handles when the active session is destroyed
     */
    private fun handleSessionDestroyed() {
        clearActiveController()
    }

    // Playback control methods

    /**
     * Toggles play/pause on the active media session
     */
    fun togglePlayPause() {
        activeController?.let { controller ->
            val isPlaying = controller.playbackState?.state == PlaybackState.STATE_PLAYING
            if (isPlaying) {
                Log.d(TAG, "Sending pause command")
                controller.transportControls?.pause()
            } else {
                Log.d(TAG, "Sending play command")
                controller.transportControls?.play()
            }
        } ?: Log.w(TAG, "No active controller for play/pause")
    }

    /**
     * Skips to the next track
     */
    fun skipToNext() {
        activeController?.let { controller ->
            Log.d(TAG, "Sending skip next command")
            controller.transportControls?.skipToNext()
        } ?: Log.w(TAG, "No active controller for skip next")
    }

    /**
     * Skips to the previous track
     */
    fun skipToPrevious() {
        activeController?.let { controller ->
            Log.d(TAG, "Sending skip previous command")
            controller.transportControls?.skipToPrevious()
        } ?: Log.w(TAG, "No active controller for skip previous")
    }

    /**
     * Stops playback
     */
    fun stop() {
        activeController?.let { controller ->
            Log.d(TAG, "Sending stop command")
            controller.transportControls?.stop()
        } ?: Log.w(TAG, "No active controller for stop")
    }

    override fun onCleared() {
        super.onCleared()
        // Cleanup
        stopPositionUpdateTimer()
        activeController?.unregisterCallback(metadataCallback)
        mediaSessionManager.removeOnActiveSessionsChangedListener(activeSessionsChangedListener)
        Log.d(TAG, "ViewModel cleared")
    }
}

/**
 * Factory for creating MediaPlayerViewModel with Context dependency
 */
class MediaPlayerViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MediaPlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MediaPlayerViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
