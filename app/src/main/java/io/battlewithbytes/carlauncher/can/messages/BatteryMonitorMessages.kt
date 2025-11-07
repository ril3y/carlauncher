package io.battlewithbytes.carlauncher.can.messages

import io.battlewithbytes.carlauncher.can.*

/**
 * Battery Monitor (Coulomb Counter) Messages (0x650-0x655)
 * 6 message types for authoritative battery monitoring
 *
 * IMPORTANT: Battery Monitor (0x650-0x655) is AUTHORITATIVE for:
 * - Pack Current (0x650)
 * - State of Charge (0x651) - THE definitive SOC source
 * - Energy Tracking (0x652)
 * - Network Time Authority (broadcasts 0x640)
 */

/**
 * 0x650 - Pack Current & Power (10Hz CRITICAL)
 * Primary current/power measurement - AUTHORITATIVE SOURCE
 */
data class BatteryCurrentPowerMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-1: Pack current (int16 × 0.1A, signed: +discharging, -charging)
    val packCurrentAmps: Float,     // Positive = discharging, Negative = charging

    // Bytes 2-3: Instantaneous power (int16 × 10W, signed)
    val instantaneousPowerWatts: Int,

    // Bytes 4-5: Pack voltage (uint16 × 0.1V, shows voltage sag)
    val packVoltageVolts: Float,

    // Byte 6: Load percentage (current ÷ 420A × 100, clamped 0-100)
    val loadPercentage: UByte

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x650
        const val UPDATE_RATE_MS = 100L // 10Hz
        const val MAX_AGE_MS = 300L // Stale after 300ms
        const val RATED_CURRENT_AMPS = 420.0f

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<BatteryCurrentPowerMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val current = data.getInt16LE(0).toCurrentSigned()
            val power = data.getInt16LE(2).toPowerSigned()
            val voltage = data.getUInt16LE(4).toVoltage()
            val loadPct = data.getUInt8(6)

            val message = BatteryCurrentPowerMessage(
                timestamp = timestamp,
                isValid = true,
                packCurrentAmps = current,
                instantaneousPowerWatts = power,
                packVoltageVolts = voltage,
                loadPercentage = loadPct
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if battery is discharging (positive current per spec)
     */
    fun isDischarging(): Boolean = packCurrentAmps > 0.0f

    /**
     * Checks if battery is charging (negative current per spec)
     */
    fun isCharging(): Boolean = packCurrentAmps < 0.0f

    /**
     * Gets absolute current value
     */
    fun getAbsoluteCurrentAmps(): Float = kotlin.math.abs(packCurrentAmps)

    /**
     * Checks if current exceeds safe threshold
     */
    fun isOvercurrent(threshold: Float = RATED_CURRENT_AMPS): Boolean {
        return getAbsoluteCurrentAmps() > threshold
    }

    /**
     * Gets absolute power value
     */
    fun getAbsolutePowerWatts(): Int = kotlin.math.abs(instantaneousPowerWatts)
}

/**
 * SOC sync flags (Byte 6 of 0x651)
 */
data class SocSyncFlags(
    val bmsFullChargeSeen: Boolean,     // Bit 0: BMS full charge seen <24hr
    val coulombCounterSynced: Boolean,  // Bit 1: Coulomb counter synced
    val socDrifting: Boolean,           // Bit 2: SOC drifting (>7 days since sync)
    val lowSocWarning: Boolean,         // Bit 3: Low SOC warning (SOC <20%)
    val criticalSocWarning: Boolean     // Bit 4: Critical SOC warning (SOC <10%)
    // Bits 5-7: Reserved
) {
    companion object {
        fun fromByte(byte: UByte): SocSyncFlags {
            return SocSyncFlags(
                bmsFullChargeSeen = byte.isBitSet(0),
                coulombCounterSynced = byte.isBitSet(1),
                socDrifting = byte.isBitSet(2),
                lowSocWarning = byte.isBitSet(3),
                criticalSocWarning = byte.isBitSet(4)
            )
        }
    }
}

/**
 * 0x651 - SOC & Capacity (1Hz AUTHORITATIVE)
 * CRITICAL: Byte 0 is THE AUTHORITATIVE SOC for the entire vehicle
 * BMS SOC (0x620) is informational only
 */
