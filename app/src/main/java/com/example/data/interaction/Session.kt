package com.example.data.interaction

import org.json.JSONObject

/**
 * Session - Session management for the interaction system.
 *
 * This is the Kotlin Android equivalent of the TypeScript session.ts module.
 * It contains:
 * - SessionKind - Session kind enum
 * - PlayMode - Play mode enum
 * - PendingDecision - Pending decision
 * - PipelineStage - Pipeline stage
 * - ToolExecution - Tool execution
 * - InteractionMessage - Interaction message
 * - BookCreationDraft - Book creation draft
 * - DraftRound - Draft round
 * - InteractionSession - Interaction session
 * - BookSession - Book session
 * - GlobalSession - Global session
 */

enum class SessionKind {
    CHAT,
    BOOK_CREATE,
    BOOK,
    SHORT,
    PLAY,
    EDIT;

    companion object {
        fun fromString(value: String): SessionKind {
            return when (value.lowercase()) {
                "chat" -> CHAT
                "book_create", "book-create" -> BOOK_CREATE
                "book" -> BOOK
                "short" -> SHORT
                "play" -> PLAY
                "edit" -> EDIT
                else -> CHAT
            }
        }
    }
}

enum class PlayMode {
    OPEN,
    GUIDED;

    companion object {
        fun fromString(value: String): PlayMode {
            return when (value.lowercase()) {
                "open" -> OPEN
                "guided" -> GUIDED
                else -> OPEN
            }
        }
    }
}

data class PendingDecision(
    val kind: String,
    val bookId: String,
    val chapterNumber: Int? = null,
    val summary: String
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("kind", kind)
            put("bookId", bookId)
            chapterNumber?.let { put("chapterNumber", it) }
            put("summary", summary)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): PendingDecision {
            return PendingDecision(
                kind = json.getString("kind"),
                bookId = json.getString("bookId"),
                chapterNumber = if (json.has("chapterNumber") && !json.isNull("chapterNumber")) json.getInt("chapterNumber") else null,
                summary = json.getString("summary")
            )
        }
    }
}

data class PipelineStage(
    val label: String,
    val status: String // "pending", "active", "completed"
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("label", label)
            put("status", status)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): PipelineStage {
            return PipelineStage(
                label = json.getString("label"),
                status = json.getString("status")
            )
        }
    }
}

data class ToolExecution(
    val id: String,
    val tool: String,
    val agent: String? = null,
    val label: String,
    val status: String, // "running", "processing", "completed", "error"
    val args: Map<String, Any?>? = null,
    val result: String? = null,
    val details: Any? = null,
    val error: String? = null,
    val stages: List<PipelineStage>? = null,
    val startedAt: Long,
    val completedAt: Long? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("tool", tool)
            agent?.let { put("agent", it) }
            put("label", label)
            put("status", status)
            args?.let { put("args", JSONObject(it)) }
            result?.let { put("result", it) }
            details?.let { put("details", it) }
            error?.let { put("error", it) }
            stages?.let {
                put("stages", org.json.JSONArray().apply {
                    it.forEach { stage -> put(stage.toJson()) }
                })
            }
            put("startedAt", startedAt)
            completedAt?.let { put("completedAt", it) }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ToolExecution {
            return ToolExecution(
                id = json.getString("id"),
                tool = json.getString("tool"),
                agent = json.optString("agent", null),
                label = json.getString("label"),
                status = json.getString("status"),
                args = json.optJSONObject("args")?.let { obj ->
                    val map = mutableMapOf<String, Any?>()
                    obj.keys().forEach { key -> map[key] = obj.get(key) }
                    map
                },
                result = json.optString("result", null),
                details = json.opt("details"),
                error = json.optString("error", null),
                stages = json.optJSONArray("stages")?.let { array ->
                    (0 until array.length()).map { i ->
                        PipelineStage.fromJson(array.getJSONObject(i))
                    }
                },
                startedAt = json.getLong("startedAt"),
                completedAt = if (json.has("completedAt") && !json.isNull("completedAt")) json.getLong("completedAt") else null
            )
        }
    }
}

