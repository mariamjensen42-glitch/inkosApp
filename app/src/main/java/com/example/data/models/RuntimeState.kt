package com.example.data.models

import kotlinx.serialization.Serializable

/**
 * Runtime state language - matches TypeScript RuntimeStateLanguageSchema
 */
@Serializable
enum class RuntimeStateLanguage {
    ZH,
    EN
}

/**
 * State manifest - matches TypeScript StateManifestSchema
 */
@Serializable
data class StateManifest(
    val schemaVersion: Int = 2,
    val language: RuntimeStateLanguage = RuntimeStateLanguage.ZH,
    val lastAppliedChapter: Int = 0,
    val projectionVersion: Int = 1,
    val migrationWarnings: List<String> = emptyList()
)

/**
 * Hook status - matches TypeScript HookStatusSchema
 */
@Serializable
enum class HookStatus {
    OPEN,
    PROGRESSING,
    DEFERRED,
    RESOLVED
}

/**
 * Hook payoff timing - matches TypeScript HookPayoffTimingSchema
 */
@Serializable
enum class HookPayoffTiming {
    IMMEDIATE,
    NEAR_TERM,
    MID_ARC,
    SLOW_BURN,
    ENDGAME
}

/**
 * Hook record - matches TypeScript HookRecordSchema
 */
@Serializable
data class HookRecord(
    val hookId: String,
    val startChapter: Int = 0,
    val type: String,
    val status: HookStatus = HookStatus.OPEN,
    val lastAdvancedChapter: Int = 0,
    val expectedPayoff: String = "",
    val payoffTiming: HookPayoffTiming? = null,
    val notes: String = "",
    val dependsOn: List<String>? = null,
    val paysOffInArc: String? = null,
    val coreHook: Boolean? = null,
    val halfLifeChapters: Int? = null,
    val advancedCount: Int? = null,
    val promoted: Boolean? = null
)

/**
 * Hooks state - matches TypeScript HooksStateSchema
 */
@Serializable
data class HooksState(
    val hooks: List<HookRecord> = emptyList()
)

/**
 * Chapter summary row - matches TypeScript ChapterSummaryRowSchema
 */
@Serializable
data class ChapterSummaryRow(
    val chapter: Int,
    val title: String,
    val characters: String = "",
    val events: String = "",
    val stateChanges: String = "",
    val hookActivity: String = "",
    val mood: String = "",
    val chapterType: String = ""
)

/**
 * Chapter summaries state - matches TypeScript ChapterSummariesStateSchema
 */
@Serializable
data class ChapterSummariesState(
    val rows: List<ChapterSummaryRow> = emptyList()
)

/**
 * Current state fact - matches TypeScript CurrentStateFactSchema
 */
@Serializable
data class CurrentStateFact(
    val subject: String,
    val predicate: String,
    val objectValue: String,
    val validFromChapter: Int = 0,
    val validUntilChapter: Int? = null,
    val sourceChapter: Int = 0
)

/**
 * Current state state - matches TypeScript CurrentStateStateSchema
 */
@Serializable
data class CurrentStateState(
    val chapter: Int = 0,
    val facts: List<CurrentStateFact> = emptyList()
)

/**
 * Current state patch - matches TypeScript CurrentStatePatchSchema
 */
@Serializable
data class CurrentStatePatch(
    val currentLocation: String? = null,
    val protagonistState: String? = null,
    val currentGoal: String? = null,
    val currentConstraint: String? = null,
    val currentAlliances: String? = null,
    val currentConflict: String? = null
)

/**
 * Hook operations - matches TypeScript HookOpsSchema
 */
@Serializable
data class HookOps(
    val upsert: List<HookRecord> = emptyList(),
    val mention: List<String> = emptyList(),
    val resolve: List<String> = emptyList(),
    val defer: List<String> = emptyList()
)

/**
 * New hook candidate - matches TypeScript NewHookCandidateSchema
 */
@Serializable
data class NewHookCandidate(
    val type: String,
    val expectedPayoff: String = "",
    val payoffTiming: HookPayoffTiming? = null,
    val notes: String = ""
)

/**
 * Runtime state delta - matches TypeScript RuntimeStateDeltaSchema
 */
@Serializable
data class RuntimeStateDelta(
    val chapter: Int,
    val currentStatePatch: CurrentStatePatch? = null,
    val hookOps: HookOps = HookOps(),
    val newHookCandidates: List<NewHookCandidate> = emptyList(),
    val chapterSummary: ChapterSummaryRow? = null,
    val subplotOps: List<Map<String, Any?>> = emptyList(),
    val emotionalArcOps: List<Map<String, Any?>> = emptyList(),
    val characterMatrixOps: List<Map<String, Any?>> = emptyList(),
    val notes: List<String> = emptyList()
)