data class BatterySocCapacityMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Byte 0: State of Charge - AUTHORITATIVE SOC FOR VEHICLE
    val stateOfChargePercent: UByte,    // THE definitive SOC source

    // Bytes 1-2: Remaining capacity (uint16 × 0.1Ah)
    val remainingCapacityAh: Float,

    // Bytes 3-4: Full capacity (uint16 × 0.1Ah)
    val fullCapacityAh: Float,

    // Byte 5: SOC confidence (0-100%, 100%=synced <24hr, degrades 1%/day)
    val socConfidencePercent: UByte,

    // Byte 6: Sync flags
    val syncFlags: SocSyncFlags

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x651
        const val UPDATE_RATE_MS = 1000L // 1Hz
        const val MAX_AGE_MS = 3000L // Stale after 3 seconds

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<BatterySocCapacityMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val soc = data.getUInt8(0)
            val remaining = data.getUInt16LE(1).toCapacityAh()
            val full = data.getUInt16LE(3).toCapacityAh()
            val confidence = data.getUInt8(5)
            val syncByte = data.getUInt8(6)

            val message = BatterySocCapacityMessage(
                timestamp = timestamp,
                isValid = true,
                stateOfChargePercent = soc,
                remainingCapacityAh = remaining,
                fullCapacityAh = full,
                socConfidencePercent = confidence,
                syncFlags = SocSyncFlags.fromByte(syncByte)
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if SOC is reliable
     */
    fun isSocReliable(minConfidence: Int = 70): Boolean {
        return socConfidencePercent.toInt() >= minConfidence && !syncFlags.socDrifting
    }

    /**
     * Checks if SOC is critically low
     */
    fun isSocCritical(threshold: Int = 10): Boolean {
        return stateOfChargePercent.toInt() <= threshold || syncFlags.criticalSocWarning
    }

    /**
     * Checks if SOC is low
     */
    fun isSocLow(threshold: Int = 20): Boolean {
        return stateOfChargePercent.toInt() <= threshold || syncFlags.lowSocWarning
    }

    /**
     * Calculates usable capacity percentage
     */
    fun getUsableCapacityPercent(): Float {
        return if (fullCapacityAh > 0.0f) {
            (remainingCapacityAh / fullCapacityAh) * 100.0f
        } else {
            0.0f
        }
    }

    /**
     * Checks if recently synced with BMS full charge
     */
    fun isRecentlySynced(): Boolean = syncFlags.bmsFullChargeSeen
}

/**
 * 0x652 - Energy Statistics (1Hz)
 */
data class BatteryEnergyStatisticsMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-1: Energy discharged today (uint16 × 0.01kWh)
    val energyDischargedKWh: Float,

    // Bytes 2-3: Energy charged today (uint16 × 0.01kWh)
    val energyChargedKWh: Float,

    // Byte 4: Round-trip efficiency (%, typical 85-95%)
    val roundTripEfficiencyPercent: UByte,

    // Byte 5: Peak power today (uint8 × 0.1kW, range 0-25.5kW)
    val peakPowerKW: Float

    // Byte 6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x652
        const val UPDATE_RATE_MS = 1000L // 1Hz
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<BatteryEnergyStatisticsMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val discharged = data.getUInt16LE(0).toEnergyKWh()
            val charged = data.getUInt16LE(2).toEnergyKWh()
            val efficiency = data.getUInt8(4)
            val peakPower = data.getUInt8(5).toFloat() / 10.0f

            val message = BatteryEnergyStatisticsMessage(
                timestamp = timestamp,
                isValid = true,
                energyDischargedKWh = discharged,
                energyChargedKWh = charged,
                roundTripEfficiencyPercent = efficiency,
                peakPowerKW = peakPower
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Gets net energy (discharged - charged)
     */
    fun getNetEnergyKWh(): Float = energyDischargedKWh - energyChargedKWh

    /**
     * Checks if efficiency is within acceptable range
     */
    fun isEfficiencyOk(minEfficiency: Int = 80): Boolean {
        return roundTripEfficiencyPercent.toInt() >= minEfficiency
    }
}

