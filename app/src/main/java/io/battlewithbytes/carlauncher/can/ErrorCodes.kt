package io.battlewithbytes.carlauncher.can

/**
 * Complete error code system for Golf Cart CAN Protocol
 * Structure: Bits 7-4 = Category, Bits 3-0 = Specific Error
 */

enum class ErrorCategory(val code: UByte) {
    NO_ERROR(0x00u),
    TEMPERATURE(0x10u),
    VOLTAGE_POWER(0x20u),
    MOTOR_CONTROL(0x30u),
    COMMUNICATION(0x40u),
    BATTERY_BMS(0x50u),
    CHARGING_SYSTEM(0x60u),
    SAFETY_SYSTEMS(0x70u),
    USER_INTERFACE(0x80u),
    SENSORS(0x90u),
    TIME_GPS(0xA0u),
    GENERAL_SYSTEM(0xF0u);

    companion object {
        fun fromCode(code: UByte): ErrorCategory? {
            val categoryCode = (code.toInt() and 0xF0).toUByte()
            return values().find { it.code == categoryCode }
        }
    }
}

/**
 * Complete error code enumeration with all 144 error codes
 */
enum class ErrorCode(val code: UByte, val category: ErrorCategory, val description: String, val priority: ErrorPriority) {
    // 0x00 - No Error
    NO_ERROR(0x00u, ErrorCategory.NO_ERROR, "All systems normal", ErrorPriority.INFO),

    // 0x10-0x1F - Temperature Errors
    TEMP_OVER_CRITICAL(0x10u, ErrorCategory.TEMPERATURE, "Temperature over critical threshold", ErrorPriority.CRITICAL),
    TEMP_OVER_WARNING(0x11u, ErrorCategory.TEMPERATURE, "Temperature over warning threshold", ErrorPriority.WARNING),
    TEMP_UNDER_CRITICAL(0x12u, ErrorCategory.TEMPERATURE, "Temperature under critical threshold", ErrorPriority.CRITICAL),
    TEMP_UNDER_WARNING(0x13u, ErrorCategory.TEMPERATURE, "Temperature under warning threshold", ErrorPriority.WARNING),
    TEMP_SENSOR_FAULT(0x14u, ErrorCategory.TEMPERATURE, "Temperature sensor fault or disconnected", ErrorPriority.ERROR),
    TEMP_SENSOR_SHORT(0x15u, ErrorCategory.TEMPERATURE, "Temperature sensor shorted", ErrorPriority.ERROR),
    TEMP_RAPID_CHANGE(0x16u, ErrorCategory.TEMPERATURE, "Rapid temperature change detected", ErrorPriority.WARNING),
    TEMP_DERATING_ACTIVE(0x17u, ErrorCategory.TEMPERATURE, "Thermal derating active", ErrorPriority.WARNING),
    TEMP_MULTIPLE_SENSORS_FAULT(0x18u, ErrorCategory.TEMPERATURE, "Multiple temperature sensors failed", ErrorPriority.ERROR),
    TEMP_GRADIENT_EXCESSIVE(0x19u, ErrorCategory.TEMPERATURE, "Temperature gradient too high", ErrorPriority.WARNING),
    TEMP_READING_INVALID(0x1Au, ErrorCategory.TEMPERATURE, "Invalid temperature reading", ErrorPriority.ERROR),

