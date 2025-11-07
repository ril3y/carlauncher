package io.battlewithbytes.carlauncher.can.messages

import io.battlewithbytes.carlauncher.can.*

/**
 * Battery Management System Messages (0x620-0x629)
 * 10 message types for comprehensive battery monitoring
 */

/**
 * 0x620 - BMS Pack Status (10Hz CRITICAL)
 * Primary battery pack telemetry
 */
data class BmsPackStatusMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-1: Pack voltage (uint16 × 0.1V, little-endian)
    val packVoltageVolts: Float,

    // Bytes 2-3: Pack current (int16 × 0.1A, signed: +charge, -discharge)
    val packCurrentAmps: Float,     // Positive = charging, Negative = discharging

    // Byte 4: State of charge (0-100%)
    val stateOfChargePercent: UByte

    // Bytes 5-6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x620
        const val UPDATE_RATE_MS = 100L // 10Hz
        const val MAX_AGE_MS = 300L // Stale after 300ms

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<BmsPackStatusMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val voltageRaw = data.getUInt16LE(0)
            val currentRaw = data.getInt16LE(2)
            val soc = data.getUInt8(4)

            val message = BmsPackStatusMessage(
                timestamp = timestamp,
                isValid = true,
                packVoltageVolts = voltageRaw.toVoltage(),
                packCurrentAmps = currentRaw.toCurrentSigned(),
                stateOfChargePercent = soc
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if battery is charging (current positive per spec: +charge, -discharge)
     */
    fun isCharging(): Boolean = packCurrentAmps > 0.0f

    /**
     * Checks if battery is discharging
     */
    fun isDischarging(): Boolean = packCurrentAmps < 0.0f

    /**
     * Calculates instantaneous power in watts
     */
    fun getPowerWatts(): Float = packVoltageVolts * kotlin.math.abs(packCurrentAmps)

    /**
     * Checks if SOC is critically low
     */
    fun isSocCritical(threshold: Int = 10): Boolean = stateOfChargePercent.toInt() < threshold

    /**
     * Checks if SOC is low
     */
    fun isSocLow(threshold: Int = 20): Boolean = stateOfChargePercent.toInt() < threshold
}

/**
 * 0x621 - BMS Cell Summary (1Hz)
 * Cell voltage statistics
 */
data class BmsCellSummaryMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Bytes 0-1: Max cell voltage (uint16 mV)
    val maxCellVoltageMillivolts: Int,

    // Bytes 2-3: Min cell voltage (uint16 mV)
    val minCellVoltageMillivolts: Int,

    // Bytes 4-5: Cell voltage difference (uint16 mV)
    val cellVoltageDifferenceMillivolts: Int

    // Byte 6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x621
        const val UPDATE_RATE_MS = 1000L // 1Hz
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<BmsCellSummaryMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val maxCell = data.getUInt16LE(0).toMillivolts()
            val minCell = data.getUInt16LE(2).toMillivolts()
            val cellDiff = data.getUInt16LE(4).toMillivolts()

            val message = BmsCellSummaryMessage(
                timestamp = timestamp,
                isValid = true,
                maxCellVoltageMillivolts = maxCell,
                minCellVoltageMillivolts = minCell,
                cellVoltageDifferenceMillivolts = cellDiff
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Gets max cell voltage in volts
     */
    fun getMaxCellVoltage(): Float = maxCellVoltageMillivolts / 1000.0f

    /**
     * Gets min cell voltage in volts
     */
    fun getMinCellVoltage(): Float = minCellVoltageMillivolts / 1000.0f

    /**
     * Gets voltage difference in volts
     */
    fun getVoltageDifference(): Float = cellVoltageDifferenceMillivolts / 1000.0f

    /**
     * Checks if cells are balanced (difference within threshold)
     */
    fun areCellsBalanced(maxDifferenceMillivolts: Int = 50): Boolean {
        return cellVoltageDifferenceMillivolts <= maxDifferenceMillivolts
    }

    /**
     * Checks if any cell voltage is concerning
     */
    fun hasCellVoltageIssue(minSafe: Int = 3000, maxSafe: Int = 4200): Boolean {
        return minCellVoltageMillivolts < minSafe || maxCellVoltageMillivolts > maxSafe
    }
}

/**
 * 0x622 - BMS Temperature Status (1Hz)
 */