/**
 * Warning flags for current limits (Byte 5 of 0x653)
 */
data class CurrentWarningFlags(
    val overcurrentWarning: Boolean,        // Bit 0: >450A
    val sustainedOvercurrent: Boolean,      // Bit 1: >400A for >10s
    val shuntOvertempWarning: Boolean,      // Bit 2: >70°C
    val shuntOvertempCritical: Boolean,     // Bit 3: >85°C
    val thermalDeratingActive: Boolean,     // Bit 4
    val shortCircuitDetected: Boolean       // Bit 5: >500A
    // Bits 6-7: Reserved
) {
    companion object {
        fun fromByte(byte: UByte): CurrentWarningFlags {
            return CurrentWarningFlags(
                overcurrentWarning = byte.isBitSet(0),
                sustainedOvercurrent = byte.isBitSet(1),
                shuntOvertempWarning = byte.isBitSet(2),
                shuntOvertempCritical = byte.isBitSet(3),
                thermalDeratingActive = byte.isBitSet(4),
                shortCircuitDetected = byte.isBitSet(5)
            )
        }
    }

    /**
     * Checks if any critical warning is active
     */
    fun hasCriticalWarning(): Boolean {
        return shuntOvertempCritical || shortCircuitDetected
    }

    /**
     * Checks if any warning is active
     */
    fun hasWarning(): Boolean {
        return overcurrentWarning || sustainedOvercurrent || shuntOvertempWarning ||
               shuntOvertempCritical || thermalDeratingActive || shortCircuitDetected
    }
}

/**
 * 0x653 - Current Limits & Warnings (1Hz)
 */
data class BatteryCurrentLimitsMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-1: Peak current recorded (uint16 × 0.1A, daily peak, resets midnight)
    val peakCurrentAmps: Float,

    // Byte 2: Current limit active (actual limit = value × 2A, 0=no limit)
    val currentLimitAmps: Int,

    // Byte 3: Shunt temperature (uint8 °C + 40, 0xFF=no sensor)
    val shuntTemperatureCelsius: Int?,

    // Byte 4: Thermal derating (0-100%, 0=no derating, 100=full cutoff)
    val thermalDeratingPercent: UByte,

    // Byte 5: Warning flags
    val warningFlags: CurrentWarningFlags

    // Byte 6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x653
        const val UPDATE_RATE_MS = 1000L // 1Hz
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<BatteryCurrentLimitsMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val peakCurrent = data.getUInt16LE(0).toCurrentUnsigned()
            val limitRaw = data.getUInt8(2)
            val limit = limitRaw.toInt() * 2
            val shuntTempRaw = data.getUInt8(3)
            val shuntTemp = if (shuntTempRaw == 0xFFu.toUByte()) null else shuntTempRaw.toTemperatureWithOffset()
            val derating = data.getUInt8(4)
            val warningByte = data.getUInt8(5)

            val message = BatteryCurrentLimitsMessage(
                timestamp = timestamp,
                isValid = true,
                peakCurrentAmps = peakCurrent,
                currentLimitAmps = limit,
                shuntTemperatureCelsius = shuntTemp,
                thermalDeratingPercent = derating,
                warningFlags = CurrentWarningFlags.fromByte(warningByte)
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if current limiting is active
     */
    fun isCurrentLimited(): Boolean = currentLimitAmps > 0

    /**
     * Checks if thermal derating is active
     */
    fun isThermalDeratingActive(): Boolean = thermalDeratingPercent > 0u

    /**
     * Checks if shunt temperature is within safe range
     */
    fun isShuntTemperatureOk(maxTemp: Int = 70): Boolean {
        return (shuntTemperatureCelsius ?: Int.MIN_VALUE) <= maxTemp
    }
}

/**
 * Calibration flags (Byte 6 of 0x654)
 */
