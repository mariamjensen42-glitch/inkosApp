package com.example.data.models

import kotlinx.serialization.Serializable

/**
 * Chapter memo - matches TypeScript ChapterMemoSchema
 */
@Serializable
data class ChapterMemo(
    val chapter: Int,
    val goal: String,
    val isGoldenOpening: Boolean = false,
    val body: String,
    val threadRefs: List<String> = emptyList()
)

/**
 * Chapter intent - matches TypeScript ChapterIntentSchema
 */
@Serializable
data class ChapterIntent(
    val chapter: Int,
    val goal: String,
    val outlineNode: String? = null,
    val arcContext: String? = null,
    val mustKeep: List<String> = emptyList(),
    val mustAvoid: List<String> = emptyList(),
    val styleEmphasis: List<String> = emptyList()
)

/**
 * Context source - matches TypeScript ContextSourceSchema
 */
@Serializable
data class ContextSource(
    val source: String,
    val reason: String,
    val excerpt: String? = null
)

/**
 * Context package - matches TypeScript ContextPackageSchema
 */
@Serializable
data class ContextPackage(
    val chapter: Int,
    val selectedContext: List<ContextSource> = emptyList()
)

/**
 * Rule layer scope - matches TypeScript RuleLayerScopeSchema
 */
@Serializable
enum class RuleLayerScope {
    GLOBAL,
    BOOK,
    ARC,
    LOCAL
}

/**
 * Rule layer - matches TypeScript RuleLayerSchema
 */
@Serializable
data class RuleLayer(
    val id: String,
    val name: String,
    val precedence: Int,
    val scope: RuleLayerScope
)

/**
 * Override edge - matches TypeScript OverrideEdgeSchema
 */
@Serializable
data class OverrideEdge(
    val from: String,
    val to: String,
    val allowed: Boolean,
    val scope: String
)

/**
 * Active override - matches TypeScript ActiveOverrideSchema
 */
@Serializable
data class ActiveOverride(
    val from: String,
    val to: String,
    val target: String,
    val reason: String
)

/**
 * Rule stack sections - matches TypeScript RuleStackSectionsSchema
 */
@Serializable
data class RuleStackSections(
    val hard: List<String> = emptyList(),
    val soft: List<String> = emptyList(),
    val diagnostic: List<String> = emptyList()
)

/**
 * Rule stack - matches TypeScript RuleStackSchema
 */
@Serializable
data class RuleStack(
    val layers: List<RuleLayer>,
    val sections: RuleStackSections = RuleStackSections(),
    val overrideEdges: List<OverrideEdge> = emptyList(),
    val activeOverrides: List<ActiveOverride> = emptyList()
)

/**
 * Chapter trace - matches TypeScript ChapterTraceSchema
 */
@Serializable
data class ChapterTrace(
    val chapter: Int,
    val plannerInputs: List<String>,
    val composerInputs: List<String>,
    val selectedSources: List<String>,
    val contextTiers: ContextTiers = ContextTiers(),
    val tokenBudget: TokenBudget = TokenBudget(),
    val notes: List<String> = emptyList()
)

/**
 * Context tiers
 */
@Serializable
data class ContextTiers(
    val protectedSources: List<String> = emptyList(),
    val compressibleSources: List<String> = emptyList()
)

/**
 * Token budget
 */
@Serializable
data class TokenBudget(
    val protectedTokens: Int = 0,
    val compressibleTokens: Int = 0,
    val totalSelectedTokens: Int = 0
)
