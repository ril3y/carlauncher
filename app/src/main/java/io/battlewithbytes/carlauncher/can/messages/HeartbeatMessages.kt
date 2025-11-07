package io.battlewithbytes.carlauncher.can.messages

import io.battlewithbytes.carlauncher.can.*

/**
 * Heartbeat Messages (0x600-0x607)
 * All devices send at 1Hz with identical format
 */

/**
 * Device status codes
 */
enum class DeviceStatus(val code: UByte) {
    ERROR(0x00u),
    OK(0x01u),
    WARNING(0x02u),
    INIT(0x03u);

    companion object {
        fun fromCode(code: UByte): DeviceStatus {
            return values().find { it.code == code } ?: ERROR
        }
    }
}

/**
 * Device type enumeration mapping to CAN IDs
 */
enum class DeviceType(val canId: Int, val description: String) {
    WIRING_HARNESS(0x600, "Wiring Harness Controller"),
    MOTOR_1(0x601, "Motor Controller 1"),
    MOTOR_2(0x602, "Motor Controller 2"),
    DISPLAY(0x603, "Display/Dashboard"),
    BMS(0x604, "Battery Management System"),
    SOLAR_CHARGER(0x605, "Solar Charge Controller"),
    GPS(0x606, "GPS Module"),
    BATTERY_MONITOR(0x607, "Battery Monitor (Coulomb Counter)");

    companion object {
        private val idMap = values().associateBy { it.canId }

        fun fromCanId(canId: Int): DeviceType? = idMap[canId]
    }
}

/**
 * Battery Monitor specific flags (byte 5 of 0x607)
 */
data class BatteryMonitorFlags(
    val shuntCalibrated: Boolean,   // Bit 0
    val socValid: Boolean,          // Bit 1
    val framOperational: Boolean,   // Bit 2
    val rtcSynced: Boolean          // Bit 3
    // Bits 4-7: Reserved
) {
    companion object {
        fun fromByte(byte: UByte): BatteryMonitorFlags {
            return BatteryMonitorFlags(
                shuntCalibrated = byte.isBitSet(0),
                socValid = byte.isBitSet(1),
                framOperational = byte.isBitSet(2),
                rtcSynced = byte.isBitSet(3)
            )
        }
    }
}

/**
 * Generic heartbeat message for all devices (0x600-0x607)
 * Transmission: 1Hz
 */
