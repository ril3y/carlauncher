package io.battlewithbytes.carlauncher.can.state

import io.battlewithbytes.carlauncher.can.protocol.CanMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Central state manager for all vehicle CAN data
 * Provides reactive StateFlows for UI consumption
 * Handles staleness detection and cross-validation
 */
class VehicleStateManager {

    // === CRITICAL 10Hz STREAMS ===
    private val _bmsPackStatus = MutableStateFlow<TimedData<CanMessage.BmsPackStatus>?>(null)
    val bmsPackStatus: StateFlow<TimedData<CanMessage.BmsPackStatus>?> = _bmsPackStatus.asStateFlow()

    private val _solarPowerStatus = MutableStateFlow<TimedData<CanMessage.SolarPowerStatus>?>(null)
    val solarPowerStatus: StateFlow<TimedData<CanMessage.SolarPowerStatus>?> = _solarPowerStatus.asStateFlow()

    private val _packCurrentPower = MutableStateFlow<TimedData<CanMessage.PackCurrentPower>?>(null)
    val packCurrentPower: StateFlow<TimedData<CanMessage.PackCurrentPower>?> = _packCurrentPower.asStateFlow()

    private val _motor1Basic = MutableStateFlow<TimedData<CanMessage.MotorBasic>?>(null)
    val motor1Basic: StateFlow<TimedData<CanMessage.MotorBasic>?> = _motor1Basic.asStateFlow()

    private val _motor2Basic = MutableStateFlow<TimedData<CanMessage.MotorBasic>?>(null)
    val motor2Basic: StateFlow<TimedData<CanMessage.MotorBasic>?> = _motor2Basic.asStateFlow()

    // === AUTHORITY STREAMS (1Hz) ===
    private val _socCapacity = MutableStateFlow<TimedData<CanMessage.SocCapacity>?>(null)
    val socCapacity: StateFlow<TimedData<CanMessage.SocCapacity>?> = _socCapacity.asStateFlow()

    // === GPS STREAMS (1Hz when locked) ===
    private val _gpsPosition = MutableStateFlow<TimedData<CanMessage.GpsPosition>?>(null)
    val gpsPosition: StateFlow<TimedData<CanMessage.GpsPosition>?> = _gpsPosition.asStateFlow()

    private val _gpsVelocity = MutableStateFlow<TimedData<CanMessage.GpsVelocity>?>(null)
    val gpsVelocity: StateFlow<TimedData<CanMessage.GpsVelocity>?> = _gpsVelocity.asStateFlow()

    // === EVENT-DRIVEN STREAMS ===
    private val _switchState = MutableStateFlow<TimedData<CanMessage.SwitchState>?>(null)
    val switchState: StateFlow<TimedData<CanMessage.SwitchState>?> = _switchState.asStateFlow()

    // === HEARTBEATS (1Hz) ===
    private val _heartbeats = MutableStateFlow<Map<Int, TimedData<CanMessage.Heartbeat>>>(emptyMap())
    val heartbeats: StateFlow<Map<Int, TimedData<CanMessage.Heartbeat>>> = _heartbeats.asStateFlow()

    // === MOTOR STREAMS ===
    private val _motor1Status = MutableStateFlow<TimedData<CanMessage.MotorStatus>?>(null)
    val motor1Status: StateFlow<TimedData<CanMessage.MotorStatus>?> = _motor1Status.asStateFlow()

    private val _motor2Status = MutableStateFlow<TimedData<CanMessage.MotorStatus>?>(null)
    val motor2Status: StateFlow<TimedData<CanMessage.MotorStatus>?> = _motor2Status.asStateFlow()

    // === ALARMS ===
    private val _bmsCriticalAlarms = MutableStateFlow<TimedData<CanMessage.BmsCriticalAlarms>?>(null)
    val bmsCriticalAlarms: StateFlow<TimedData<CanMessage.BmsCriticalAlarms>?> = _bmsCriticalAlarms.asStateFlow()

    // === VALIDATION STATE ===
    private val _validationWarnings = MutableStateFlow<List<ValidationWarning>>(emptyList())
    val validationWarnings: StateFlow<List<ValidationWarning>> = _validationWarnings.asStateFlow()

