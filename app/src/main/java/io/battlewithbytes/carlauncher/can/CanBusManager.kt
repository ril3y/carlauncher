package io.battlewithbytes.carlauncher.can

import android.content.Context
import android.util.Log
import io.battlewithbytes.carlauncher.can.protocol.CanMessage
import io.battlewithbytes.carlauncher.can.state.VehicleStateManager
import io.battlewithbytes.carlauncher.usb.UsbCanBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Main CAN bus manager - orchestrates message flow from USB to state
 * Integrates with existing UsbCanBridge and provides reactive state updates
 *
 * Provides two consumption patterns:
 * 1. Global state (VehicleStateManager) - for system-wide state
 * 2. Pub/Sub (MessageSubscriptionManager) - for isolated UI widgets
 */
class CanBusManager(context: Context) {
    private val TAG = "CanBusManager"

    private val usbBridge = UsbCanBridge(context)

    // Global state manager (legacy/system-wide state)
    val stateManager = VehicleStateManager()

    // Pub/Sub manager (for UI widget subscriptions)
    val subscriptions = MessageSubscriptionManager()

    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // Performance metrics
    private val _messageRate = MutableStateFlow(0)
    val messageRate: StateFlow<Int> = _messageRate.asStateFlow()

    private val _crcErrorRate = MutableStateFlow(0f)
    val crcErrorRate: StateFlow<Float> = _crcErrorRate.asStateFlow()

    private val _connectionState = MutableStateFlow(UsbCanBridge.ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<UsbCanBridge.ConnectionState> = _connectionState.asStateFlow()

    private var messageCount = 0
    private var crcErrorCount = 0

    init {
        // Monitor USB bridge connection state
        scope.launch {
            usbBridge.connectionState.collect { state ->
                _connectionState.value = state
                Log.d(TAG, "USB Connection State: $state")
            }
        }

        // Monitor USB bridge for incoming CAN messages
        scope.launch {
            usbBridge.canMessages
                .filterNotNull()
                .collect { usbMessage ->
                    handleRawCanMessage(usbMessage)
                }
        }

        // Performance monitoring (updates every second)
        scope.launch {
            while (isActive) {
                delay(1000)
                _messageRate.value = messageCount
                _crcErrorRate.value = if (messageCount > 0) {
                    (crcErrorCount.toFloat() / messageCount) * 100f
                } else 0f
                messageCount = 0
                crcErrorCount = 0
            }
        }

        // Staleness monitoring
        scope.launch {
            while (isActive) {
                delay(5000) // Check every 5 seconds
                val staleness = stateManager.getCriticalStaleness()
                staleness.forEach { (stream, isStale) ->
                    if (isStale) {
                        Log.w(TAG, "Stream $stream is STALE")
                    }
                }
            }
        }
    }

    /**
     * Handle raw CAN message from USB bridge
     */
    private fun handleRawCanMessage(usbMessage: UsbCanBridge.CanMessage) {
        messageCount++

        // Parse message using protocol parser
        val parsed = CanMessage.parse(usbMessage.id, usbMessage.data, usbMessage.timestamp)

        if (!parsed.crcValid) {
            crcErrorCount++
            Log.w(TAG, "CRC error on message ID 0x${usbMessage.id.toString(16)}")
        }

        // Update global state manager
        stateManager.updateMessage(parsed)

        // Publish to subscribers (Pub/Sub pattern)
        scope.launch {
            subscriptions.publish(parsed)
        }

        // Log critical errors
        if (parsed is CanMessage.BmsCriticalAlarms && parsed.hasAnyAlarm) {
            Log.e(TAG, "BMS CRITICAL ALARM: V=${parsed.voltageAlarms} I=${parsed.currentAlarms} T=${parsed.tempAlarms}")
        }

        // Debug logging for key messages
        when (parsed) {
            is CanMessage.BmsPackStatus -> {
                Log.v(TAG, "BMS Pack: ${parsed.packVoltageV}V ${parsed.packCurrentA}A ${parsed.soc}% (${if (parsed.isCharging) "Charging" else "Discharging"})")
            }
            is CanMessage.SocCapacity -> {
                Log.v(TAG, "SOC Authority: ${parsed.soc}% (confidence: ${parsed.socConfidence}%)")
            }
            is CanMessage.MotorBasic -> {
                Log.v(TAG, "Motor ${parsed.motorNumber}: ${parsed.rpm}rpm ${parsed.currentA}A ${parsed.throttlePercent}%")
            }
            else -> { /* No debug logging for other types */ }
        }
    }

    /**
     * Connect to USB CAN device
     */
    fun connect() {
        Log.d(TAG, "Initiating CAN bus connection")
        usbBridge.findAndConnect()
    }

    /**
     * Disconnect from USB CAN device
     */
    fun disconnect() {
        Log.d(TAG, "Disconnecting from CAN bus")
        usbBridge.disconnect()
    }

    /**
     * Send CAN message to bus (for future use - commands)
     */
    fun sendMessage(id: Int, data: ByteArray) {
        usbBridge.sendCanMessage(id, data)
    }

    /**
     * Cleanup resources
     */
    fun cleanup() {
        scope.cancel()
        usbBridge.cleanup()
    }
}