data class InteractionMessage(
    val role: String, // "user", "assistant", "system"
    val content: String,
    val thinking: String? = null,
    val toolExecutions: List<ToolExecution>? = null,
    val timestamp: Long
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("role", role)
            put("content", content)
            thinking?.let { put("thinking", it) }
            toolExecutions?.let {
                put("toolExecutions", org.json.JSONArray().apply {
                    it.forEach { execution -> put(execution.toJson()) }
                })
            }
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): InteractionMessage {
            return InteractionMessage(
                role = json.getString("role"),
                content = json.getString("content"),
                thinking = json.optString("thinking", null),
                toolExecutions = json.optJSONArray("toolExecutions")?.let { array ->
                    (0 until array.length()).map { i ->
                        ToolExecution.fromJson(array.getJSONObject(i))
                    }
                },
                timestamp = json.getLong("timestamp")
            )
        }
    }
}

data class BookCreationDraft(
    val concept: String,
    val title: String? = null,
    val genre: String? = null,
    val platform: String? = null,
    val language: String? = null, // "zh", "en"
    val targetChapters: Int? = null,
    val chapterWordCount: Int? = null,
    val blurb: String? = null,
    val worldPremise: String? = null,
    val settingNotes: String? = null,
    val protagonist: String? = null,
    val supportingCast: String? = null,
    val conflictCore: String? = null,
    val volumeOutline: String? = null,
    val constraints: String? = null,
    val authorIntent: String? = null,
    val currentFocus: String? = null,
    val nextQuestion: String? = null,
    val missingFields: List<String> = emptyList(),
    val readyToCreate: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("concept", concept)
            title?.let { put("title", it) }
            genre?.let { put("genre", it) }
            platform?.let { put("platform", it) }
            language?.let { put("language", it) }
            targetChapters?.let { put("targetChapters", it) }
            chapterWordCount?.let { put("chapterWordCount", it) }
            blurb?.let { put("blurb", it) }
            worldPremise?.let { put("worldPremise", it) }
            settingNotes?.let { put("settingNotes", it) }
            protagonist?.let { put("protagonist", it) }
            supportingCast?.let { put("supportingCast", it) }
            conflictCore?.let { put("conflictCore", it) }
            volumeOutline?.let { put("volumeOutline", it) }
            constraints?.let { put("constraints", it) }
            authorIntent?.let { put("authorIntent", it) }
            currentFocus?.let { put("currentFocus", it) }
            nextQuestion?.let { put("nextQuestion", it) }
            put("missingFields", org.json.JSONArray(missingFields))
            put("readyToCreate", readyToCreate)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): BookCreationDraft {
            return BookCreationDraft(
                concept = json.getString("concept"),
                title = json.optString("title", null),
                genre = json.optString("genre", null),
                platform = json.optString("platform", null),
                language = json.optString("language", null),
                targetChapters = if (json.has("targetChapters") && !json.isNull("targetChapters")) json.getInt("targetChapters") else null,
                chapterWordCount = if (json.has("chapterWordCount") && !json.isNull("chapterWordCount")) json.getInt("chapterWordCount") else null,
                blurb = json.optString("blurb", null),
                worldPremise = json.optString("worldPremise", null),
                settingNotes = json.optString("settingNotes", null),
                protagonist = json.optString("protagonist", null),
                supportingCast = json.optString("supportingCast", null),
                conflictCore = json.optString("conflictCore", null),
                volumeOutline = json.optString("volumeOutline", null),
                constraints = json.optString("constraints", null),
                authorIntent = json.optString("authorIntent", null),
                currentFocus = json.optString("currentFocus", null),
                nextQuestion = json.optString("nextQuestion", null),
                missingFields = json.optJSONArray("missingFields")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                readyToCreate = json.optBoolean("readyToCreate", false)
            )
        }
    }
}

data class DraftRound(
    val roundId: Int,
    val userMessage: String,
    val assistantRaw: String,
    val fieldsUpdated: List<String> = emptyList(),
    val summary: String = "",
    val timestamp: Long
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("roundId", roundId)
            put("userMessage", userMessage)
            put("assistantRaw", assistantRaw)
            put("fieldsUpdated", org.json.JSONArray(fieldsUpdated))
            put("summary", summary)
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DraftRound {
            return DraftRound(
                roundId = json.getInt("roundId"),
                userMessage = json.getString("userMessage"),
                assistantRaw = json.getString("assistantRaw"),
                fieldsUpdated = json.optJSONArray("fieldsUpdated")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                summary = json.optString("summary", ""),
                timestamp = json.getLong("timestamp")
            )
        }
    }
}