    /**
     * Update state with new message
     * Routes to appropriate StateFlow based on message type
     */
    fun updateMessage(message: CanMessage) {
        val timedData = TimedData(message, System.currentTimeMillis())

        when (message) {
            is CanMessage.BmsPackStatus -> _bmsPackStatus.value = timedData as TimedData<CanMessage.BmsPackStatus>
            is CanMessage.SolarPowerStatus -> _solarPowerStatus.value = timedData as TimedData<CanMessage.SolarPowerStatus>
            is CanMessage.PackCurrentPower -> {
                _packCurrentPower.value = timedData as TimedData<CanMessage.PackCurrentPower>
                validateVoltageConsistency()
            }
            is CanMessage.SocCapacity -> _socCapacity.value = timedData as TimedData<CanMessage.SocCapacity>
            is CanMessage.GpsPosition -> _gpsPosition.value = timedData as TimedData<CanMessage.GpsPosition>
            is CanMessage.GpsVelocity -> _gpsVelocity.value = timedData as TimedData<CanMessage.GpsVelocity>
            is CanMessage.SwitchState -> _switchState.value = timedData as TimedData<CanMessage.SwitchState>
            is CanMessage.Heartbeat -> {
                val current = _heartbeats.value.toMutableMap()
                current[message.id] = timedData as TimedData<CanMessage.Heartbeat>
                _heartbeats.value = current
            }
            is CanMessage.MotorBasic -> {
                if (message.motorNumber == 1) {
                    _motor1Basic.value = timedData as TimedData<CanMessage.MotorBasic>
                } else {
                    _motor2Basic.value = timedData as TimedData<CanMessage.MotorBasic>
                }
            }
            is CanMessage.MotorStatus -> {
                if (message.motorNumber == 1) {
                    _motor1Status.value = timedData as TimedData<CanMessage.MotorStatus>
                } else {
                    _motor2Status.value = timedData as TimedData<CanMessage.MotorStatus>
                }
            }
            is CanMessage.BmsCriticalAlarms -> _bmsCriticalAlarms.value = timedData as TimedData<CanMessage.BmsCriticalAlarms>
            is CanMessage.Unknown -> {
                // Log but don't store
            }
        }
    }

    /**
     * Cross-validate voltage readings from multiple sources
     * Protocol requires: BMS voltage (0x620) vs Monitor voltage (0x650) < 2V difference
     */
    private fun validateVoltageConsistency() {
        val bmsVoltage = _bmsPackStatus.value?.data?.packVoltageV
        val monitorVoltage = _packCurrentPower.value?.data?.packVoltageV

        if (bmsVoltage != null && monitorVoltage != null) {
            val diff = kotlin.math.abs(bmsVoltage - monitorVoltage)
            if (diff > 2.0f) {
                addValidationWarning(
                    ValidationWarning(
                        severity = ValidationSeverity.ERROR,
                        category = "VOLTAGE_SENSOR_FAULT",
                        message = "Voltage mismatch: BMS=${bmsVoltage}V, Monitor=${monitorVoltage}V (diff=${diff}V)"
                    )
                )
            }
        }
    }

    private fun addValidationWarning(warning: ValidationWarning) {
        val current = _validationWarnings.value.toMutableList()
        if (current.size >= 10) {
            current.removeAt(0)
        }
        current.add(warning)
        _validationWarnings.value = current
    }

    /**
     * Check if message stream is stale based on expected update rate
     */
    fun isStale(timedData: TimedData<*>?, expectedIntervalMs: Long): Boolean {
        if (timedData == null) return true
        val age = System.currentTimeMillis() - timedData.receivedAt
        return age > (expectedIntervalMs * 2) // 2x tolerance
    }

    /**
     * Get staleness status for critical streams
     */
    fun getCriticalStaleness(): Map<String, Boolean> {
        return mapOf(
            "BMS" to isStale(_bmsPackStatus.value, 100),
            "Solar" to isStale(_solarPowerStatus.value, 100),
            "Monitor" to isStale(_packCurrentPower.value, 100),
            "Motor1" to isStale(_motor1Basic.value, 100),
            "Motor2" to isStale(_motor2Basic.value, 100),
            "SOC" to isStale(_socCapacity.value, 1000),
            "Switches" to isStale(_switchState.value, 1000)
        )
    }
}

/**
 * Wrapper to track when data was received for staleness detection
 */
data class TimedData<T : CanMessage>(
    val data: T,
    val receivedAt: Long
) {
    fun getAge(): Long = System.currentTimeMillis() - receivedAt
    fun isStale(maxAgeMs: Long): Boolean = getAge() > maxAgeMs
}

/**
 * Validation warning from cross-checks
 */
data class ValidationWarning(
    val severity: ValidationSeverity,
    val category: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

enum class ValidationSeverity {
    INFO, WARNING, ERROR, CRITICAL
}
