package io.battlewithbytes.carlauncher.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager

/**
 * Manages registered media apps (Spotify, YouTube Music, etc.)
 */
class MediaAppManager(private val context: Context) {

    companion object {
        // Known media app packages
        const val SPOTIFY_PACKAGE = "com.spotify.music"
        const val YOUTUBE_MUSIC_PACKAGE = "com.google.android.apps.youtube.music"
        const val APPLE_MUSIC_PACKAGE = "com.apple.android.music"
    }

    data class MediaApp(
        val packageName: String,
        val appName: String,
        val isInstalled: Boolean
    )

    fun getInstalledMediaApps(): List<MediaApp> {
        val apps = listOf(
            MediaApp(SPOTIFY_PACKAGE, "Spotify", isAppInstalled(SPOTIFY_PACKAGE)),
            MediaApp(YOUTUBE_MUSIC_PACKAGE, "YouTube Music", isAppInstalled(YOUTUBE_MUSIC_PACKAGE)),
            MediaApp(APPLE_MUSIC_PACKAGE, "Apple Music", isAppInstalled(APPLE_MUSIC_PACKAGE))
        )
        return apps.filter { it.isInstalled }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun launchMediaApp(packageName: String) {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(packageName)
            if (launchIntent != null) {
                context.startActivity(launchIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Register Spotify for media controls
     */
    fun registerSpotify() {
        if (isAppInstalled(SPOTIFY_PACKAGE)) {
            // Spotify will automatically register with MediaSession when playing
            // We just need to ensure we're listening to media button events
        }
    }

    /**
     * Get the currently playing media app from MediaController
     */
    fun getCurrentMediaApp(): String? {
        // TODO: Query MediaSessionManager for active sessions
        return null
    }
}