data class InteractionSession(
    val sessionId: String,
    val projectRoot: String,
    val activeBookId: String? = null,
    val activeChapterNumber: Int? = null,
    val creationDraft: BookCreationDraft? = null,
    val draftRounds: List<DraftRound> = emptyList(),
    val automationMode: AutomationMode = AutomationMode.SEMI,
    val messages: List<InteractionMessage> = emptyList(),
    val events: List<InteractionEvent> = emptyList(),
    val pendingDecision: PendingDecision? = null,
    val currentExecution: ExecutionState? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("sessionId", sessionId)
            put("projectRoot", projectRoot)
            activeBookId?.let { put("activeBookId", it) }
            activeChapterNumber?.let { put("activeChapterNumber", it) }
            creationDraft?.let { put("creationDraft", it.toJson()) }
            put("draftRounds", org.json.JSONArray().apply {
                draftRounds.forEach { round -> put(round.toJson()) }
            })
            put("automationMode", automationMode.name.lowercase())
            put("messages", org.json.JSONArray().apply {
                messages.forEach { message -> put(message.toJson()) }
            })
            put("events", org.json.JSONArray().apply {
                events.forEach { event -> put(event.toJson()) }
            })
            pendingDecision?.let { put("pendingDecision", it.toJson()) }
            currentExecution?.let { put("currentExecution", it.toJson()) }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): InteractionSession {
            return InteractionSession(
                sessionId = json.getString("sessionId"),
                projectRoot = json.getString("projectRoot"),
                activeBookId = json.optString("activeBookId", null),
                activeChapterNumber = if (json.has("activeChapterNumber") && !json.isNull("activeChapterNumber")) json.getInt("activeChapterNumber") else null,
                creationDraft = json.optJSONObject("creationDraft")?.let { BookCreationDraft.fromJson(it) },
                draftRounds = json.optJSONArray("draftRounds")?.let { array ->
                    (0 until array.length()).map { i ->
                        DraftRound.fromJson(array.getJSONObject(i))
                    }
                } ?: emptyList(),
                automationMode = json.optString("automationMode", "semi").let { AutomationMode.fromString(it) },
                messages = json.optJSONArray("messages")?.let { array ->
                    (0 until array.length()).map { i ->
                        InteractionMessage.fromJson(array.getJSONObject(i))
                    }
                } ?: emptyList(),
                events = json.optJSONArray("events")?.let { array ->
                    (0 until array.length()).map { i ->
                        InteractionEvent.fromJson(array.getJSONObject(i))
                    }
                } ?: emptyList(),
                pendingDecision = json.optJSONObject("pendingDecision")?.let { PendingDecision.fromJson(it) },
                currentExecution = json.optJSONObject("currentExecution")?.let { ExecutionState.fromJson(it) }
            )
        }
    }
}

