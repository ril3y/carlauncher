package io.battlewithbytes.carlauncher.can.messages

import io.battlewithbytes.carlauncher.can.*
import java.util.Date

/**
 * GPS and Time Synchronization Messages (0x640-0x649)
 * 10 message types for navigation and network time
 */

/**
 * Network time status flags (Byte 5 of 0x640)
 */
data class TimeStatusFlags(
    val gpsSynced: Boolean,         // Bit 0: GPS-locked (1) or RTC free-running (0)
    val timeValid: Boolean,         // Bit 1: Time set (1) or never synced (0)
    val gpsAvailable: Boolean,      // Bit 2: GPS module present
    val driftWarning: Boolean       // Bit 3: RTC drift detected
    // Bits 4-7: Reserved
) {
    companion object {
        fun fromByte(byte: UByte): TimeStatusFlags {
            return TimeStatusFlags(
                gpsSynced = byte.isBitSet(0),
                timeValid = byte.isBitSet(1),
                gpsAvailable = byte.isBitSet(2),
                driftWarning = byte.isBitSet(3)
            )
        }
    }
}

/**
 * 0x640 - Network Time Broadcast (1Hz CANONICAL)
 * This is the single source of truth for network time
 */
data class NetworkTimeMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-3: UNIX timestamp (uint32 little-endian, seconds since 1970)
    val unixTimestamp: UInt,

    // Byte 4: Subsecond (ms ÷ 4, range 0-1000ms)
    val subsecondMillis: Int,

    // Byte 5: Status flags
    val statusFlags: TimeStatusFlags,

    // Byte 6: Sequence counter (0-255 rolling)
    val sequenceCounter: UByte

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x640
        const val UPDATE_RATE_MS = 1000L // 1Hz
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<NetworkTimeMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val unixTime = data.getUInt32LE(0)
            val subsecond = data.getUInt8(4).toSubsecondMs()
            val statusByte = data.getUInt8(5)
            val sequence = data.getUInt8(6)

            val message = NetworkTimeMessage(
                timestamp = timestamp,
                isValid = true,
                unixTimestamp = unixTime,
                subsecondMillis = subsecond,
                statusFlags = TimeStatusFlags.fromByte(statusByte),
                sequenceCounter = sequence
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Gets the time as a Java Date object
     */
    fun getDate(): Date {
        return Date(unixTimestamp.toLong() * 1000L + subsecondMillis)
    }

    /**
     * Gets full timestamp in milliseconds
     */
    fun getTimestampMillis(): Long {
        return unixTimestamp.toLong() * 1000L + subsecondMillis
    }

    /**
     * Checks if time is GPS-synchronized
     */
    fun isGpsSynced(): Boolean = statusFlags.gpsSynced

    /**
     * Checks if time source is reliable
     */
    fun isReliable(): Boolean {
        return statusFlags.timeValid && !statusFlags.driftWarning
    }
}

/**
 * GPS fix type enumeration
 */
enum class GpsFixType(val code: Int) {
    NO_FIX(0),
    FIX_2D(1),
    FIX_3D(2),
    DGPS(3);

    companion object {
        fun fromCode(code: Int): GpsFixType {
            return values().find { it.code == code } ?: NO_FIX
        }
    }
}

/**
 * GPS status flags (Byte 5 of 0x641)
 */
data class GpsStatusFlags(
    val fixType: GpsFixType,            // Bits 0-1
    val timeValid: Boolean,             // Bit 2
    val dateValid: Boolean,             // Bit 3
    val leapSecondWarning: Boolean      // Bit 4
    // Bits 5-7: Reserved
) {
    companion object {
        fun fromByte(byte: UByte): GpsStatusFlags {
            val fixCode = byte.getBits(0, 2)
            return GpsStatusFlags(
                fixType = GpsFixType.fromCode(fixCode),
                timeValid = byte.isBitSet(2),
                dateValid = byte.isBitSet(3),
                leapSecondWarning = byte.isBitSet(4)
            )
        }
    }
}

/**
 * 0x641 - GPS Time Update (1Hz when locked)
 * Only transmitted when GPS has valid fix
 */