    // 0x20-0x2F - Voltage/Power Errors
    VOLTAGE_OVER_CRITICAL(0x20u, ErrorCategory.VOLTAGE_POWER, "Voltage over critical threshold", ErrorPriority.CRITICAL),
    VOLTAGE_OVER_WARNING(0x21u, ErrorCategory.VOLTAGE_POWER, "Voltage over warning threshold", ErrorPriority.WARNING),
    VOLTAGE_UNDER_CRITICAL(0x22u, ErrorCategory.VOLTAGE_POWER, "Voltage under critical threshold", ErrorPriority.CRITICAL),
    VOLTAGE_UNDER_WARNING(0x23u, ErrorCategory.VOLTAGE_POWER, "Voltage under warning threshold", ErrorPriority.WARNING),
    VOLTAGE_12V_LOW(0x24u, ErrorCategory.VOLTAGE_POWER, "12V auxiliary supply low", ErrorPriority.ERROR),
    VOLTAGE_5V_FAULT(0x25u, ErrorCategory.VOLTAGE_POWER, "5V logic supply fault", ErrorPriority.CRITICAL),
    POWER_SUPPLY_FAULT(0x26u, ErrorCategory.VOLTAGE_POWER, "Power supply fault", ErrorPriority.CRITICAL),
    VOLTAGE_IMBALANCE(0x27u, ErrorCategory.VOLTAGE_POWER, "Phase voltage imbalance", ErrorPriority.WARNING),
    VOLTAGE_DIVIDER_FAULT(0x28u, ErrorCategory.VOLTAGE_POWER, "Voltage divider measurement fault", ErrorPriority.ERROR),
    POWER_LIMIT_EXCEEDED(0x29u, ErrorCategory.VOLTAGE_POWER, "Power limit exceeded", ErrorPriority.WARNING),
    VOLTAGE_SPIKE_DETECTED(0x2Au, ErrorCategory.VOLTAGE_POWER, "Voltage spike detected", ErrorPriority.WARNING),
    VOLTAGE_SAG_DETECTED(0x2Bu, ErrorCategory.VOLTAGE_POWER, "Voltage sag detected", ErrorPriority.WARNING),
    GROUND_FAULT(0x2Cu, ErrorCategory.VOLTAGE_POWER, "Ground fault detected", ErrorPriority.CRITICAL),

    // 0x30-0x3F - Motor Control Errors
    MOTOR_OVER_CURRENT(0x30u, ErrorCategory.MOTOR_CONTROL, "Motor over current", ErrorPriority.CRITICAL),
    MOTOR_OVER_TEMP(0x31u, ErrorCategory.MOTOR_CONTROL, "Motor over temperature", ErrorPriority.CRITICAL),
    MOTOR_COMM_TIMEOUT(0x32u, ErrorCategory.MOTOR_CONTROL, "Motor controller communication timeout", ErrorPriority.ERROR),
    MOTOR_HALL_FAULT(0x33u, ErrorCategory.MOTOR_CONTROL, "Hall sensor fault", ErrorPriority.ERROR),
    MOTOR_THROTTLE_FAULT(0x34u, ErrorCategory.MOTOR_CONTROL, "Throttle input fault", ErrorPriority.ERROR),
    MOTOR_CONTROLLER_FAULT(0x35u, ErrorCategory.MOTOR_CONTROL, "Motor controller general fault", ErrorPriority.ERROR),
    MOTOR_1_OFFLINE(0x36u, ErrorCategory.MOTOR_CONTROL, "Motor 1 controller offline", ErrorPriority.ERROR),
    MOTOR_2_OFFLINE(0x37u, ErrorCategory.MOTOR_CONTROL, "Motor 2 controller offline", ErrorPriority.ERROR),
    MOTOR_PHASE_FAULT(0x38u, ErrorCategory.MOTOR_CONTROL, "Motor phase winding fault", ErrorPriority.CRITICAL),
    MOTOR_ENCODER_FAULT(0x39u, ErrorCategory.MOTOR_CONTROL, "Motor encoder fault", ErrorPriority.ERROR),
    MOTOR_OVERCURRENT_WARNING(0x3Au, ErrorCategory.MOTOR_CONTROL, "Motor over current warning", ErrorPriority.WARNING),
    MOTOR_PWM_FAULT(0x3Bu, ErrorCategory.MOTOR_CONTROL, "PWM output fault", ErrorPriority.ERROR),
    MOTOR_REGEN_FAULT(0x3Cu, ErrorCategory.MOTOR_CONTROL, "Regenerative braking fault", ErrorPriority.WARNING),
    MOTOR_STALL_DETECTED(0x3Du, ErrorCategory.MOTOR_CONTROL, "Motor stall detected", ErrorPriority.WARNING),
    MOTOR_DESYNC(0x3Eu, ErrorCategory.MOTOR_CONTROL, "Motor desynchronization", ErrorPriority.ERROR),

