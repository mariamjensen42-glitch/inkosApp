package com.example.data.interaction

/**
 * Modes - Automation mode definitions.
 *
 * This is the Kotlin Android equivalent of the TypeScript modes.ts module.
 * It contains:
 * - AutomationMode - Automation mode enum
 */

enum class AutomationMode {
    AUTO,
    SEMI,
    MANUAL;

    companion object {
        fun fromString(value: String): AutomationMode {
            return when (value.lowercase()) {
                "auto" -> AUTO
                "semi" -> SEMI
                "manual" -> MANUAL
                else -> SEMI
            }
        }
    }
}

fun normalizeAutomationMode(mode: String?, fallback: AutomationMode = AutomationMode.SEMI): AutomationMode {
    if (mode == null) return fallback
    return try {
        AutomationMode.fromString(mode)
    } catch (e: Exception) {
        fallback
    }
}