data class BmsTemperatureMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Byte 0: Max temperature (int8_t °C, signed, no offset)
    val maxTemperatureCelsius: Int?,

    // Byte 1: Min temperature (int8_t °C, signed, no offset)
    val minTemperatureCelsius: Int?,

    // Byte 2: Average temperature (int8_t °C, signed, no offset)
    val avgTemperatureCelsius: Int?,

    // Byte 3: Number of sensors (1-16)
    val sensorCount: UByte

    // Bytes 4-6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x622
        const val UPDATE_RATE_MS = 1000L // 1Hz
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<BmsTemperatureMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val maxTemp = data.getInt8(0).toTemperatureNoOffset()
            val minTemp = data.getInt8(1).toTemperatureNoOffset()
            val avgTemp = data.getInt8(2).toTemperatureNoOffset()
            val sensors = data.getUInt8(3)

            val message = BmsTemperatureMessage(
                timestamp = timestamp,
                isValid = true,
                maxTemperatureCelsius = maxTemp,
                minTemperatureCelsius = minTemp,
                avgTemperatureCelsius = avgTemp,
                sensorCount = sensors
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if temperatures are within safe range
     */
    fun isTemperatureOk(minSafe: Int = -10, maxSafe: Int = 45): Boolean {
        val max = maxTemperatureCelsius ?: return false
        val min = minTemperatureCelsius ?: return false
        return max <= maxSafe && min >= minSafe
    }

    /**
     * Checks if battery is overheating
     */
    fun isOverheating(threshold: Int = 45): Boolean {
        return (maxTemperatureCelsius ?: Int.MIN_VALUE) > threshold
    }

    /**
     * Checks if battery is too cold
     */
    fun isTooCold(threshold: Int = -10): Boolean {
        return (minTemperatureCelsius ?: Int.MAX_VALUE) < threshold
    }

    /**
     * Gets temperature spread in Celsius
     */
    fun getTemperatureSpread(): Int? {
        val max = maxTemperatureCelsius ?: return null
        val min = minTemperatureCelsius ?: return null
        return max - min
    }
}

/**
 * BMS Power Status Flags (Byte 0 of 0x623)
 */
data class BmsPowerStatusFlags(
    val chargeMosfetEnabled: Boolean,       // Bit 0
    val dischargeMosfetEnabled: Boolean,    // Bit 1
    val chargerConnected: Boolean,          // Bit 2
    val loadConnected: Boolean,             // Bit 3
    val cellBalanceActive: Boolean,         // Bit 4
    val state: BmsOperationalState          // Bits 5-6
    // Bit 7: Reserved
) {
    companion object {
        fun fromByte(byte: UByte): BmsPowerStatusFlags {
            val stateBits = byte.getBits(5, 2)
            val state = when (stateBits) {
                0 -> BmsOperationalState.IDLE
                1 -> BmsOperationalState.CHARGING
                2 -> BmsOperationalState.DISCHARGING
                else -> BmsOperationalState.IDLE
            }

            return BmsPowerStatusFlags(
                chargeMosfetEnabled = byte.isBitSet(0),
                dischargeMosfetEnabled = byte.isBitSet(1),
                chargerConnected = byte.isBitSet(2),
                loadConnected = byte.isBitSet(3),
                cellBalanceActive = byte.isBitSet(4),
                state = state
            )
        }
    }
}

/**
 * BMS operational state
 */
enum class BmsOperationalState {
    IDLE,           // 00
    CHARGING,       // 01
    DISCHARGING     // 10
}

/**
 * 0x623 - BMS Power Status (1Hz)
 */
data class BmsPowerStatusMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Byte 0: Status flags
    val statusFlags: BmsPowerStatusFlags,

    // Bytes 1-2: Residual capacity (uint16 × 0.1Ah)
    val residualCapacityAh: Float,

    // Byte 3: Number of cells (1-48)
    val cellCount: UByte,

    // Byte 4: Cells currently balancing (0-48)
    val cellsBalancing: UByte,

    // Byte 5: Charge/discharge cycles (lower 8 bits, wraps)
    val cycleCount: UByte

    // Byte 6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x623
        const val UPDATE_RATE_MS = 1000L // 1Hz
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<BmsPowerStatusMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val statusByte = data.getUInt8(0)
            val capacityRaw = data.getUInt16LE(1)
            val cells = data.getUInt8(3)
            val balancing = data.getUInt8(4)
            val cycles = data.getUInt8(5)

            val message = BmsPowerStatusMessage(
                timestamp = timestamp,
                isValid = true,
                statusFlags = BmsPowerStatusFlags.fromByte(statusByte),
                residualCapacityAh = capacityRaw.toCapacityAh(),
                cellCount = cells,
                cellsBalancing = balancing,
                cycleCount = cycles
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if charging is possible
     */
    fun canCharge(): Boolean = statusFlags.chargeMosfetEnabled && statusFlags.chargerConnected

    /**
     * Checks if discharging is possible
     */
    fun canDischarge(): Boolean = statusFlags.dischargeMosfetEnabled && statusFlags.loadConnected

    /**
     * Checks if balancing is active
     */
    fun isBalancing(): Boolean = statusFlags.cellBalanceActive

    /**
     * Gets current operational state
     */
    fun getState(): BmsOperationalState = statusFlags.state
}

