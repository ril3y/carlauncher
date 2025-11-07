package io.battlewithbytes.carlauncher.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Binder
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import io.battlewithbytes.carlauncher.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

/**
 * TelemetryService - Handles GPS tracking, ground speed calculation, and CAN bus logging
 *
 * Features:
 * - High-frequency GPS updates (1 second intervals)
 * - Accurate ground speed calculation from GPS
 * - Location data with altitude, bearing, accuracy
 * - CAN bus integration for telemetry logging
 * - Foreground service for reliable operation
 */
class TelemetryService : Service() {

    companion object {
        private const val TAG = "TelemetryService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "telemetry_channel"

        // GPS update intervals
        private const val GPS_UPDATE_INTERVAL = 1000L // 1 second
        private const val GPS_FASTEST_INTERVAL = 500L // 0.5 seconds

        // CAN message IDs for telemetry (adjust based on your protocol)
        private const val CAN_GPS_SPEED_ID = 0x201
        private const val CAN_GPS_LOCATION_ID = 0x202
        private const val CAN_GPS_ALTITUDE_ID = 0x203
    }

    private val binder = TelemetryBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // GPS/Location services
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    // Telemetry data flows
    private val _telemetryData = MutableStateFlow(TelemetryData())
    val telemetryData: StateFlow<TelemetryData> = _telemetryData.asStateFlow()

    // CAN bus stub (will be connected to real CAN service later)
    private var canBusEnabled = false

    inner class TelemetryBinder : Binder() {
        fun getService(): TelemetryService = this@TelemetryService
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "TelemetryService created")

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())

        initializeLocationServices()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "TelemetryService started")
        startLocationUpdates()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "TelemetryService destroyed")
        stopLocationUpdates()
        serviceScope.cancel()
    }

    private fun initializeLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    processLocationUpdate(location)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            Log.w(TAG, "Location permission not granted")
            return
        }

        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            GPS_UPDATE_INTERVAL
        ).apply {
            setMinUpdateIntervalMillis(GPS_FASTEST_INTERVAL)
            setMaxUpdateDelayMillis(GPS_UPDATE_INTERVAL)
        }.build()

        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null // Looper - use main looper by default
            )
            Log.d(TAG, "Location updates started")
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception starting location updates", e)
        }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
        Log.d(TAG, "Location updates stopped")
    }

    private fun processLocationUpdate(location: Location) {
        // Calculate ground speed from GPS
        val groundSpeedMps = location.speed // m/s from GPS
        val groundSpeedKmh = groundSpeedMps * 3.6f // Convert to km/h

        // Create telemetry data
        val telemetry = TelemetryData(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = location.altitude,
            groundSpeed = groundSpeedKmh,
            bearing = location.bearing,
            accuracy = location.accuracy,
            timestamp = location.time,
            hasValidFix = location.accuracy < 20f // Good accuracy threshold
        )

        // Update state flow
        _telemetryData.value = telemetry

        // Log to CAN bus (stubbed for now)
        logTelemetryToCan(telemetry)

        Log.v(TAG, "Telemetry: Speed=${String.format("%.1f", groundSpeedKmh)} km/h, " +
                "Lat=${location.latitude}, Lon=${location.longitude}, " +
                "Alt=${location.altitude}m, Accuracy=${location.accuracy}m")
    }

    /**
     * Log telemetry data to CAN bus
     * This is currently stubbed - will be connected to real CAN service
     */
    private fun logTelemetryToCan(telemetry: TelemetryData) {
        if (!canBusEnabled) {
            // CAN bus not enabled yet - stub
            return
        }

        serviceScope.launch {
            try {
                // TODO: Connect to CanBusService and send telemetry

                // Example CAN message structure (to be implemented):
                // CAN_GPS_SPEED_ID: [speed_high, speed_low, accuracy, fix_quality]
                // CAN_GPS_LOCATION_ID: [lat_bytes..., lon_bytes...]
                // CAN_GPS_ALTITUDE_ID: [alt_high, alt_low, bearing_high, bearing_low]

                Log.v(TAG, "CAN: Would send telemetry to bus (stubbed)")

            } catch (e: Exception) {
                Log.e(TAG, "Error logging telemetry to CAN", e)
            }
        }
    }

    /**
     * Enable/disable CAN bus logging
     */
    fun setCanBusEnabled(enabled: Boolean) {
        canBusEnabled = enabled
        Log.d(TAG, "CAN bus logging ${if (enabled) "enabled" else "disabled"}")
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Telemetry Service",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "GPS and telemetry tracking"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Telemetry Active")
            .setContentText("GPS tracking and telemetry logging")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Use system icon
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun CoroutineScope.asExecutor() = CoroutineDispatcher(coroutineContext)

    private class CoroutineDispatcher(
        private val context: kotlin.coroutines.CoroutineContext
    ) : java.util.concurrent.Executor {
        override fun execute(command: Runnable) {
            CoroutineScope(context).launch { command.run() }
        }
    }
}

/**
 * Telemetry data class containing all GPS and calculated values
 */
data class TelemetryData(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val altitude: Double = 0.0,
    val groundSpeed: Float = 0f, // km/h calculated from GPS
    val bearing: Float = 0f, // degrees
    val accuracy: Float = 0f, // meters
    val timestamp: Long = 0L,
    val hasValidFix: Boolean = false
) {
    /**
     * Get ground speed in different units
     */
    fun getSpeedMph(): Float = groundSpeed * 0.621371f
    fun getSpeedMps(): Float = groundSpeed / 3.6f

    /**
     * Format coordinates for display
     */
    fun getFormattedCoordinates(): String {
        return String.format("%.6f, %.6f", latitude, longitude)
    }

    /**
     * Check if moving (speed > 1 km/h)
     */
    fun isMoving(): Boolean = groundSpeed > 1.0f
}
