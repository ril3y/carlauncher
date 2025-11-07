package io.battlewithbytes.carlauncher.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView as OsmMapView
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay

@Composable
fun MapView(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = true
) {
    val context = LocalContext.current
    var currentLocation by remember { mutableStateOf<Location?>(null) }

    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(
            context,
            context.getSharedPreferences("osmdroid", Context.MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = context.packageName
    }

    // Request location updates
    LaunchedEffect(Unit) {
        if (hasLocationPermission(context)) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                5000L // Update every 5 seconds
            ).build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    currentLocation = locationResult.lastLocation
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    null
                )
            } catch (e: SecurityException) {
                // Location permission not granted
            }
        }
    }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            OsmMapView(ctx).apply {
                // Set tile source based on theme
                setTileSource(if (isDarkMode) {
                    // Use standard MAPNIK for now (dark overlay will be added via CSS/style in future)
                    TileSourceFactory.MAPNIK
                } else {
                    TileSourceFactory.MAPNIK
                })

                // Enable multi-touch controls
                setMultiTouchControls(true)

                // Set zoom level and center
                controller.setZoom(18.0) // Good zoom for driving

                // Default center (will be updated with GPS)
                controller.setCenter(GeoPoint(37.7749, -122.4194)) // San Francisco default

                // Constrain map scrolling
                isHorizontalMapRepetitionEnabled = false
                isVerticalMapRepetitionEnabled = false
                setScrollableAreaLimitDouble(null) // Allow full world scrolling

                // Disable scroll gestures during driving (Tesla style)
                setBuiltInZoomControls(false)

                // Add location overlay if permission granted
                if (hasLocationPermission(ctx)) {
                    val locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(ctx), this)
                    locationOverlay.enableMyLocation()
                    locationOverlay.enableFollowLocation() // Auto-center on current location
                    overlays.add(locationOverlay)
                }

                // Theme-specific styling
                if (isDarkMode) {
                    // Dark mode - much darker inverted colors for Tesla-style night visibility
                    overlayManager.tilesOverlay.loadingBackgroundColor = android.graphics.Color.parseColor("#0A0A0A")
                    overlayManager.tilesOverlay.loadingLineColor = android.graphics.Color.parseColor("#1A1A1A")

                    // Apply much darker filter overlay to map tiles
                    val darkOverlay = android.graphics.ColorMatrix().apply {
                        // Stronger inversion and darker brightness for true dark mode
                        set(floatArrayOf(
                            -0.8f, 0f, 0f, 0f, 200f,  // Darker red channel
                            0f, -0.8f, 0f, 0f, 200f,  // Darker green channel
                            0f, 0f, -0.8f, 0f, 200f,  // Darker blue channel
                            0f, 0f, 0f, 1f, 0f         // Alpha unchanged
                        ))
                    }
                    overlayManager.tilesOverlay.setColorFilter(
                        android.graphics.ColorMatrixColorFilter(darkOverlay)
                    )
                } else {
                    // Light mode - standard colors
                    overlayManager.tilesOverlay.loadingBackgroundColor = android.graphics.Color.WHITE
                    overlayManager.tilesOverlay.loadingLineColor = android.graphics.Color.LTGRAY
                }
            }
        },
        update = { mapView ->
            // Update map center when location changes
            currentLocation?.let { location ->
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                mapView.controller.animateTo(geoPoint)
            }
        }
    )
}

private fun hasLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}
