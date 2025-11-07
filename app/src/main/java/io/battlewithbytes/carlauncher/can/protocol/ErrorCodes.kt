package io.battlewithbytes.carlauncher.can.protocol

/**
 * Golf Cart CAN Protocol Error Codes
 * 8-bit structured: [Category:4 bits][Specific:4 bits]
 */
enum class ErrorCode(val code: Byte, val category: ErrorCategory, val description: String, val severity: ErrorSeverity) {
    // 0x00: No Error
    NO_ERROR(0x00, ErrorCategory.NONE, "No error", ErrorSeverity.INFO),

    // 0x1_: Temperature Errors
    TEMP_OVER_CRITICAL(0x10, ErrorCategory.TEMPERATURE, "Temperature over critical (>85°C)", ErrorSeverity.CRITICAL),
    TEMP_OVER_WARNING(0x11, ErrorCategory.TEMPERATURE, "Temperature over warning (>75°C)", ErrorSeverity.WARNING),
    TEMP_UNDER_CRITICAL(0x12, ErrorCategory.TEMPERATURE, "Temperature under critical (<-20°C)", ErrorSeverity.CRITICAL),
    TEMP_SENSOR_FAULT(0x13, ErrorCategory.TEMPERATURE, "Temperature sensor fault", ErrorSeverity.ERROR),

    // 0x2_: Voltage/Power Errors
    VOLTAGE_OVER_CRITICAL(0x20, ErrorCategory.VOLTAGE, "Voltage over critical (>60V)", ErrorSeverity.CRITICAL),
    VOLTAGE_OVER_WARNING(0x21, ErrorCategory.VOLTAGE, "Voltage over warning (>58V)", ErrorSeverity.WARNING),
    VOLTAGE_UNDER_CRITICAL(0x22, ErrorCategory.VOLTAGE, "Voltage under critical (<40V)", ErrorSeverity.CRITICAL),
    VOLTAGE_UNDER_WARNING(0x23, ErrorCategory.VOLTAGE, "Voltage under warning (<42V)", ErrorSeverity.WARNING),
    VOLTAGE_SENSOR_FAULT(0x27, ErrorCategory.VOLTAGE, "Voltage sensor fault", ErrorSeverity.ERROR),

    // 0x3_: Motor Controller Errors
    MOTOR_OVER_CURRENT(0x30, ErrorCategory.MOTOR, "Motor over-current", ErrorSeverity.ERROR),
    MOTOR_STALL(0x31, ErrorCategory.MOTOR, "Motor stall detected", ErrorSeverity.ERROR),
    MOTOR_COMM_TIMEOUT(0x32, ErrorCategory.MOTOR, "Motor communication timeout", ErrorSeverity.CRITICAL),

    // 0x4_: Communication Errors
    CAN_BUS_OFF(0x40, ErrorCategory.COMMUNICATION, "CAN bus off", ErrorSeverity.CRITICAL),
    CAN_RX_OVERRUN(0x41, ErrorCategory.COMMUNICATION, "CAN RX buffer overrun", ErrorSeverity.WARNING),

    // 0x5_: Battery/BMS Errors
    BATTERY_UNDER_VOLTAGE(0x51, ErrorCategory.BATTERY, "Battery under-voltage", ErrorSeverity.CRITICAL),
    BATTERY_OVER_CURRENT(0x52, ErrorCategory.BATTERY, "Battery over-current", ErrorSeverity.ERROR),
    BMS_FAULT(0x55, ErrorCategory.BATTERY, "BMS general fault", ErrorSeverity.CRITICAL),
    BATTERY_SOC_LOW(0x57, ErrorCategory.BATTERY, "Battery SOC low (<20%)", ErrorSeverity.WARNING),
    BMS_NOT_RESPONDING(0x58, ErrorCategory.BATTERY, "BMS not responding", ErrorSeverity.CRITICAL),

    // 0x6_: Charging System Errors
    CHARGER_FAULT(0x60, ErrorCategory.CHARGING, "Charger fault", ErrorSeverity.ERROR),
    CHARGING_TIMEOUT(0x61, ErrorCategory.CHARGING, "Charging timeout", ErrorSeverity.WARNING),

    // 0x7_: Safety System Errors
    EMERGENCY_STOP(0x73, ErrorCategory.SAFETY, "Emergency stop activated", ErrorSeverity.CRITICAL),

    // 0xA_: Time/GPS Errors
    TIME_JUMP_DETECTED(0xA7.toByte(), ErrorCategory.TIME_GPS, "Time jump detected (>10s)", ErrorSeverity.WARNING),
    GPS_NO_FIX(0xA8.toByte(), ErrorCategory.TIME_GPS, "GPS no fix", ErrorSeverity.INFO),

    // 0xF_: General System Errors
    SYSTEM_INIT_FAIL(0xF0.toByte(), ErrorCategory.SYSTEM, "System initialization failed", ErrorSeverity.CRITICAL),
    WATCHDOG_RESET(0xF1.toByte(), ErrorCategory.SYSTEM, "Watchdog reset occurred", ErrorSeverity.ERROR);

    companion object {
        fun fromByte(code: Byte): ErrorCode {
            return values().find { it.code == code } ?: NO_ERROR
        }

        fun getCategory(code: Byte): ErrorCategory {
            val categoryBits = (code.toInt() and 0xF0)
            return ErrorCategory.values().find { it.mask == categoryBits } ?: ErrorCategory.UNKNOWN
        }
    }
}

enum class ErrorCategory(val mask: Int, val displayName: String) {
    NONE(0x00, "No Error"),
    TEMPERATURE(0x10, "Temperature"),
    VOLTAGE(0x20, "Voltage/Power"),
    MOTOR(0x30, "Motor Controller"),
    COMMUNICATION(0x40, "Communication"),
    BATTERY(0x50, "Battery/BMS"),
    CHARGING(0x60, "Charging System"),
    SAFETY(0x70, "Safety Systems"),
    UI(0x80, "User Interface"),
    SENSORS(0x90, "Sensors"),
    TIME_GPS(0xA0, "Time/GPS"),
    SYSTEM(0xF0, "General System"),
    UNKNOWN(0xFF, "Unknown")
}

enum class ErrorSeverity {
    INFO,      // Informational only
    WARNING,   // Degraded performance
    ERROR,     // Functionality impaired
    CRITICAL   // Safety-critical, immediate action required
}
