package io.battlewithbytes.carlauncher.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import io.battlewithbytes.carlauncher.R
import io.battlewithbytes.carlauncher.model.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Foreground service for CAN bus communication
 * Maintains connection to CAN interface and broadcasts vehicle data
 */
class CanBusService : Service() {

    private val binder = CanBusServiceBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _batteryData = MutableStateFlow<BatteryData?>(null)
    val batteryData: StateFlow<BatteryData?> = _batteryData.asStateFlow()

    private val _motorData = MutableStateFlow<MotorData?>(null)
    val motorData: StateFlow<MotorData?> = _motorData.asStateFlow()

    private val _vehicleStatus = MutableStateFlow<VehicleStatus?>(null)
    val vehicleStatus: StateFlow<VehicleStatus?> = _vehicleStatus.asStateFlow()

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private var canInterfaceUrl = "http://192.168.1.100:8080" // Default, can be configured
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    enum class ConnectionState {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
        ERROR
    }

    inner class CanBusServiceBinder : Binder() {
        fun getService(): CanBusService = this@CanBusService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, createNotification())
        startCanMonitoring()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    private fun createNotification(): Notification {
        createNotificationChannel()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.can_service_notification_title))
            .setContentText(getString(R.string.can_service_notification_text))
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CAN Bus Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitors vehicle CAN bus data"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Start monitoring CAN bus data
     */
    private fun startCanMonitoring() {
        serviceScope.launch {
            while (isActive) {
                try {
                    _connectionState.value = ConnectionState.CONNECTING
                    fetchCanData()
                    _connectionState.value = ConnectionState.CONNECTED
                } catch (e: Exception) {
                    _connectionState.value = ConnectionState.ERROR
                    e.printStackTrace()
                }
                delay(100) // Poll every 100ms
            }
        }
    }

    /**
     * Fetch CAN data from interface
     * This is a placeholder - implement based on your actual CAN interface
     */
    private suspend fun fetchCanData() {
        // TODO: Replace with actual CAN interface protocol
        // This is a mock implementation

        // Simulate parsing different CAN message IDs
        // You'll need to implement actual parsing based on your CAN protocol

        // Example: Parse battery data from CAN ID 0x100
        _batteryData.value = BatteryData(
            voltageV = 48.5f,
            currentA = 15.2f,
            stateOfChargePercent = 78.5f,
            temperatureC = 25.0f
        )

        // Example: Parse motor data from CAN ID 0x200
        _motorData.value = MotorData(
            rpm = 3500,
            temperatureC = 65.0f,
            currentA = 45.0f,
            powerKw = 2.5f,
            faultCode = 0
        )

        // Example: Parse vehicle status from CAN ID 0x300
        _vehicleStatus.value = VehicleStatus(
            speedKmh = 25.5f,
            odometerKm = 1250.0f,
            tripMeterKm = 15.5f
        )
    }

    /**
     * Send CAN message to the bus
     * @param id CAN message ID
     * @param data Data bytes to send
     */
    suspend fun sendCanMessage(id: Int, data: ByteArray): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val message = CanMessage(id, data)
                // TODO: Implement actual sending based on your CAN interface
                // This is a placeholder
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * Configure CAN interface URL
     */
    fun setCanInterfaceUrl(url: String) {
        canInterfaceUrl = url
    }

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "can_bus_service"
    }
}