data class GpsTimeUpdateMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-3: UNIX timestamp (uint32 little-endian)
    val unixTimestamp: UInt,

    // Byte 4: Subsecond (ms ÷ 4)
    val subsecondMillis: Int,

    // Byte 5: GPS status
    val gpsStatus: GpsStatusFlags,

    // Byte 6: Satellite count (0-32)
    val satelliteCount: UByte

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x641
        const val UPDATE_RATE_MS = 1000L // 1Hz when locked
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<GpsTimeUpdateMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val unixTime = data.getUInt32LE(0)
            val subsecond = data.getUInt8(4).toSubsecondMs()
            val statusByte = data.getUInt8(5)
            val satCount = data.getUInt8(6)

            val message = GpsTimeUpdateMessage(
                timestamp = timestamp,
                isValid = true,
                unixTimestamp = unixTime,
                subsecondMillis = subsecond,
                gpsStatus = GpsStatusFlags.fromByte(statusByte),
                satelliteCount = satCount
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if GPS has valid fix
     */
    fun hasFix(): Boolean = gpsStatus.fixType != GpsFixType.NO_FIX

    /**
     * Checks if time is valid
     */
    fun isTimeValid(): Boolean = gpsStatus.timeValid && gpsStatus.dateValid
}

/**
 * Time authority level enumeration
 */
enum class TimeAuthority(val code: UByte) {
    MANUAL(0x00u),
    NETWORK(0x01u),
    GPS(0x02u);

    companion object {
        fun fromCode(code: UByte): TimeAuthority {
            return values().find { it.code == code } ?: MANUAL
        }
    }
}

/**
 * 0x642 - Time Set Command (On-demand)
 */
data class TimeSetCommandMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-3: UNIX timestamp (desired time)
    val unixTimestamp: UInt,

    // Byte 4: Subsecond (ms ÷ 4)
    val subsecondMillis: Int,

    // Byte 5: Authority level
    val authority: TimeAuthority,

    // Byte 6: Set flags (Bit 0: Force Set, Bit 1: Reset Drift Stats)
    val forceSet: Boolean,
    val resetDriftStats: Boolean

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x642

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<TimeSetCommandMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val unixTime = data.getUInt32LE(0)
            val subsecond = data.getUInt8(4).toSubsecondMs()
            val auth = TimeAuthority.fromCode(data.getUInt8(5))
            val flags = data.getUInt8(6)

            val message = TimeSetCommandMessage(
                timestamp = timestamp,
                isValid = true,
                unixTimestamp = unixTime,
                subsecondMillis = subsecond,
                authority = auth,
                forceSet = flags.isBitSet(0),
                resetDriftStats = flags.isBitSet(1)
            )

            return ParseResult.Success(message)
        }
    }
}

/**
 * Time sync state enumeration
 */
enum class TimeSyncState(val code: UByte) {
    UNINITIALIZED(0x00u),
    GPS_LOCKED(0x01u),
    GPS_RECENT(0x02u),
    RTC_FREE_RUN(0x03u),
    RTC_DRIFT_WARNING(0x04u),
    RTC_FAULT(0x05u);

    companion object {
        fun fromCode(code: UByte): TimeSyncState {
            return values().find { it.code == code } ?: UNINITIALIZED
        }
    }
}

/**
 * Last sync source enumeration
 */
enum class SyncSource(val code: UByte) {
    NEVER(0x00u),
    MANUAL(0x01u),
    GPS(0x02u),
    NETWORK(0x03u);

    companion object {
        fun fromCode(code: UByte): SyncSource {
            return values().find { it.code == code } ?: NEVER
        }
    }
}

/**
 * 0x643 - Time Sync Status (Event + 1Hz)
 */
