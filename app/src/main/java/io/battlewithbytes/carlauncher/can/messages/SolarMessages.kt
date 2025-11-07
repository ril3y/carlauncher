package io.battlewithbytes.carlauncher.can.messages

import io.battlewithbytes.carlauncher.can.*

/**
 * Solar Charge Controller Messages (0x630-0x637)
 * 8 message types for comprehensive solar charging monitoring
 */

/**
 * 0x630 - Solar Power Status (10Hz CRITICAL)
 */
data class SolarPowerStatusMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-1: Battery voltage (uint16 × 0.1V)
    val batteryVoltageVolts: Float,

    // Bytes 2-3: Charging current (uint16 × 0.1A)
    val chargingCurrentAmps: Float,

    // Byte 4: Solar panel voltage (uint8 × 0.5V, range 0-127.5V)
    val solarPanelVoltageVolts: Float,

    // Byte 5: Charging power (uint8 × 10W, range 0-2550W)
    val chargingPowerWatts: Int

    // Byte 6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x630
        const val UPDATE_RATE_MS = 100L // 10Hz
        const val MAX_AGE_MS = 300L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<SolarPowerStatusMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val batteryVoltage = data.getUInt16LE(0).toVoltage()
            val chargingCurrent = data.getUInt16LE(2).toCurrentUnsigned()
            val solarVoltage = data.getUInt8(4).toFloat() * 0.5f
            val power = data.getUInt8(5).toInt() * 10

            val message = SolarPowerStatusMessage(
                timestamp = timestamp,
                isValid = true,
                batteryVoltageVolts = batteryVoltage,
                chargingCurrentAmps = chargingCurrent,
                solarPanelVoltageVolts = solarVoltage,
                chargingPowerWatts = power
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if charger is actively charging
     */
    fun isCharging(): Boolean = chargingCurrentAmps > 0.0f && chargingPowerWatts > 0

    /**
     * Checks if solar panels have input
     */
    fun hasSolarInput(): Boolean = solarPanelVoltageVolts > 0.0f
}

/**
 * Solar charging mode enumeration
 */
enum class ChargingMode(val code: UByte) {
    OFF(0x00u),
    BULK(0x01u),
    ABSORPTION(0x02u),
    FLOAT(0x03u),
    EQUALIZATION(0x04u);

    companion object {
        fun fromCode(code: UByte): ChargingMode {
            return values().find { it.code == code } ?: OFF
        }
    }
}

/**
 * Load status enumeration
 */
enum class LoadStatus(val code: UByte) {
    OFF(0x00u),
    ON(0x01u),
    FAULT(0x02u);

    companion object {
        fun fromCode(code: UByte): LoadStatus {
            return values().find { it.code == code } ?: OFF
        }
    }
}

/**
 * Street light status enumeration
 */
enum class StreetLightStatus(val code: UByte) {
    OFF(0x00u),
    ON(0x01u),
    AUTO(0x02u);

    companion object {
        fun fromCode(code: UByte): StreetLightStatus {
            return values().find { it.code == code } ?: OFF
        }
    }
}

/**
 * Solar charging status flags (Byte 1 of 0x631)
 */
data class SolarStatusFlags(
    val chargingActive: Boolean,        // Bit 0
    val loadConnected: Boolean,         // Bit 1
    val nightMode: Boolean,             // Bit 2
    val batteryFull: Boolean,           // Bit 3
    val overTempProtection: Boolean,    // Bit 4
    val manualControlMode: Boolean      // Bit 5
    // Bits 6-7: Reserved
) {
    companion object {
        fun fromByte(byte: UByte): SolarStatusFlags {
            return SolarStatusFlags(
                chargingActive = byte.isBitSet(0),
                loadConnected = byte.isBitSet(1),
                nightMode = byte.isBitSet(2),
                batteryFull = byte.isBitSet(3),
                overTempProtection = byte.isBitSet(4),
                manualControlMode = byte.isBitSet(5)
            )
        }
    }
}

/**
 * 0x631 - Solar Charging Status (5Hz)
 */