data class BookSession(
    val sessionId: String,
    val bookId: String?,
    val sessionKind: SessionKind? = null,
    val playMode: PlayMode? = null,
    val title: String? = null,
    val messages: List<InteractionMessage> = emptyList(),
    val creationDraft: BookCreationDraft? = null,
    val draftRounds: List<DraftRound> = emptyList(),
    val events: List<InteractionEvent> = emptyList(),
    val currentExecution: ExecutionState? = null,
    val createdAt: Long,
    val updatedAt: Long
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("sessionId", sessionId)
            put("bookId", bookId)
            sessionKind?.let { put("sessionKind", it.name.lowercase()) }
            playMode?.let { put("playMode", it.name.lowercase()) }
            title?.let { put("title", it) }
            put("messages", org.json.JSONArray().apply {
                messages.forEach { message -> put(message.toJson()) }
            })
            creationDraft?.let { put("creationDraft", it.toJson()) }
            put("draftRounds", org.json.JSONArray().apply {
                draftRounds.forEach { round -> put(round.toJson()) }
            })
            put("events", org.json.JSONArray().apply {
                events.forEach { event -> put(event.toJson()) }
            })
            currentExecution?.let { put("currentExecution", it.toJson()) }
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): BookSession {
            return BookSession(
                sessionId = json.getString("sessionId"),
                bookId = json.optString("bookId", null),
                sessionKind = json.optString("sessionKind", null)?.let { SessionKind.fromString(it) },
                playMode = json.optString("playMode", null)?.let { PlayMode.fromString(it) },
                title = json.optString("title", null),
                messages = json.optJSONArray("messages")?.let { array ->
                    (0 until array.length()).map { i ->
                        InteractionMessage.fromJson(array.getJSONObject(i))
                    }
                } ?: emptyList(),
                creationDraft = json.optJSONObject("creationDraft")?.let { BookCreationDraft.fromJson(it) },
                draftRounds = json.optJSONArray("draftRounds")?.let { array ->
                    (0 until array.length()).map { i ->
                        DraftRound.fromJson(array.getJSONObject(i))
                    }
                } ?: emptyList(),
                events = json.optJSONArray("events")?.let { array ->
                    (0 until array.length()).map { i ->
                        InteractionEvent.fromJson(array.getJSONObject(i))
                    }
                } ?: emptyList(),
                currentExecution = json.optJSONObject("currentExecution")?.let { ExecutionState.fromJson(it) },
                createdAt = json.getLong("createdAt"),
                updatedAt = json.getLong("updatedAt")
            )
        }
    }
}

data class GlobalSession(
    val activeBookId: String? = null,
    val automationMode: AutomationMode = AutomationMode.SEMI
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            activeBookId?.let { put("activeBookId", it) }
            put("automationMode", automationMode.name.lowercase())
        }
    }

    companion object {
        fun fromJson(json: JSONObject): GlobalSession {
            return GlobalSession(
                activeBookId = json.optString("activeBookId", null),
                automationMode = json.optString("automationMode", "semi").let { AutomationMode.fromString(it) }
            )
        }
    }
}

fun createBookSession(
    bookId: String?,
    sessionId: String? = null,
    sessionKind: SessionKind? = null,
    playMode: PlayMode? = null
): BookSession {
    val now = System.currentTimeMillis()
    return BookSession(
        sessionId = sessionId ?: "$now-${(Math.random() * 1000000).toInt().toString().padStart(6, '0')}",
        bookId = bookId,
        sessionKind = sessionKind,
        playMode = playMode,
        title = null,
        messages = emptyList(),
        draftRounds = emptyList(),
        events = emptyList(),
        createdAt = now,
        updatedAt = now
    )
}

fun appendBookSessionMessage(
    session: BookSession,
    message: InteractionMessage
): BookSession {
    return session.copy(
        messages = (session.messages + message).sortedBy { it.timestamp },
        updatedAt = System.currentTimeMillis()
    )
}

fun bindActiveBook(
    session: InteractionSession,
    bookId: String,
    chapterNumber: Int? = null
): InteractionSession {
    return session.copy(
        activeBookId = bookId,
        activeChapterNumber = chapterNumber ?: session.activeChapterNumber
    )
}

fun clearPendingDecision(session: InteractionSession): InteractionSession {
    if (session.pendingDecision == null) {
        return session
    }

    return session.copy(pendingDecision = null)
}

fun updateCreationDraft(
    session: InteractionSession,
    draft: BookCreationDraft
): InteractionSession {
    return session.copy(creationDraft = draft)
}

fun clearCreationDraft(session: InteractionSession): InteractionSession {
    if (session.creationDraft == null) {
        return session
    }

    return session.copy(
        creationDraft = null,
        draftRounds = emptyList()
    )
}

fun updateAutomationMode(
    session: InteractionSession,
    automationMode: AutomationMode
): InteractionSession {
    return session.copy(automationMode = automationMode)
}

fun appendInteractionMessage(
    session: InteractionSession,
    message: InteractionMessage
): InteractionSession {
    return session.copy(
        messages = (session.messages + message).sortedBy { it.timestamp }
    )
}

fun appendInteractionEvent(
    session: InteractionSession,
    event: InteractionEvent
): InteractionSession {
    return session.copy(
        events = session.events + event
    )
}