data class TimeSyncStatusMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Byte 0: Sync state
    val syncState: TimeSyncState,

    // Byte 1: Last sync source
    val lastSyncSource: SyncSource,

    // Bytes 2-5: Seconds since last sync (uint32 little-endian)
    val secondsSinceLastSync: UInt,

    // Byte 6: RTC drift estimate (int8 × 10 PPM, range -128 to +127 → -12.8 to +12.7 PPM)
    val rtcDriftPpm: Float

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x643
        const val UPDATE_RATE_MS = 1000L // 1Hz (+ event-driven)
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<TimeSyncStatusMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val state = TimeSyncState.fromCode(data.getUInt8(0))
            val source = SyncSource.fromCode(data.getUInt8(1))
            val secondsSince = data.getUInt32LE(2)
            val drift = data.getInt8(6).toFloat() / 10.0f

            val message = TimeSyncStatusMessage(
                timestamp = timestamp,
                isValid = true,
                syncState = state,
                lastSyncSource = source,
                secondsSinceLastSync = secondsSince,
                rtcDriftPpm = drift
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if time is currently GPS-locked
     */
    fun isGpsLocked(): Boolean = syncState == TimeSyncState.GPS_LOCKED

    /**
     * Checks if RTC has concerning drift
     */
    fun hasDriftWarning(): Boolean = syncState == TimeSyncState.RTC_DRIFT_WARNING
}

/**
 * 0x644 - Time Query Request (On-demand)
 */
data class TimeQueryRequestMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Byte 0: Requesting node ID (heartbeat CAN ID)
    val requestingNodeId: UByte

    // Bytes 1-6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x644

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<TimeQueryRequestMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val message = TimeQueryRequestMessage(
                timestamp = timestamp,
                isValid = true,
                requestingNodeId = data.getUInt8(0)
            )

            return ParseResult.Success(message)
        }
    }
}

/**
 * 0x645 - GPS Position (Lat/Lon) (1Hz when locked)
 */
data class GpsPositionMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-3: Latitude (int32 × 1e-7 degrees, ±180°, ~11mm resolution)
    val latitudeDegrees: Double,

    // Bytes 4-6: Longitude (int24 × 1e-6 degrees, ±8.388607°, 3 bytes, ~111mm resolution)
    val longitudeDegrees: Double

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x645
        const val UPDATE_RATE_MS = 1000L // 1Hz when locked
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<GpsPositionMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val latRaw = data.getInt32LE(0)
            val lonRaw = data.getInt24LE(4)

            val message = GpsPositionMessage(
                timestamp = timestamp,
                isValid = true,
                latitudeDegrees = latRaw.toLatitude(),
                longitudeDegrees = lonRaw.toLongitude()
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Gets position as a formatted string
     */
    fun getPositionString(): String {
        val latDir = if (latitudeDegrees >= 0) "N" else "S"
        val lonDir = if (longitudeDegrees >= 0) "E" else "W"
        return "${kotlin.math.abs(latitudeDegrees)}° $latDir, ${kotlin.math.abs(longitudeDegrees)}° $lonDir"
    }
}

/**
 * 0x646 - GPS Velocity & Course (1Hz when locked)
 */
data class GpsVelocityMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-1: Ground speed (uint16 × 0.01 m/s)
    val groundSpeedMs: Float,

    // Bytes 2-3: Course over ground (uint16 × 0.01°, range 0-359.99°)
    val courseOverGroundDegrees: Float,

    // Byte 4: Vertical speed (int8 × 0.1 m/s, range -12.8 to +12.7 m/s)
    val verticalSpeedMs: Float,

    // Byte 5: Speed accuracy (uint8 × 0.1 m/s, range 0-25.5 m/s)
    val speedAccuracyMs: Float,

    // Byte 6: Course accuracy (uint8 degrees, range 0-180°)
    val courseAccuracyDegrees: UByte

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x646
        const val UPDATE_RATE_MS = 1000L // 1Hz when locked
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<GpsVelocityMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val speed = data.getUInt16LE(0).toSpeed()
            val course = data.getUInt16LE(2).toFloat() / 100.0f
            val vertSpeed = data.getInt8(4).toFloat() / 10.0f
            val speedAcc = data.getUInt8(5).toFloat() / 10.0f
            val courseAcc = data.getUInt8(6)

            val message = GpsVelocityMessage(
                timestamp = timestamp,
                isValid = true,
                groundSpeedMs = speed,
                courseOverGroundDegrees = course,
                verticalSpeedMs = vertSpeed,
                speedAccuracyMs = speedAcc,
                courseAccuracyDegrees = courseAcc
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Gets ground speed in km/h
     */
    fun getSpeedKmh(): Float = groundSpeedMs.toKmh()

    /**
     * Gets ground speed in mph
     */
    fun getSpeedMph(): Float = groundSpeedMs.toMph()

    /**
     * Checks if vehicle is moving
     */
    fun isMoving(threshold: Float = 0.5f): Boolean = groundSpeedMs > threshold
}

