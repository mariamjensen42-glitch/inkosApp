package com.example.data.models

import kotlinx.serialization.Serializable

/**
 * Platform enum - matches TypeScript PlatformSchema
 */
@Serializable
enum class Platform {
    TOMATO,    // 番茄
    FEILU,     // 飞卢
    QIDIAN,    // 起点
    OTHER;

    companion object {
        fun fromString(value: String): Platform {
            val normalized = value.trim().lowercase().replace(Regex("[\\s_-]+"), "")
            return when {
                normalized in listOf("tomato", "fanqie", "fanqienovel") || value.contains("番茄") -> TOMATO
                normalized in listOf("qidian", "qidianzhongwenwang") || value.contains("起点") -> QIDIAN
                normalized in listOf("feilu") || value.contains("飞卢") -> FEILU
                else -> OTHER
            }
        }
    }
}

/**
 * Book status enum - matches TypeScript BookStatusSchema
 */
@Serializable
enum class BookStatus {
    INCUBATING,
    OUTLINING,
    ACTIVE,
    PAUSED,
    COMPLETED,
    DROPPED
}

/**
 * Fanfic mode enum - matches TypeScript FanficModeSchema
 */
@Serializable
enum class FanficMode {
    CANON,
    AU,
    OOC,
    CP
}

/**
 * Book configuration - matches TypeScript BookConfigSchema
 */
@Serializable
data class BookConfig(
    val id: String,
    val title: String,
    val platform: Platform = Platform.OTHER,
    val genre: String = "other",
    val status: BookStatus = BookStatus.INCUBATING,
    val targetChapters: Int = 200,
    val chapterWordCount: Int = 3000,
    val language: String? = null, // "zh" or "en"
    val createdAt: String = "",
    val updatedAt: String = "",
    val parentBookId: String? = null,
    val fanficMode: FanficMode? = null
)

/**
 * Chapter status enum - matches TypeScript ChapterStatusSchema
 */
@Serializable
enum class ChapterStatus {
    CARD_GENERATED,
    DRAFTING,
    DRAFTED,
    AUDITING,
    AUDIT_PASSED,
    AUDIT_FAILED,
    STATE_DEGRADED,
    REVISING,
    READY_FOR_REVIEW,
    APPROVED,
    REJECTED,
    PUBLISHED,
    IMPORTED
}

/**
 * Chapter metadata - matches TypeScript ChapterMetaSchema
 */
@Serializable
data class ChapterMeta(
    val number: Int,
    val title: String,
    val status: ChapterStatus = ChapterStatus.CARD_GENERATED,
    val wordCount: Int = 0,
    val createdAt: String = "",
    val updatedAt: String = "",
    val auditIssues: List<String> = emptyList(),
    val lengthWarnings: List<String> = emptyList(),
    val reviewNote: String? = null,
    val detectionScore: Double? = null,
    val detectionProvider: String? = null,
    val detectedAt: String? = null,
    val lengthTelemetry: LengthTelemetry? = null,
    val tokenUsage: TokenUsage? = null
)

/**
 * Token usage tracking
 */
@Serializable
data class TokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
) {
    operator fun plus(other: TokenUsage): TokenUsage = TokenUsage(
        promptTokens = promptTokens + other.promptTokens,
        completionTokens = completionTokens + other.completionTokens,
        totalTokens = totalTokens + other.totalTokens
    )
}