data class SolarChargingStatusMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Byte 0: Battery SOC (0-100%)
    val batterySOCPercent: UByte,

    // Byte 1: Status flags
    val statusFlags: SolarStatusFlags,

    // Byte 2: Charging mode
    val chargingMode: ChargingMode,

    // Byte 3: Load status
    val loadStatus: LoadStatus,

    // Byte 4: Street light status
    val streetLightStatus: StreetLightStatus,

    // Byte 5: Brightness level (0-100%)
    val brightnessPercent: UByte

    // Byte 6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x631
        const val UPDATE_RATE_MS = 200L // 5Hz
        const val MAX_AGE_MS = 600L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<SolarChargingStatusMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val soc = data.getUInt8(0)
            val statusByte = data.getUInt8(1)
            val mode = ChargingMode.fromCode(data.getUInt8(2))
            val load = LoadStatus.fromCode(data.getUInt8(3))
            val streetLight = StreetLightStatus.fromCode(data.getUInt8(4))
            val brightness = data.getUInt8(5)

            val message = SolarChargingStatusMessage(
                timestamp = timestamp,
                isValid = true,
                batterySOCPercent = soc,
                statusFlags = SolarStatusFlags.fromByte(statusByte),
                chargingMode = mode,
                loadStatus = load,
                streetLightStatus = streetLight,
                brightnessPercent = brightness
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if actively charging
     */
    fun isCharging(): Boolean = statusFlags.chargingActive

    /**
     * Checks if battery is full
     */
    fun isBatteryFull(): Boolean = statusFlags.batteryFull

    /**
     * Checks if in night mode
     */
    fun isNightMode(): Boolean = statusFlags.nightMode
}

/**
 * 0x632 - Solar Temperature Status (1Hz)
 */
data class SolarTemperatureMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Byte 0: Controller temperature (int8_t °C, signed, no offset)
    val controllerTemperatureCelsius: Int?,

    // Byte 1: Battery temperature (int8_t °C, 0x7F=invalid)
    val batteryTemperatureCelsius: Int?,

    // Byte 2: Max temperature today (int8_t °C)
    val maxTempTodayCelsius: Int?,

    // Byte 3: Thermal derating (0-100%)
    val thermalDeratingPercent: UByte

    // Bytes 4-6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x632
        const val UPDATE_RATE_MS = 1000L // 1Hz
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<SolarTemperatureMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val controllerTemp = data.getInt8(0).toTemperatureNoOffset()
            val batteryTemp = data.getInt8(1).toTemperatureNoOffset()
            val maxTemp = data.getInt8(2).toTemperatureNoOffset()
            val derating = data.getUInt8(3)

            val message = SolarTemperatureMessage(
                timestamp = timestamp,
                isValid = true,
                controllerTemperatureCelsius = controllerTemp,
                batteryTemperatureCelsius = batteryTemp,
                maxTempTodayCelsius = maxTemp,
                thermalDeratingPercent = derating
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if controller is overheating
     */
    fun isOverheating(threshold: Int = 70): Boolean {
        return (controllerTemperatureCelsius ?: Int.MIN_VALUE) > threshold
    }

    /**
     * Checks if thermal derating is active
     */
    fun isDeratingActive(): Boolean = thermalDeratingPercent > 0u
}

/**
 * Load control mode enumeration
 */
enum class LoadControlMode(val code: UByte) {
    OFF(0x00u),
    ON(0x01u),
    AUTO(0x02u),
    MANUAL(0xFFu);

    companion object {
        fun fromCode(code: UByte): LoadControlMode {
            return values().find { it.code == code } ?: OFF
        }
    }
}

/**
 * 0x633 - Solar Load Status (1Hz)
 */
data class SolarLoadStatusMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-1: Load voltage (uint16 × 0.1V)
    val loadVoltageVolts: Float,

    // Byte 2: Load current (uint8 × 0.1A, range 0-25.5A)
    val loadCurrentAmps: Float,

    // Bytes 3-4: Load power (uint16 W)
    val loadPowerWatts: Int,

    // Byte 5: Load control mode
    val loadControl: LoadControlMode

    // Byte 6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x633
        const val UPDATE_RATE_MS = 1000L // 1Hz
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<SolarLoadStatusMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val voltage = data.getUInt16LE(0).toVoltage()
            val current = data.getUInt8(2).toFloat() * 0.1f
            val power = data.getUInt16LE(3).toInt()
            val control = LoadControlMode.fromCode(data.getUInt8(5))

            val message = SolarLoadStatusMessage(
                timestamp = timestamp,
                isValid = true,
                loadVoltageVolts = voltage,
                loadCurrentAmps = current,
                loadPowerWatts = power,
                loadControl = control
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if load is active
     */
    fun isLoadActive(): Boolean = loadCurrentAmps > 0.0f && loadPowerWatts > 0
}

/**
 * 0x634 - Solar Critical Alarms (Event + 1Hz)
 */
data class SolarCriticalAlarmsMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Byte 0: Voltage alarms (5 alarm bits)
    val voltageAlarms: UByte,

    // Byte 1: Current alarms (5 alarm bits)
    val currentAlarms: UByte,

    // Byte 2: Temperature alarms (5 alarm bits)
    val temperatureAlarms: UByte,

    // Byte 3: Controller alarms (8 alarm bits)
    val controllerAlarms: UByte,

    // Byte 4: Battery alarms (4 alarm bits)
    val batteryAlarms: UByte,

    // Byte 5: Alarm change counter
    val alarmChangeCounter: UByte

    // Byte 6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x634
        const val UPDATE_RATE_MS = 1000L // 1Hz (+ event-driven)
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<SolarCriticalAlarmsMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val message = SolarCriticalAlarmsMessage(
                timestamp = timestamp,
                isValid = true,
                voltageAlarms = data.getUInt8(0),
                currentAlarms = data.getUInt8(1),
                temperatureAlarms = data.getUInt8(2),
                controllerAlarms = data.getUInt8(3),
                batteryAlarms = data.getUInt8(4),
                alarmChangeCounter = data.getUInt8(5)
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if any critical alarms are active
     */
    fun hasAlarms(): Boolean {
        return voltageAlarms != 0.toUByte() ||
               currentAlarms != 0.toUByte() ||
               temperatureAlarms != 0.toUByte() ||
               controllerAlarms != 0.toUByte() ||
               batteryAlarms != 0.toUByte()
    }

    /**
     * Gets list of active error codes
     */
    fun getActiveErrors(): List<ErrorCode> {
        val errors = mutableListOf<ErrorCode>()

        if (voltageAlarms.isBitSet(0)) errors.add(ErrorCode.SOLAR_OVER_VOLTAGE)
        if (currentAlarms.isBitSet(0)) errors.add(ErrorCode.SOLAR_OVER_CURRENT)
        if (temperatureAlarms.isBitSet(0)) errors.add(ErrorCode.CHARGER_OVER_TEMP)
        if (controllerAlarms.isBitSet(0)) errors.add(ErrorCode.LOAD_SHORT_CIRCUIT)
        if (controllerAlarms.isBitSet(1)) errors.add(ErrorCode.PV_INPUT_SHORT)
        if (controllerAlarms.isBitSet(2)) errors.add(ErrorCode.CHARGE_MOS_FAULT)
        if (controllerAlarms.isBitSet(3)) errors.add(ErrorCode.LOAD_MOS_FAULT)

        return errors
    }
}

/**
 * 0x635 - Solar General Alarms (1Hz)
 */
data class SolarGeneralAlarmsMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Byte 0: System warnings (low solar, dust, night, maintenance, equalization)
    val systemWarnings: UByte,

    // Byte 1: Operation warnings (derating, limiting, load disable)
    val operationWarnings: UByte,

    // Byte 2: Configuration status (defaults, custom profile, street light, timer)
    val configurationStatus: UByte

    // Bytes 3-6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x635
        const val UPDATE_RATE_MS = 1000L // 1Hz
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<SolarGeneralAlarmsMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val message = SolarGeneralAlarmsMessage(
                timestamp = timestamp,
                isValid = true,
                systemWarnings = data.getUInt8(0),
                operationWarnings = data.getUInt8(1),
                configurationStatus = data.getUInt8(2)
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if any warnings are active
     */
    fun hasWarnings(): Boolean {
        return systemWarnings != 0.toUByte() || operationWarnings != 0.toUByte()
    }
}

/**
 * 0x636 - Solar Daily Statistics (0.1Hz = every 10 seconds)
 */
data class SolarDailyStatisticsMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-1: Energy generated (uint16 × 0.1kWh)
    val energyGeneratedKWh: Float,

    // Bytes 2-3: Energy consumed (uint16 × 0.1kWh)
    val energyConsumedKWh: Float,

    // Byte 4: Ah charged (0-255Ah)
    val ahCharged: UByte,

    // Byte 5: Ah discharged (0-255Ah)
    val ahDischarged: UByte

    // Byte 6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x636
        const val UPDATE_RATE_MS = 10000L // 0.1Hz
        const val MAX_AGE_MS = 30000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<SolarDailyStatisticsMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val generated = data.getUInt16LE(0).toEnergyKWhLowRes()
            val consumed = data.getUInt16LE(2).toEnergyKWhLowRes()
            val ahChg = data.getUInt8(4)
            val ahDis = data.getUInt8(5)

            val message = SolarDailyStatisticsMessage(
                timestamp = timestamp,
                isValid = true,
                energyGeneratedKWh = generated,
                energyConsumedKWh = consumed,
                ahCharged = ahChg,
                ahDischarged = ahDis
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Calculates net energy (generated - consumed)
     */
    fun getNetEnergyKWh(): Float = energyGeneratedKWh - energyConsumedKWh

    /**
     * Calculates round-trip efficiency
     */
    fun getRoundTripEfficiency(): Float? {
        return if (energyConsumedKWh > 0.0f) {
            (energyGeneratedKWh / energyConsumedKWh) * 100.0f
        } else {
            null
        }
    }
}