    // 0x40-0x4F - Communication Errors
    CAN_BUS_OFF(0x40u, ErrorCategory.COMMUNICATION, "CAN bus off state", ErrorPriority.CRITICAL),
    CAN_RX_OVERFLOW(0x41u, ErrorCategory.COMMUNICATION, "CAN receive buffer overflow", ErrorPriority.ERROR),
    CAN_TX_OVERFLOW(0x42u, ErrorCategory.COMMUNICATION, "CAN transmit buffer overflow", ErrorPriority.WARNING),
    CAN_ERROR_PASSIVE(0x43u, ErrorCategory.COMMUNICATION, "CAN error passive state", ErrorPriority.WARNING),
    CAN_BUS_ERROR(0x44u, ErrorCategory.COMMUNICATION, "CAN bus error", ErrorPriority.ERROR),
    CAN_CRC_ERROR(0x45u, ErrorCategory.COMMUNICATION, "CAN message CRC error", ErrorPriority.WARNING),
    CAN_PROTOCOL_ERROR(0x46u, ErrorCategory.COMMUNICATION, "CAN protocol violation", ErrorPriority.ERROR),
    DEVICE_NOT_RESPONDING(0x47u, ErrorCategory.COMMUNICATION, "Device not responding", ErrorPriority.ERROR),
    SERIAL_COMM_ERROR(0x48u, ErrorCategory.COMMUNICATION, "Serial communication error", ErrorPriority.ERROR),
    I2C_BUS_ERROR(0x49u, ErrorCategory.COMMUNICATION, "I2C bus error", ErrorPriority.ERROR),
    SPI_BUS_ERROR(0x4Au, ErrorCategory.COMMUNICATION, "SPI bus error", ErrorPriority.ERROR),
    UART_FRAMING_ERROR(0x4Bu, ErrorCategory.COMMUNICATION, "UART framing error", ErrorPriority.WARNING),
    UART_OVERRUN(0x4Cu, ErrorCategory.COMMUNICATION, "UART overrun error", ErrorPriority.WARNING),
    MESSAGE_STALE(0x4Du, ErrorCategory.COMMUNICATION, "Message data stale/timeout", ErrorPriority.WARNING),
    NETWORK_CONGESTION(0x4Eu, ErrorCategory.COMMUNICATION, "Network congestion detected", ErrorPriority.WARNING),

    // 0x50-0x5F - Battery/BMS Errors
    BATTERY_OVER_VOLTAGE(0x50u, ErrorCategory.BATTERY_BMS, "Battery pack over voltage", ErrorPriority.CRITICAL),
    BATTERY_UNDER_VOLTAGE(0x51u, ErrorCategory.BATTERY_BMS, "Battery pack under voltage", ErrorPriority.CRITICAL),
    BATTERY_OVER_CURRENT(0x52u, ErrorCategory.BATTERY_BMS, "Battery over current", ErrorPriority.CRITICAL),
    BATTERY_OVER_TEMP(0x53u, ErrorCategory.BATTERY_BMS, "Battery over temperature", ErrorPriority.CRITICAL),
    BATTERY_UNDER_TEMP(0x54u, ErrorCategory.BATTERY_BMS, "Battery under temperature", ErrorPriority.WARNING),
    BMS_FAULT(0x55u, ErrorCategory.BATTERY_BMS, "BMS general fault", ErrorPriority.CRITICAL),
    CELL_IMBALANCE(0x56u, ErrorCategory.BATTERY_BMS, "Cell voltage imbalance", ErrorPriority.WARNING),
    BATTERY_SOC_LOW(0x57u, ErrorCategory.BATTERY_BMS, "Battery state of charge low", ErrorPriority.WARNING),
    BATTERY_SOC_CRITICAL(0x58u, ErrorCategory.BATTERY_BMS, "Battery state of charge critical", ErrorPriority.ERROR),
    CELL_OVER_VOLTAGE(0x59u, ErrorCategory.BATTERY_BMS, "Individual cell over voltage", ErrorPriority.CRITICAL),
    CELL_UNDER_VOLTAGE(0x5Au, ErrorCategory.BATTERY_BMS, "Individual cell under voltage", ErrorPriority.CRITICAL),
    BMS_COMM_FAULT(0x5Bu, ErrorCategory.BATTERY_BMS, "BMS communication fault", ErrorPriority.ERROR),
    CHARGE_MOSFET_FAULT(0x5Cu, ErrorCategory.BATTERY_BMS, "Charge MOSFET fault", ErrorPriority.ERROR),
    DISCHARGE_MOSFET_FAULT(0x5Du, ErrorCategory.BATTERY_BMS, "Discharge MOSFET fault", ErrorPriority.ERROR),
    BMS_CALIBRATION_ERROR(0x5Eu, ErrorCategory.BATTERY_BMS, "BMS calibration error", ErrorPriority.WARNING),
    BATTERY_AGING_WARNING(0x5Fu, ErrorCategory.BATTERY_BMS, "Battery aging/degradation warning", ErrorPriority.INFO),