data class CalibrationFlags(
    val factoryCalibrated: Boolean,     // Bit 0
    val fieldCalibrated: Boolean,       // Bit 1
    val autoZeroEnabled: Boolean,       // Bit 2
    val calibrationExpired: Boolean,    // Bit 3: >365 days
    val shuntDriftDetected: Boolean,    // Bit 4
    val adcSaturated: Boolean           // Bit 5: Clipping occurred
    // Bits 6-7: Reserved
) {
    companion object {
        fun fromByte(byte: UByte): CalibrationFlags {
            return CalibrationFlags(
                factoryCalibrated = byte.isBitSet(0),
                fieldCalibrated = byte.isBitSet(1),
                autoZeroEnabled = byte.isBitSet(2),
                calibrationExpired = byte.isBitSet(3),
                shuntDriftDetected = byte.isBitSet(4),
                adcSaturated = byte.isBitSet(5)
            )
        }
    }

    /**
     * Checks if calibration is valid
     */
    fun isCalibrationValid(): Boolean {
        return (factoryCalibrated || fieldCalibrated) &&
               !calibrationExpired &&
               !shuntDriftDetected
    }

    /**
     * Checks if recalibration is recommended
     */
    fun needsRecalibration(): Boolean {
        return calibrationExpired || shuntDriftDetected
    }
}

/**
 * 0x654 - Calibration Status (0.1Hz = every 10 seconds)
 */
data class BatteryCalibrationStatusMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-1: Shunt zero offset (int16 ADC counts, typical ±10)
    val shuntZeroOffset: Short,

    // Bytes 2-3: Calibration gain (uint16 × 0.001, typical 950-1050 → 0.95-1.05)
    val calibrationGain: Float,

    // Byte 4: Calibration age (days, 0-255 wraps, recommend annual recal)
    val calibrationAgeDays: UByte,

    // Byte 5: ADC noise level (LSB RMS, <5 excellent, 5-10 acceptable, >10 poor)
    val adcNoiseLevelLsb: UByte,

    // Byte 6: Calibration flags
    val calibrationFlags: CalibrationFlags

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x654
        const val UPDATE_RATE_MS = 10000L // 0.1Hz
        const val MAX_AGE_MS = 30000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<BatteryCalibrationStatusMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val offset = data.getInt16LE(0)
            val gainRaw = data.getUInt16LE(2)
            val gain = gainRaw.toFloat() / 1000.0f
            val age = data.getUInt8(4)
            val noise = data.getUInt8(5)
            val flagsByte = data.getUInt8(6)

            val message = BatteryCalibrationStatusMessage(
                timestamp = timestamp,
                isValid = true,
                shuntZeroOffset = offset,
                calibrationGain = gain,
                calibrationAgeDays = age,
                adcNoiseLevelLsb = noise,
                calibrationFlags = CalibrationFlags.fromByte(flagsByte)
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if ADC noise is acceptable
     */
    fun isNoiseAcceptable(maxNoise: Int = 10): Boolean {
        return adcNoiseLevelLsb.toInt() <= maxNoise
    }

    /**
     * Gets noise quality rating
     */
    fun getNoiseQuality(): String {
        return when (adcNoiseLevelLsb.toInt()) {
            in 0..4 -> "Excellent"
            in 5..10 -> "Acceptable"
            else -> "Poor"
        }
    }

    /**
     * Checks if calibration is current
     */
    fun isCalibrationCurrent(maxAgeDays: Int = 365): Boolean {
        return calibrationAgeDays.toInt() <= maxAgeDays && !calibrationFlags.calibrationExpired
    }
}

/**
 * Event trigger type for SOC checkpoints
 */
enum class SocCheckpointEvent(val code: UByte) {
    POWER_ON(0x01u),
    POWER_OFF(0x02u),
    SOC_MILESTONE(0x03u),
    FULL_CHARGE(0x04u),
    LOW_SOC(0x05u),
    CRITICAL_SOC(0x06u),
    HIGH_CURRENT(0x07u),
    PERIODIC(0x08u);      // Hourly

    companion object {
        fun fromCode(code: UByte): SocCheckpointEvent? {
            return values().find { it.code == code }
        }
    }
}

/**
 * 0x655 - SOC History Checkpoints (Event-Driven)
 * Stored in FRAM for persistence across power cycles
 */