/**
 * 0x624 - BMS Critical Alarms (Event + 1Hz)
 */
data class BmsCriticalAlarmsMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Byte 0: Voltage alarms (8 alarm bits)
    val voltageAlarms: UByte,

    // Byte 1: Current/SOC alarms (8 alarm bits)
    val currentSocAlarms: UByte,

    // Byte 2: Temperature alarms (8 alarm bits)
    val temperatureAlarms: UByte,

    // Byte 3: System alarms (cell/temp difference)
    val systemAlarms: UByte,

    // Byte 4: MOSFET failures
    val mosfetFailures: UByte,

    // Byte 5: Alarm change counter
    val alarmChangeCounter: UByte

    // Byte 6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x624
        const val UPDATE_RATE_MS = 1000L // 1Hz (+ event-driven)
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<BmsCriticalAlarmsMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val message = BmsCriticalAlarmsMessage(
                timestamp = timestamp,
                isValid = true,
                voltageAlarms = data.getUInt8(0),
                currentSocAlarms = data.getUInt8(1),
                temperatureAlarms = data.getUInt8(2),
                systemAlarms = data.getUInt8(3),
                mosfetFailures = data.getUInt8(4),
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
               currentSocAlarms != 0.toUByte() ||
               temperatureAlarms != 0.toUByte() ||
               systemAlarms != 0.toUByte() ||
               mosfetFailures != 0.toUByte()
    }

    /**
     * Gets list of active error codes
     */
    fun getActiveErrors(): List<ErrorCode> {
        val errors = mutableListOf<ErrorCode>()

        // Voltage alarms
        if (voltageAlarms.isBitSet(0)) errors.add(ErrorCode.BATTERY_OVER_VOLTAGE)
        if (voltageAlarms.isBitSet(1)) errors.add(ErrorCode.BATTERY_UNDER_VOLTAGE)

        // Current/SOC alarms
        if (currentSocAlarms.isBitSet(0)) errors.add(ErrorCode.BATTERY_OVER_CURRENT)
        if (currentSocAlarms.isBitSet(1)) errors.add(ErrorCode.BATTERY_SOC_LOW)

        // Temperature alarms
        if (temperatureAlarms.isBitSet(0)) errors.add(ErrorCode.BATTERY_OVER_TEMP)
        if (temperatureAlarms.isBitSet(1)) errors.add(ErrorCode.BATTERY_UNDER_TEMP)

        // System alarms
        if (systemAlarms.isBitSet(0)) errors.add(ErrorCode.CELL_IMBALANCE)

        return errors
    }
}

/**
 * 0x625 - BMS General Alarms (1Hz)
 */
data class BmsGeneralAlarmsMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,

    // Byte 0: Temperature sensor failures (bitfield)
    val tempSensorFailures: UByte,

    // Byte 1: Module failures (bitfield)
    val moduleFailures: UByte,

    // Byte 2: Communication failures (bitfield)
    val commFailures: UByte,

    // Byte 3: Protection failures (bitfield)
    val protectionFailures: UByte

    // Bytes 4-6: Reserved (0x00)

) : CanMessage {
    override val canId: Int = CAN_ID

    companion object {
        const val CAN_ID = 0x625
        const val UPDATE_RATE_MS = 1000L // 1Hz
        const val MAX_AGE_MS = 3000L

        fun parse(data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<BmsGeneralAlarmsMessage> {
            if (data.size != 8) {
                return ParseResult.InvalidData(CAN_ID, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(CAN_ID, data)
            }

            val message = BmsGeneralAlarmsMessage(
                timestamp = timestamp,
                isValid = true,
                tempSensorFailures = data.getUInt8(0),
                moduleFailures = data.getUInt8(1),
                commFailures = data.getUInt8(2),
                protectionFailures = data.getUInt8(3)
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Checks if any general alarms are active
     */
    fun hasAlarms(): Boolean {
        return tempSensorFailures != 0.toUByte() ||
               moduleFailures != 0.toUByte() ||
               commFailures != 0.toUByte() ||
               protectionFailures != 0.toUByte()
    }
}

/**
 * 0x626-0x629 - BMS Cell Voltage Banks (0.5Hz rotating)
 * Each message contains 2 cell voltages
 */
data class BmsCellVoltageBankMessage(
    override val timestamp: Long = System.currentTimeMillis(),
    override val isValid: Boolean,
    override val canId: Int,

    // Byte 0: Bank index (0-3 identifies which pair)
    val bankIndex: UByte,

    // Bytes 1-2: Cell 1 voltage (uint16 mV)
    val cell1VoltageMillivolts: Int,

    // Bytes 3-4: Cell 2 voltage (uint16 mV)
    val cell2VoltageMillivolts: Int

    // Bytes 5-6: Reserved (0x00)

) : CanMessage {

    companion object {
        const val BANK_0_CAN_ID = 0x626
        const val BANK_1_CAN_ID = 0x627
        const val BANK_2_CAN_ID = 0x628
        const val BANK_3_CAN_ID = 0x629
        const val UPDATE_RATE_MS = 2000L // 0.5Hz
        const val MAX_AGE_MS = 6000L

        fun parse(canId: Int, data: ByteArray, timestamp: Long = System.currentTimeMillis()): ParseResult<BmsCellVoltageBankMessage> {
            if (canId !in BANK_0_CAN_ID..BANK_3_CAN_ID) {
                return ParseResult.InvalidData(canId, "Invalid cell bank CAN ID: 0x${canId.toString(16)}")
            }

            if (data.size != 8) {
                return ParseResult.InvalidData(canId, "Invalid frame size: ${data.size}")
            }

            if (!data.validateCrc8()) {
                return ParseResult.InvalidCrc(canId, data)
            }

            val bankIdx = data.getUInt8(0)
            val cell1 = data.getUInt16LE(1).toMillivolts()
            val cell2 = data.getUInt16LE(3).toMillivolts()

            val message = BmsCellVoltageBankMessage(
                timestamp = timestamp,
                isValid = true,
                canId = canId,
                bankIndex = bankIdx,
                cell1VoltageMillivolts = cell1,
                cell2VoltageMillivolts = cell2
            )

            return ParseResult.Success(message)
        }
    }

    /**
     * Gets cell 1 voltage in volts
     */
    fun getCell1Voltage(): Float = cell1VoltageMillivolts / 1000.0f

    /**
     * Gets cell 2 voltage in volts
     */
    fun getCell2Voltage(): Float = cell2VoltageMillivolts / 1000.0f

    /**
     * Gets the absolute cell indices based on bank
     * Returns pair of (cell1Index, cell2Index)
     */
    fun getCellIndices(): Pair<Int, Int> {
        val baseIndex = bankIndex.toInt() * 2
        return Pair(baseIndex, baseIndex + 1)
    }
}

/**
 * Complete BMS state aggregator
 */
data class BmsState(
    val packStatus: BmsPackStatusMessage? = null,
    val cellSummary: BmsCellSummaryMessage? = null,
    val temperature: BmsTemperatureMessage? = null,
    val powerStatus: BmsPowerStatusMessage? = null,
    val criticalAlarms: BmsCriticalAlarmsMessage? = null,
    val generalAlarms: BmsGeneralAlarmsMessage? = null,
    val cellBanks: Map<Int, BmsCellVoltageBankMessage> = emptyMap()
) {
    /**
     * Checks if critical data is fresh
     */
    fun isCriticalDataFresh(maxAgeMs: Long = BmsPackStatusMessage.MAX_AGE_MS): Boolean {
        return packStatus?.isStale(maxAgeMs) == false
    }

    /**
     * Checks if BMS has any critical alarms
     */
    fun hasCriticalAlarms(): Boolean {
        return criticalAlarms?.hasAlarms() == true
    }

    /**
     * Checks if BMS has any general alarms
     */
    fun hasGeneralAlarms(): Boolean {
        return generalAlarms?.hasAlarms() == true
    }

    /**
     * Checks if BMS is healthy
     */
    fun isHealthy(): Boolean {
        return isCriticalDataFresh() &&
               !hasCriticalAlarms() &&
               temperature?.isTemperatureOk() == true &&
               cellSummary?.areCellsBalanced() == true
    }

    /**
     * Gets all cell voltages from banks
     */
    fun getAllCellVoltages(): List<Float> {
        return cellBanks.values.sortedBy { it.bankIndex }.flatMap {
            listOf(it.getCell1Voltage(), it.getCell2Voltage())
        }
    }

    /**
     * Updates with a new BMS message
     */
    fun updateWith(msg: CanMessage): BmsState {
        return when (msg) {
            is BmsPackStatusMessage -> copy(packStatus = msg)
            is BmsCellSummaryMessage -> copy(cellSummary = msg)
            is BmsTemperatureMessage -> copy(temperature = msg)
            is BmsPowerStatusMessage -> copy(powerStatus = msg)
            is BmsCriticalAlarmsMessage -> copy(criticalAlarms = msg)
            is BmsGeneralAlarmsMessage -> copy(generalAlarms = msg)
            is BmsCellVoltageBankMessage -> {
                val updatedBanks = cellBanks.toMutableMap()
                updatedBanks[msg.canId] = msg
                copy(cellBanks = updatedBanks)
            }
            else -> this
        }
    }
}