/**
 * 0x647 - GPS Altitude & Accuracy (1Hz when locked)
 */
data class GpsAltitudeMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-1: Altitude MSL (uint16 meters, offset -500, range -500 to +65035m)
    val altitudeMeters: Int,

    // Byte 2: Horizontal DOP (uint8 × 10, range 0-25.5)
    val horizontalDOP: Float,

    // Byte 3: Vertical DOP (uint8 × 10, range 0-25.5)
    val verticalDOP: Float,

    // Byte 4: Position DOP (uint8 × 10, range 0-25.5)
    val positionDOP: Float,

    // Byte 5: Horizontal accuracy (uint8 meters, 0-255m estimate)
    val horizontalAccuracyMeters: UByte,

    // Byte 6: Vertical accuracy (uint8 meters, 0-255m estimate)
    val verticalAccuracyMeters: UByte

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x647
        const val UPDATE_RATE_MS = 1000L // 1Hz when locked
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<GpsAltitudeMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val altRaw = data.getUInt16LE(0).toInt() - 500
            val hdop = data.getUInt8(2).toFloat() / 10.0f
            val vdop = data.getUInt8(3).toFloat() / 10.0f
            val pdop = data.getUInt8(4).toFloat() / 10.0f
            val hAcc = data.getUInt8(5)
            val vAcc = data.getUInt8(6)

            val message = GpsAltitudeMessage(
                timestamp = timestamp,
                isValid = true,
                altitudeMeters = altRaw,
                horizontalDOP = hdop,
                verticalDOP = vdop,
                positionDOP = pdop,
                horizontalAccuracyMeters = hAcc,
                verticalAccuracyMeters = vAcc
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks DOP quality (<2.0 = excellent, 2-5 = good, 5-10 = moderate, >10 = poor)
     */
    fun getDopQuality(): String {
        return when {
            positionDOP < 2.0f -> "Excellent"
            positionDOP < 5.0f -> "Good"
            positionDOP < 10.0f -> "Moderate"
            else -> "Poor"
        }
    }
}

/**
 * 0x648 - GPS Satellite Status (0.2Hz = every 5s)
 */
data class GpsSatelliteStatusMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Byte 0: Satellites used (0-32)
    val satellitesUsed: UByte,

    // Byte 1: Satellites visible (0-64)
    val satellitesVisible: UByte,

    // Byte 2: GPS constellation (0-32)
    val gpsCount: UByte,

    // Byte 3: GLONASS constellation (0-24)
    val glonassCount: UByte,

    // Byte 4: BeiDou constellation (0-35)
    val beidouCount: UByte,

    // Byte 5: Galileo constellation (0-24)
    val galileoCount: UByte,

    // Byte 6: Longitude reference (int8 degrees offset -128° to +127°)
    val longitudeReferenceOffset: Byte

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x648
        const val UPDATE_RATE_MS = 5000L // 0.2Hz (every 5s)
        const val MAX_AGE_MS = 15000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<GpsSatelliteStatusMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val message = GpsSatelliteStatusMessage(
                timestamp = timestamp,
                isValid = true,
                satellitesUsed = data.getUInt8(0),
                satellitesVisible = data.getUInt8(1),
                gpsCount = data.getUInt8(2),
                glonassCount = data.getUInt8(3),
                beidouCount = data.getUInt8(4),
                galileoCount = data.getUInt8(5),
                longitudeReferenceOffset = data.getInt8(6)
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Gets total constellation count
     */
    fun getTotalConstellations(): Int {
        return (gpsCount + glonassCount + beidouCount + galileoCount).toInt()
    }
}

/**
 * 0x649 - GPS Trip Statistics (0.1Hz = every 10s)
 */
