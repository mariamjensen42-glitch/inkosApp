package com.example.data.models

import org.json.JSONObject

/**
 * ContextCompression - Context compression event models.
 *
 * This is the Kotlin Android equivalent of the TypeScript context-compression.ts models.
 * It contains:
 * - ContextCompressionEvent - Context compression event
 * - ContextCompressionCategory - Compression category
 * - ContextCompressionPhase - Compression phase
 */

enum class ContextCompressionCategory {
    SESSION_CONTEXT,
    STORY_CONTEXT
}

enum class ContextCompressionPhase {
    START,
    END,
    ERROR
}

data class ContextCompressionEvent(
    val category: ContextCompressionCategory,
    val phase: ContextCompressionPhase,
    val message: String? = null,
    val protectedTokens: Int? = null,
    val compressibleTokens: Int? = null,
    val budgetTokens: Int? = null,
    val sources: List<String>? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("category", category.name.lowercase())
            put("phase", phase.name.lowercase())
            message?.let { put("message", it) }
            protectedTokens?.let { put("protectedTokens", it) }
            compressibleTokens?.let { put("compressibleTokens", it) }
            budgetTokens?.let { put("budgetTokens", it) }
            sources?.let { put("sources", org.json.JSONArray(it)) }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ContextCompressionEvent {
            return ContextCompressionEvent(
                category = ContextCompressionCategory.valueOf(json.getString("category").uppercase()),
                phase = ContextCompressionPhase.valueOf(json.getString("phase").uppercase()),
                message = json.optString("message", null),
                protectedTokens = if (json.has("protectedTokens") && !json.isNull("protectedTokens")) json.getInt("protectedTokens") else null,
                compressibleTokens = if (json.has("compressibleTokens") && !json.isNull("compressibleTokens")) json.getInt("compressibleTokens") else null,
                budgetTokens = if (json.has("budgetTokens") && !json.isNull("budgetTokens")) json.getInt("budgetTokens") else null,
                sources = json.optJSONArray("sources")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                }
            )
        }
    }
}

typealias ContextCompressionCallback = (ContextCompressionEvent) -> Unit
