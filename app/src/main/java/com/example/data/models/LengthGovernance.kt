package com.example.data.models

import kotlinx.serialization.Serializable

/**
 * Length counting mode - matches TypeScript LengthCountingModeSchema
 */
@Serializable
enum class LengthCountingMode {
    ZH_CHARS,  // Chinese character counting
    EN_WORDS   // English word counting
}

/**
 * Length normalize mode - matches TypeScript LengthNormalizeModeSchema
 */
@Serializable
enum class LengthNormalizeMode {
    EXPAND,
    COMPRESS,
    NONE
}

/**
 * Length specification - matches TypeScript LengthSpecSchema
 */
@Serializable
data class LengthSpec(
    val target: Int,
    val softMin: Int,
    val softMax: Int,
    val hardMin: Int,
    val hardMax: Int,
    val countingMode: LengthCountingMode,
    val normalizeMode: LengthNormalizeMode
)

/**
 * Length telemetry - matches TypeScript LengthTelemetrySchema
 */
@Serializable
data class LengthTelemetry(
    val target: Int,
    val softMin: Int,
    val softMax: Int,
    val hardMin: Int,
    val hardMax: Int,
    val countingMode: LengthCountingMode,
    val writerCount: Int = 0,
    val postWriterNormalizeCount: Int = 0,
    val postReviseCount: Int = 0,
    val finalCount: Int = 0,
    val normalizeApplied: Boolean = false,
    val lengthWarning: Boolean = false
)

/**
 * Length warning - matches TypeScript LengthWarningSchema
 */
@Serializable
data class LengthWarning(
    val chapter: Int,
    val target: Int,
    val actual: Int,
    val countingMode: LengthCountingMode,
    val reason: String
)

/**
 * Length language type
 */
typealias LengthLanguage = String // "zh" or "en"

/**
 * Build a length specification based on target words and language
 */
fun buildLengthSpec(targetWords: Int, language: LengthLanguage): LengthSpec {
    val countingMode = if (language == "en") LengthCountingMode.EN_WORDS else LengthCountingMode.ZH_CHARS
    val softMin = (targetWords * 0.75).toInt()
    val softMax = (targetWords * 1.25).toInt()
    val hardMin = (targetWords * 0.5).toInt()
    val hardMax = (targetWords * 1.5).toInt()
    return LengthSpec(
        target = targetWords,
        softMin = softMin,
        softMax = softMax,
        hardMin = hardMin,
        hardMax = hardMax,
        countingMode = countingMode,
        normalizeMode = LengthNormalizeMode.NONE
    )
}

/**
 * Count chapter length based on counting mode
 */
fun countChapterLength(content: String, mode: LengthCountingMode): Int {
    return when (mode) {
        LengthCountingMode.ZH_CHARS -> {
            // Count Chinese characters
            content.count { it.code in 0x4E00..0x9FFF }
        }
        LengthCountingMode.EN_WORDS -> {
            // Count English words
            content.split(Regex("\\s+")).filter { it.isNotEmpty() }.size
        }
    }
}

/**
 * Format length count with unit
 */
fun formatLengthCount(count: Int, mode: LengthCountingMode): String {
    return when (mode) {
        LengthCountingMode.ZH_CHARS -> "${count}字"
        LengthCountingMode.EN_WORDS -> "${count} words"
    }
}

/**
 * Check if length is outside soft range
 */
fun isOutsideSoftRange(count: Int, spec: LengthSpec): Boolean {
    return count < spec.softMin || count > spec.softMax
}

/**
 * Check if length is outside hard range
 */
fun isOutsideHardRange(count: Int, spec: LengthSpec): Boolean {
    return count < spec.hardMin || count > spec.hardMax
}

/**
 * Choose normalize mode based on count and spec
 */
fun chooseNormalizeMode(count: Int, spec: LengthSpec): LengthNormalizeMode {
    return when {
        count < spec.softMin -> LengthNormalizeMode.EXPAND
        count > spec.softMax -> LengthNormalizeMode.COMPRESS
        else -> LengthNormalizeMode.NONE
    }
}

/**
 * Default chapter length constants
 */
const val DEFAULT_CHAPTER_LENGTH_ZH = 3000
const val DEFAULT_CHAPTER_LENGTH_EN = 2000

/**
 * Resolve length counting mode from language
 */
fun resolveLengthCountingMode(language: LengthLanguage): LengthCountingMode {
    return if (language == "en") LengthCountingMode.EN_WORDS else LengthCountingMode.ZH_CHARS
}
