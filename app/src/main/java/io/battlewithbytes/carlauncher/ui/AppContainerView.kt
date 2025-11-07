package io.battlewithbytes.carlauncher.ui

import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import android.app.Activity
import android.widget.FrameLayout

/**
 * Container that launches apps within the map area while keeping sidebar visible
 * Uses Activity embedding to contain apps in a specific area
 */
@Composable
fun AppContainerView(
    packageName: String?,
    activityName: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (packageName != null && activityName != null) {
            // Launch the app in embedded mode
            LaunchedEffect(packageName, activityName) {
                try {
                    launchAppInContainer(context, packageName, activityName)
                } catch (e: Exception) {
                    e.printStackTrace()
                    onDismiss()
                }
            }

            // Placeholder view for now - we'll need to implement activity embedding
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "App Container: $packageName",
                    color = Color.White
                )
            }
        }
    }
}

/**
 * Launch an app activity with bounds set to only the map area
 * This keeps the sidebar visible while the app runs
 */
private fun launchAppInContainer(
    context: Context,
    packageName: String,
    activityName: String
) {
    val intent = Intent().apply {
        setClassName(packageName, activityName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
    }

    // Launch with options to constrain to specific bounds
    // Note: This requires SYSTEM_ALERT_WINDOW permission for overlay windows
    // Or we need to use ActivityView (API 28+) for proper embedding
    context.startActivity(intent)
}

/**
 * Data class for app launch info
 */
data class AppLaunchInfo(
    val packageName: String,
    val activityName: String,
    val displayName: String
)

/**
 * Common apps that users might want to launch
 */
val commonApps = listOf(
    AppLaunchInfo(
        packageName = "com.spotify.music",
        activityName = "com.spotify.music.MainActivity",
        displayName = "Spotify"
    ),
    AppLaunchInfo(
        packageName = "com.google.android.apps.maps",
        activityName = "com.google.android.maps.MapsActivity",
        displayName = "Google Maps"
    ),
    AppLaunchInfo(
        packageName = "com.android.settings",
        activityName = "com.android.settings.Settings",
        displayName = "Settings"
    )
)