    // 0x60-0x6F - Charging System Errors
    SOLAR_OVER_VOLTAGE(0x60u, ErrorCategory.CHARGING_SYSTEM, "Solar charger over voltage", ErrorPriority.CRITICAL),
    SOLAR_OVER_CURRENT(0x61u, ErrorCategory.CHARGING_SYSTEM, "Solar charger over current", ErrorPriority.ERROR),
    CHARGER_OVER_TEMP(0x62u, ErrorCategory.CHARGING_SYSTEM, "Charger over temperature", ErrorPriority.CRITICAL),
    CHARGER_FAULT(0x63u, ErrorCategory.CHARGING_SYSTEM, "Charger general fault", ErrorPriority.ERROR),
    CHARGER_TIMEOUT(0x64u, ErrorCategory.CHARGING_SYSTEM, "Charging timeout", ErrorPriority.WARNING),
    CHARGER_INPUT_FAULT(0x65u, ErrorCategory.CHARGING_SYSTEM, "Charger input fault", ErrorPriority.ERROR),
    CHARGER_OUTPUT_FAULT(0x66u, ErrorCategory.CHARGING_SYSTEM, "Charger output fault", ErrorPriority.ERROR),
    LOAD_SHORT_CIRCUIT(0x67u, ErrorCategory.CHARGING_SYSTEM, "Load output short circuit", ErrorPriority.CRITICAL),
    CHARGE_MOS_FAULT(0x68u, ErrorCategory.CHARGING_SYSTEM, "Charge MOSFET fault", ErrorPriority.ERROR),
    LOAD_MOS_FAULT(0x69u, ErrorCategory.CHARGING_SYSTEM, "Load MOSFET fault", ErrorPriority.ERROR),
    PV_INPUT_SHORT(0x6Au, ErrorCategory.CHARGING_SYSTEM, "PV input short circuit", ErrorPriority.ERROR),
    BATTERY_TYPE_MISMATCH(0x6Bu, ErrorCategory.CHARGING_SYSTEM, "Battery type mismatch", ErrorPriority.ERROR),
    CHARGING_REVERSE_POLARITY(0x6Cu, ErrorCategory.CHARGING_SYSTEM, "Reverse polarity detected", ErrorPriority.CRITICAL),
    PV_OVER_VOLTAGE(0x6Du, ErrorCategory.CHARGING_SYSTEM, "PV input over voltage", ErrorPriority.ERROR),
    EQUALIZATION_FAULT(0x6Eu, ErrorCategory.CHARGING_SYSTEM, "Equalization fault", ErrorPriority.WARNING),