/**
 * 0x637 - Solar Historical Peak Values (0.1Hz = every 10 seconds)
 */
data class SolarHistoricalPeaksMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-1: Max charge power (uint16 × 10W)
    val maxChargePowerWatts: Int,

    // Byte 2: Max charge current (uint8 × 0.5A, range 0-127.5A)
    val maxChargeCurrentAmps: Float,

    // Byte 3: Max battery voltage (offset +40V, range 40.0-65.5V)
    val maxBatteryVoltageVolts: Float,

    // Byte 4: Min battery voltage (offset +40V)
    val minBatteryVoltageVolts: Float,

    // Byte 5: Total running days (lower 8 bits, wraps)
    val runningDays: UByte

    // Byte 6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x637
        const val UPDATE_RATE_MS = 10000L // 0.1Hz
        const val MAX_AGE_MS = 30000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<SolarHistoricalPeaksMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val maxPower = data.getUInt16LE(0).toInt() * 10
            val maxCurrent = data.getUInt8(2).toFloat() * 0.5f
            val maxVoltage = data.getUInt8(3).toFloat() + 40.0f
            val minVoltage = data.getUInt8(4).toFloat() + 40.0f
            val days = data.getUInt8(5)

            val message = SolarHistoricalPeaksMessage(
                timestamp = timestamp,
                isValid = true,
                maxChargePowerWatts = maxPower,
                maxChargeCurrentAmps = maxCurrent,
                maxBatteryVoltageVolts = maxVoltage,
                minBatteryVoltageVolts = minVoltage,
                runningDays = days
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Gets battery voltage range
     */
    fun getVoltageRange(): Float = maxBatteryVoltageVolts - minBatteryVoltageVolts
}

/**
 * Complete solar charger state aggregator
 */
data class SolarChargerState(
    val powerStatus: SolarPowerStatusMessage? = null,
    val chargingStatus: SolarChargingStatusMessage? = null,
    val temperature: SolarTemperatureMessage? = null,
    val loadStatus: SolarLoadStatusMessage? = null,
    val criticalAlarms: SolarCriticalAlarmsMessage? = null,
    val generalAlarms: SolarGeneralAlarmsMessage? = null,
    val dailyStats: SolarDailyStatisticsMessage? = null,
    val historicalPeaks: SolarHistoricalPeaksMessage? = null
) {
    /**
     * Checks if critical data is fresh
     */
    fun isCriticalDataFresh(maxAgeMs: Long = SolarPowerStatusMessage.MAX_AGE_MS): Boolean {
        return powerStatus?.isStale(maxAgeMs) == false
    }

    /**
     * Checks if charger has any critical alarms
     */
    fun hasCriticalAlarms(): Boolean {
        return criticalAlarms?.hasAlarms() == true
    }

    /**
     * Checks if charger is healthy
     */
    fun isHealthy(): Boolean {
        return isCriticalDataFresh() &&
               !hasCriticalAlarms() &&
               temperature?.isOverheating() == false
    }

    /**
     * Updates with a new solar message
     */
    fun updateWith(msg: CanMessage): SolarChargerState {
        return when (msg) {
            is SolarPowerStatusMessage -> copy(powerStatus = msg)
            is SolarChargingStatusMessage -> copy(chargingStatus = msg)
            is SolarTemperatureMessage -> copy(temperature = msg)
            is SolarLoadStatusMessage -> copy(loadStatus = msg)
            is SolarCriticalAlarmsMessage -> copy(criticalAlarms = msg)
            is SolarGeneralAlarmsMessage -> copy(generalAlarms = msg)
            is SolarDailyStatisticsMessage -> copy(dailyStats = msg)
            is SolarHistoricalPeaksMessage -> copy(historicalPeaks = msg)
            else -> this
        }
    }
}