data class BatterySocCheckpointMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Byte 0: SOC snapshot (0-100%)
    val socSnapshot: UByte,

    // Byte 1: Event trigger
    val eventTrigger: SocCheckpointEvent?,

    // Byte 2: Sequence counter (0-255 wraps)
    val sequenceCounter: UByte,

    // Bytes 3-4: Time since boot (uint16 seconds, 0-65535s = 18.2hrs)
    val timeSinceBootSeconds: Int,

    // Byte 5: GPS fix quality (HDOP × 10, 0xFF=no GPS)
    val gpsFixQuality: Float?,

    // Byte 6: Temperature (uint8 + 40°C)
    val temperatureCelsius: Int

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x655

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<BatterySocCheckpointMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val soc = data.getUInt8(0)
            val event = SocCheckpointEvent.fromCode(data.getUInt8(1))
            val sequence = data.getUInt8(2)
            val uptime = data.getUInt16LE(3).toInt()
            val gpsRaw = data.getUInt8(5)
            val gpsQuality = if (gpsRaw == 0xFFu.toUByte()) null else gpsRaw.toFloat() / 10.0f
            val temp = data.getUInt8(6).toTemperatureWithOffset()

            val message = BatterySocCheckpointMessage(
                timestamp = timestamp,
                isValid = true,
                socSnapshot = soc,
                eventTrigger = event,
                sequenceCounter = sequence,
                timeSinceBootSeconds = uptime,
                gpsFixQuality = gpsQuality,
                temperatureCelsius = temp
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if GPS data was available at checkpoint
     */
    fun hasGpsData(): Boolean = gpsFixQuality != null

    /**
     * Gets uptime as formatted string
     */
    fun getUptimeFormatted(): String {
        val hours = timeSinceBootSeconds / 3600
        val minutes = (timeSinceBootSeconds % 3600) / 60
        val seconds = timeSinceBootSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
}

/**
 * Complete battery monitor state aggregator
 */
data class BatteryMonitorState(
    val currentPower: BatteryCurrentPowerMessage? = null,
    val socCapacity: BatterySocCapacityMessage? = null,
    val energyStats: BatteryEnergyStatisticsMessage? = null,
    val currentLimits: BatteryCurrentLimitsMessage? = null,
    val calibrationStatus: BatteryCalibrationStatusMessage? = null,
    val lastCheckpoint: BatterySocCheckpointMessage? = null
) {
    /**
     * Checks if critical data is fresh
     */
    fun isCriticalDataFresh(maxAgeMs: Long = BatteryCurrentPowerMessage.MAX_AGE_MS): Boolean {
        return currentPower?.isStale(maxAgeMs) == false &&
               socCapacity?.isStale(BatterySocCapacityMessage.MAX_AGE_MS) == false
    }

    /**
     * Gets the authoritative SOC
     */
    fun getAuthoritativeSOC(): UByte? = socCapacity?.stateOfChargePercent

    /**
     * Checks if battery monitor is healthy
     */
    fun isHealthy(): Boolean {
        return isCriticalDataFresh() &&
               socCapacity?.isSocReliable() == true &&
               currentLimits?.warningFlags?.hasCriticalWarning() == false &&
               calibrationStatus?.calibrationFlags?.isCalibrationValid() == true
    }

    /**
     * Checks if any warnings are present
     */
    fun hasWarnings(): Boolean {
        return currentLimits?.warningFlags?.hasWarning() == true ||
               socCapacity?.isSocLow() == true ||
               calibrationStatus?.calibrationFlags?.needsRecalibration() == true
    }

    /**
     * Updates with a new battery monitor message
     */
    fun updateWith(msg: CanMessage): BatteryMonitorState {
        return when (msg) {
            is BatteryCurrentPowerMessage -> copy(currentPower = msg)
            is BatterySocCapacityMessage -> copy(socCapacity = msg)
            is BatteryEnergyStatisticsMessage -> copy(energyStats = msg)
            is BatteryCurrentLimitsMessage -> copy(currentLimits = msg)
            is BatteryCalibrationStatusMessage -> copy(calibrationStatus = msg)
            is BatterySocCheckpointMessage -> copy(lastCheckpoint = msg)
            else -> this
        }
    }
}