    // 0x70-0x7F - Safety Systems Errors
    CONTACTOR_OPEN_FAULT(0x70u, ErrorCategory.SAFETY_SYSTEMS, "Main contactor failed to open", ErrorPriority.CRITICAL),
    CONTACTOR_CLOSE_FAULT(0x71u, ErrorCategory.SAFETY_SYSTEMS, "Main contactor failed to close", ErrorPriority.CRITICAL),
    PRECHARGE_FAULT(0x72u, ErrorCategory.SAFETY_SYSTEMS, "Precharge circuit fault", ErrorPriority.ERROR),
    EMERGENCY_STOP(0x73u, ErrorCategory.SAFETY_SYSTEMS, "Emergency stop activated", ErrorPriority.CRITICAL),
    INTERLOCK_OPEN(0x74u, ErrorCategory.SAFETY_SYSTEMS, "Safety interlock open", ErrorPriority.CRITICAL),
    BRAKE_FAULT(0x75u, ErrorCategory.SAFETY_SYSTEMS, "Brake system fault", ErrorPriority.CRITICAL),
    SEAT_SWITCH_FAULT(0x76u, ErrorCategory.SAFETY_SYSTEMS, "Seat occupancy switch fault", ErrorPriority.WARNING),
    PARKING_BRAKE_NOT_SET(0x77u, ErrorCategory.SAFETY_SYSTEMS, "Parking brake not set", ErrorPriority.WARNING),
    DIRECTION_CONFLICT(0x78u, ErrorCategory.SAFETY_SYSTEMS, "Forward/Reverse direction conflict", ErrorPriority.ERROR),
    SAFETY_RELAY_FAULT(0x79u, ErrorCategory.SAFETY_SYSTEMS, "Safety relay fault", ErrorPriority.CRITICAL),
    FUSE_BLOWN(0x7Au, ErrorCategory.SAFETY_SYSTEMS, "Fuse blown", ErrorPriority.CRITICAL),
    ISOLATION_FAULT(0x7Bu, ErrorCategory.SAFETY_SYSTEMS, "Isolation fault detected", ErrorPriority.CRITICAL),
    ARC_FAULT_DETECTED(0x7Cu, ErrorCategory.SAFETY_SYSTEMS, "Arc fault detected", ErrorPriority.CRITICAL),
    ROLLAWAY_PREVENTION(0x7Du, ErrorCategory.SAFETY_SYSTEMS, "Rollaway prevention active", ErrorPriority.INFO),

    // 0x80-0x8F - User Interface Errors
    DISPLAY_NOT_RESPONDING(0x80u, ErrorCategory.USER_INTERFACE, "Display not responding", ErrorPriority.WARNING),
    DISPLAY_COMM_ERROR(0x81u, ErrorCategory.USER_INTERFACE, "Display communication error", ErrorPriority.WARNING),
    LEFT_BLINKER_FAULT(0x82u, ErrorCategory.USER_INTERFACE, "Left turn signal fault", ErrorPriority.WARNING),
    RIGHT_BLINKER_FAULT(0x83u, ErrorCategory.USER_INTERFACE, "Right turn signal fault", ErrorPriority.WARNING),
    HEADLIGHT_FAULT(0x84u, ErrorCategory.USER_INTERFACE, "Headlight fault", ErrorPriority.WARNING),
    BRAKE_LIGHT_FAULT(0x85u, ErrorCategory.USER_INTERFACE, "Brake light fault", ErrorPriority.WARNING),
    HORN_FAULT(0x86u, ErrorCategory.USER_INTERFACE, "Horn fault", ErrorPriority.WARNING),
    BUTTON_STUCK(0x87u, ErrorCategory.USER_INTERFACE, "Button stuck/debounce fault", ErrorPriority.WARNING),
    SWITCH_FAULT(0x88u, ErrorCategory.USER_INTERFACE, "Switch input fault", ErrorPriority.WARNING),
    LED_DRIVER_FAULT(0x89u, ErrorCategory.USER_INTERFACE, "LED driver fault", ErrorPriority.WARNING),
    BACKLIGHT_FAULT(0x8Au, ErrorCategory.USER_INTERFACE, "Display backlight fault", ErrorPriority.INFO),
    BUZZER_FAULT(0x8Bu, ErrorCategory.USER_INTERFACE, "Buzzer/audio fault", ErrorPriority.INFO),

