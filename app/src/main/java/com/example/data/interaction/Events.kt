package com.example.data.interaction

import org.json.JSONObject

/**
 * Events - Event definitions for the interaction system.
 *
 * This is the Kotlin Android equivalent of the TypeScript events.ts module.
 * It contains:
 * - ExecutionStatus - Execution status enum
 * - ExecutionState - Execution state
 * - InteractionEvent - Interaction event
 */

enum class ExecutionStatus {
    IDLE,
    PLANNING,
    COMPOSING,
    WRITING,
    ASSESSING,
    REPAIRING,
    PERSISTING,
    WAITING_HUMAN,
    BLOCKED,
    COMPLETED,
    FAILED;

    companion object {
        fun fromString(value: String): ExecutionStatus {
            return when (value.lowercase()) {
                "idle" -> IDLE
                "planning" -> PLANNING
                "composing" -> COMPOSING
                "writing" -> WRITING
                "assessing" -> ASSESSING
                "repairing" -> REPAIRING
                "persisting" -> PERSISTING
                "waiting_human" -> WAITING_HUMAN
                "blocked" -> BLOCKED
                "completed" -> COMPLETED
                "failed" -> FAILED
                else -> IDLE
            }
        }
    }
}

data class ExecutionState(
    val status: ExecutionStatus,
    val bookId: String? = null,
    val chapterNumber: Int? = null,
    val stageLabel: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("status", status.name.lowercase())
            bookId?.let { put("bookId", it) }
            chapterNumber?.let { put("chapterNumber", it) }
            stageLabel?.let { put("stageLabel", it) }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ExecutionState {
            return ExecutionState(
                status = ExecutionStatus.fromString(json.getString("status")),
                bookId = json.optString("bookId", null),
                chapterNumber = if (json.has("chapterNumber") && !json.isNull("chapterNumber")) json.getInt("chapterNumber") else null,
                stageLabel = json.optString("stageLabel", null)
            )
        }
    }
}

data class InteractionEvent(
    val kind: String,
    val timestamp: Long,
    val status: ExecutionStatus,
    val bookId: String? = null,
    val chapterNumber: Int? = null,
    val detail: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("kind", kind)
            put("timestamp", timestamp)
            put("status", status.name.lowercase())
            bookId?.let { put("bookId", it) }
            chapterNumber?.let { put("chapterNumber", it) }
            detail?.let { put("detail", it) }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): InteractionEvent {
            return InteractionEvent(
                kind = json.getString("kind"),
                timestamp = json.getLong("timestamp"),
                status = ExecutionStatus.fromString(json.getString("status")),
                bookId = json.optString("bookId", null),
                chapterNumber = if (json.has("chapterNumber") && !json.isNull("chapterNumber")) json.getInt("chapterNumber") else null,
                detail = json.optString("detail", null)
            )
        }
    }
}

fun isTerminalExecutionStatus(status: ExecutionStatus): Boolean {
    return status == ExecutionStatus.COMPLETED || status == ExecutionStatus.FAILED || status == ExecutionStatus.BLOCKED
}