data class HeartbeatMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,
    override val canId: Int,

    // Byte 0: Status code
    val status: DeviceStatus,

    // Byte 1: Error count (rolling counter)
    val errorCount: UByte,

    // Byte 2: Current error code
    val currentError: ErrorCode,

    // Byte 3: Protocol version
    val protocolVersion: UByte,     // 0x01 = v1.4 optimized

    // Byte 4: Temperature (with +40°C offset)
    val temperatureCelsius: Int,    // Actual temperature in °C

    // Byte 5: Device-specific data
    val deviceSpecific: UByte,

    // Byte 6: Uptime (lower 8 bits, wraps every 256 seconds)
    val uptimeSeconds: UByte

) : CanMessage {

    companion object {
        const val UPDATE_RATE_MS = 1000L // 1Hz
        const val MAX_AGE_MS = 3000L // Stale after 3 seconds (3 missed heartbeats)

        /**
         * Parses a CAN frame into a HeartbeatMessage
         */
        fun parse(canId: Int, data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<HeartbeatMessage> {
            if (canId !in 0x600..0x607) {
                return ParseResult.InvalidData(canId, "Invalid heartbeat CAN ID: 0x${canId.toString(16)}")
            }

            if (data.size != 8) {
                return ParseResult.InvalidData(canId, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(canId, data)
            }

            val statusCode = data.getUInt8(0)
            val errorCount = data.getUInt8(1)
            val currentErrorCode = data.getUInt8(2)
            val protocolVersion = data.getUInt8(3)
            val temperatureRaw = data.getUInt8(4)
            val deviceSpecific = data.getUInt8(5)
            val uptime = data.getUInt8(6)

            val message = HeartbeatMessage(
                timestamp = timestamp,
                isValid = true,
                canId = canId,
                status = DeviceStatus.fromCode(statusCode),
                errorCount = errorCount,
                currentError = ErrorCode.fromCode(currentErrorCode),
                protocolVersion = protocolVersion,
                temperatureCelsius = temperatureRaw.toTemperatureWithOffset(),
                deviceSpecific = deviceSpecific,
                uptimeSeconds = uptime
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Gets the device type for this heartbeat
     */
    fun getDeviceType(): DeviceType? = DeviceType.fromCanId(canId)

    /**
     * Checks if device is in OK state
     */
    fun isOk(): Boolean = status == DeviceStatus.OK && currentError == ErrorCode.NO_ERROR

    /**
     * Checks if device has any active error
     */
    fun hasError(): Boolean = currentError != ErrorCode.NO_ERROR

    /**
     * Checks if device has critical error
     */
    fun hasCriticalError(): Boolean = currentError.isCritical()

    /**
     * Gets battery monitor flags (only valid for 0x607)
     */
    fun getBatteryMonitorFlags(): BatteryMonitorFlags? {
        return if (canId == 0x607) {
            BatteryMonitorFlags.fromByte(deviceSpecific)
        } else {
            null
        }
    }

    /**
     * Checks if this is a battery monitor heartbeat
     */
    fun isBatteryMonitor(): Boolean = canId == 0x607

    /**
     * Checks if temperature is within safe operating range
     * @param maxTemp Maximum safe temperature in Celsius
     */
    fun isTemperatureOk(maxTemp: Int = 70): Boolean {
        return temperatureCelsius in -20..maxTemp
    }

    /**
     * Gets human-readable device description
     */
    fun getDeviceDescription(): String {
        return getDeviceType()?.description ?: "Unknown Device (0x${canId.toString(16)})"
    }

    /**
     * Checks if protocol version matches expected
     */
    fun isProtocolVersionValid(): Boolean = protocolVersion == 0x01u.toUByte()
}

/**
 * Collection of all device heartbeats for monitoring entire system health
 */
data class SystemHeartbeats(
    val wiringHarness: HeartbeatMessage? = null,
    val motor1: HeartbeatMessage? = null,
    val motor2: HeartbeatMessage? = null,
    val display: HeartbeatMessage? = null,
    val bms: HeartbeatMessage? = null,
    val solarCharger: HeartbeatMessage? = null,
    val gps: HeartbeatMessage? = null,
    val batteryMonitor: HeartbeatMessage? = null
) {
    /**
     * Gets all available heartbeats
     */
    fun getAllHeartbeats(): List<HeartbeatMessage> {
        return listOfNotNull(
            wiringHarness, motor1, motor2, display,
            bms, solarCharger, gps, batteryMonitor
        )
    }

    /**
     * Checks if all critical devices are online
     */
    fun allCriticalDevicesOnline(maxAgeMs: Long = HeartbeatMessage.MAX_AGE_MS): Boolean {
        val critical = listOfNotNull(motor1, bms, batteryMonitor)
        return critical.isNotEmpty() && critical.all { !it.isStale(maxAgeMs) }
    }

    /**
     * Gets all devices with errors
     */
    fun getDevicesWithErrors(): List<HeartbeatMessage> {
        return getAllHeartbeats().filter { it.hasError() }
    }

    /**
     * Gets all devices with critical errors
     */
    fun getDevicesWithCriticalErrors(): List<HeartbeatMessage> {
        return getAllHeartbeats().filter { it.hasCriticalError() }
    }

    /**
     * Checks if system has any critical errors
     */
    fun hasAnyCriticalError(): Boolean {
        return getAllHeartbeats().any { it.hasCriticalError() }
    }

    /**
     * Gets all stale heartbeats
     */
    fun getStaleHeartbeats(maxAgeMs: Long = HeartbeatMessage.MAX_AGE_MS): List<HeartbeatMessage> {
        return getAllHeartbeats().filter { it.isStale(maxAgeMs) }
    }

    /**
     * Gets overall system health status
     */
    fun getSystemHealth(): SystemHealth {
        return when {
            hasAnyCriticalError() -> SystemHealth.CRITICAL
            getDevicesWithErrors().isNotEmpty() -> SystemHealth.ERROR
            getStaleHeartbeats().isNotEmpty() -> SystemHealth.WARNING
            getAllHeartbeats().all { it.isOk() } -> SystemHealth.OK
            else -> SystemHealth.UNKNOWN
        }
    }

    /**
     * Updates with a new heartbeat message
     */
    fun updateWith(heartbeat: HeartbeatMessage): SystemHeartbeats {
        return when (heartbeat.canId) {
            0x600 -> copy(wiringHarness = heartbeat)
            0x601 -> copy(motor1 = heartbeat)
            0x602 -> copy(motor2 = heartbeat)
            0x603 -> copy(display = heartbeat)
            0x604 -> copy(bms = heartbeat)
            0x605 -> copy(solarCharger = heartbeat)
            0x606 -> copy(gps = heartbeat)
            0x607 -> copy(batteryMonitor = heartbeat)
            else -> this
        }
    }
}

/**
 * Overall system health status
 */
enum class SystemHealth {
    OK,         // All systems normal
    WARNING,    // Some devices offline or warnings present
    ERROR,      // Errors present but not critical
    CRITICAL,   // Critical errors present
    UNKNOWN     // Insufficient data
}