    // 0x90-0x9F - Sensor Errors
    SPEED_SENSOR_FAULT(0x90u, ErrorCategory.SENSORS, "Speed sensor fault", ErrorPriority.ERROR),
    CURRENT_SENSOR_FAULT(0x91u, ErrorCategory.SENSORS, "Current sensor fault", ErrorPriority.CRITICAL),
    VOLTAGE_SENSOR_FAULT(0x92u, ErrorCategory.SENSORS, "Voltage sensor fault", ErrorPriority.CRITICAL),
    THROTTLE_SENSOR_FAULT(0x93u, ErrorCategory.SENSORS, "Throttle position sensor fault", ErrorPriority.ERROR),
    BRAKE_SENSOR_FAULT(0x94u, ErrorCategory.SENSORS, "Brake position sensor fault", ErrorPriority.ERROR),
    ADC_FAULT(0x95u, ErrorCategory.SENSORS, "ADC conversion fault", ErrorPriority.ERROR),
    SHUNT_CALIBRATION_ERROR(0x96u, ErrorCategory.SENSORS, "Current shunt calibration error", ErrorPriority.WARNING),
    SENSOR_OUT_OF_RANGE(0x97u, ErrorCategory.SENSORS, "Sensor reading out of range", ErrorPriority.WARNING),
    SENSOR_NOISE_EXCESSIVE(0x98u, ErrorCategory.SENSORS, "Sensor noise level excessive", ErrorPriority.WARNING),
    SENSOR_DRIFT_DETECTED(0x99u, ErrorCategory.SENSORS, "Sensor drift detected", ErrorPriority.WARNING),
    ACCELEROMETER_FAULT(0x9Au, ErrorCategory.SENSORS, "Accelerometer fault", ErrorPriority.WARNING),
    GYROSCOPE_FAULT(0x9Bu, ErrorCategory.SENSORS, "Gyroscope fault", ErrorPriority.WARNING),
    PRESSURE_SENSOR_FAULT(0x9Cu, ErrorCategory.SENSORS, "Pressure sensor fault", ErrorPriority.WARNING),

    // 0xA0-0xAF - Time/GPS Errors
    TIME_NEVER_SET(0xA0u, ErrorCategory.TIME_GPS, "Time never set/initialized", ErrorPriority.WARNING),
    GPS_NO_FIX(0xA1u, ErrorCategory.TIME_GPS, "GPS no fix available", ErrorPriority.INFO),
    GPS_NOT_RESPONDING(0xA2u, ErrorCategory.TIME_GPS, "GPS module not responding", ErrorPriority.WARNING),
    GPS_ANTENNA_FAULT(0xA3u, ErrorCategory.TIME_GPS, "GPS antenna fault", ErrorPriority.WARNING),
    RTC_FAULT(0xA4u, ErrorCategory.TIME_GPS, "Real-time clock fault", ErrorPriority.ERROR),
    RTC_BATTERY_LOW(0xA5u, ErrorCategory.TIME_GPS, "RTC backup battery low", ErrorPriority.WARNING),
    TIME_DRIFT_WARNING(0xA6u, ErrorCategory.TIME_GPS, "Time drift warning", ErrorPriority.WARNING),
    TIME_JUMP_DETECTED(0xA7u, ErrorCategory.TIME_GPS, "Time jump detected", ErrorPriority.WARNING),
    GPS_SIGNAL_WEAK(0xA8u, ErrorCategory.TIME_GPS, "GPS signal weak", ErrorPriority.INFO),
    GPS_JAMMING_DETECTED(0xA9u, ErrorCategory.TIME_GPS, "GPS jamming/interference detected", ErrorPriority.WARNING),
    LEAP_SECOND_WARNING(0xAAu, ErrorCategory.TIME_GPS, "Leap second approaching", ErrorPriority.INFO),
    GPS_ACCURACY_POOR(0xABu, ErrorCategory.TIME_GPS, "GPS accuracy degraded", ErrorPriority.INFO),