data class GpsTripStatisticsMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-2: Trip distance (uint24 meters, 0-16,777,215m = 16,777 km)
    val tripDistanceMeters: Int,

    // Byte 3: Max speed recorded (uint8 × 0.5 m/s, range 0-127.5 m/s = 459 km/h)
    val maxSpeedMs: Float,

    // Byte 4: Average speed (uint8 × 0.5 m/s, range 0-127.5 m/s)
    val avgSpeedMs: Float,

    // Byte 5: Trip duration (uint8 minutes, 0-255, wraps at 4.25 hours)
    val tripDurationMinutes: UByte,

    // Byte 6: Motion time (uint8 minutes, time actually moving, speed >0.5 m/s)
    val motionTimeMinutes: UByte

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x649
        const val UPDATE_RATE_MS = 10000L // 0.1Hz (every 10s)
        const val MAX_AGE_MS = 30000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<GpsTripStatisticsMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val distance = data.getUInt24LE(0)
            val maxSpeed = data.getUInt8(3).toFloat() * 0.5f
            val avgSpeed = data.getUInt8(4).toFloat() * 0.5f
            val duration = data.getUInt8(5)
            val motion = data.getUInt8(6)

            val message = GpsTripStatisticsMessage(
                timestamp = timestamp,
                isValid = true,
                tripDistanceMeters = distance,
                maxSpeedMs = maxSpeed,
                avgSpeedMs = avgSpeed,
                tripDurationMinutes = duration,
                motionTimeMinutes = motion
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Gets trip distance in kilometers
     */
    fun getTripDistanceKm(): Float = tripDistanceMeters / 1000.0f

    /**
     * Gets max speed in km/h
     */
    fun getMaxSpeedKmh(): Float = maxSpeedMs.toKmh()

    /**
     * Gets average speed in km/h
     */
    fun getAvgSpeedKmh(): Float = avgSpeedMs.toKmh()

    /**
     * Calculates motion efficiency (motion time / total time)
     */
    fun getMotionEfficiency(): Float {
        return if (tripDurationMinutes.toInt() > 0) {
            (motionTimeMinutes.toFloat() / tripDurationMinutes.toFloat()) * 100.0f
        } else {
            0.0f
        }
    }
}

/**
 * Complete GPS/Time state aggregator
 */
data class GpsTimeState(
    val networkTime: NetworkTimeMessage? = null,
    val gpsTimeUpdate: GpsTimeUpdateMessage? = null,
    val timeSyncStatus: TimeSyncStatusMessage? = null,
    val position: GpsPositionMessage? = null,
    val velocity: GpsVelocityMessage? = null,
    val altitude: GpsAltitudeMessage? = null,
    val satelliteStatus: GpsSatelliteStatusMessage? = null,
    val tripStats: GpsTripStatisticsMessage? = null
) {
    /**
     * Checks if network time is fresh
     */
    fun isTimeDataFresh(maxAgeMs: Long = NetworkTimeMessage.MAX_AGE_MS): Boolean {
        return networkTime?.isStale(maxAgeMs) == false
    }

    /**
     * Checks if GPS has valid fix
     */
    fun hasGpsFix(): Boolean {
        return gpsTimeUpdate?.hasFix() == true
    }

    /**
     * Checks if position data is available
     */
    fun hasPosition(): Boolean {
        return position != null && !position.isStale(GpsPositionMessage.MAX_AGE_MS)
    }

    /**
     * Updates with a new GPS/Time message
     */
    fun updateWith(msg: CanMessage): GpsTimeState {
        return when (msg) {
            is NetworkTimeMessage -> copy(networkTime = msg)
            is GpsTimeUpdateMessage -> copy(gpsTimeUpdate = msg)
            is TimeSyncStatusMessage -> copy(timeSyncStatus = msg)
            is GpsPositionMessage -> copy(position = msg)
            is GpsVelocityMessage -> copy(velocity = msg)
            is GpsAltitudeMessage -> copy(altitude = msg)
            is GpsSatelliteStatusMessage -> copy(satelliteStatus = msg)
            is GpsTripStatisticsMessage -> copy(tripStats = msg)
            else -> this
        }
    }
}