    // 0xF0-0xFF - General System Errors
    WATCHDOG_RESET(0xF0u, ErrorCategory.GENERAL_SYSTEM, "Watchdog timer reset", ErrorPriority.ERROR),
    MEMORY_CORRUPTION(0xF1u, ErrorCategory.GENERAL_SYSTEM, "Memory corruption detected", ErrorPriority.CRITICAL),
    EEPROM_FAULT(0xF2u, ErrorCategory.GENERAL_SYSTEM, "EEPROM read/write fault", ErrorPriority.ERROR),
    FRAM_FAULT(0xF3u, ErrorCategory.GENERAL_SYSTEM, "FRAM memory fault", ErrorPriority.ERROR),
    FLASH_FAULT(0xF4u, ErrorCategory.GENERAL_SYSTEM, "Flash memory fault", ErrorPriority.ERROR),
    CONFIG_ERROR(0xF5u, ErrorCategory.GENERAL_SYSTEM, "Configuration error", ErrorPriority.ERROR),
    FIRMWARE_MISMATCH(0xF6u, ErrorCategory.GENERAL_SYSTEM, "Firmware version mismatch", ErrorPriority.WARNING),
    BOOTLOADER_ERROR(0xF7u, ErrorCategory.GENERAL_SYSTEM, "Bootloader error", ErrorPriority.CRITICAL),
    STACK_OVERFLOW(0xF8u, ErrorCategory.GENERAL_SYSTEM, "Stack overflow detected", ErrorPriority.CRITICAL),
    HEAP_EXHAUSTED(0xF9u, ErrorCategory.GENERAL_SYSTEM, "Heap memory exhausted", ErrorPriority.CRITICAL),
    CPU_OVERLOAD(0xFAu, ErrorCategory.GENERAL_SYSTEM, "CPU overload detected", ErrorPriority.WARNING),
    TASK_TIMEOUT(0xFBu, ErrorCategory.GENERAL_SYSTEM, "Task execution timeout", ErrorPriority.ERROR),
    ASSERT_FAILED(0xFCu, ErrorCategory.GENERAL_SYSTEM, "Software assertion failed", ErrorPriority.CRITICAL),
    UNKNOWN_ERROR(0xFDu, ErrorCategory.GENERAL_SYSTEM, "Unknown error condition", ErrorPriority.ERROR),
    SYSTEM_INITIALIZATION_FAILED(0xFEu, ErrorCategory.GENERAL_SYSTEM, "System initialization failed", ErrorPriority.CRITICAL),
    RESERVED_ERROR(0xFFu, ErrorCategory.GENERAL_SYSTEM, "Reserved error code", ErrorPriority.INFO);

    companion object {
        private val codeMap = values().associateBy { it.code }

        fun fromCode(code: UByte): ErrorCode {
            return codeMap[code] ?: UNKNOWN_ERROR
        }

        fun getCategoryErrors(category: ErrorCategory): List<ErrorCode> {
            return values().filter { it.category == category }
        }

        fun getCriticalErrors(): List<ErrorCode> {
            return values().filter { it.priority == ErrorPriority.CRITICAL }
        }
    }

    /**
     * Extracts category code (bits 7-4)
     */
    fun getCategoryCode(): UByte = (code.toInt() and 0xF0).toUByte()

    /**
     * Extracts specific error code (bits 3-0)
     */
    fun getSpecificCode(): UByte = (code.toInt() and 0x0F).toUByte()

    /**
     * Checks if this is a critical error that requires immediate action
     */
    fun isCritical(): Boolean = priority == ErrorPriority.CRITICAL

    /**
     * Checks if this error should trigger an alarm/warning
     */
    fun shouldAlarm(): Boolean = priority >= ErrorPriority.ERROR
}

/**
 * Error priority levels
 */
enum class ErrorPriority {
    INFO,      // Informational only
    WARNING,   // Warning condition, no immediate action required
    ERROR,     // Error condition, action required soon
    CRITICAL   // Critical condition, immediate action required
}

/**
 * Extension function to convert UByte to ErrorCode
 */
fun UByte.toErrorCode(): ErrorCode = ErrorCode.fromCode(this)

/**
 * Extension function to check if error code represents an error condition
 */
fun UByte.isError(): Boolean = this != 0x00.toUByte()

/**
 * Extension function to get error category
 */
fun UByte.getErrorCategory(): ErrorCategory? = ErrorCategory.fromCode(this)
