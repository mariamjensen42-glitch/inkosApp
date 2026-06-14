package com.example.data.state

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * RuntimeStateStore - Manages runtime state for books.
 *
 * This is the Kotlin Android equivalent of the TypeScript RuntimeStateStore module.
 * It handles:
 * - Loading and saving runtime state snapshots
 * - Building runtime state artifacts
 * - Managing narrative memory seeds
 */

// Data classes for runtime state

data class StateManifest(
    val schemaVersion: Int = 2,
    val language: String,
    val lastAppliedChapter: Int,
    val projectionVersion: Int = 1,
    val migrationWarnings: List<String> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("schemaVersion", schemaVersion)
            put("language", language)
            put("lastAppliedChapter", lastAppliedChapter)
            put("projectionVersion", projectionVersion)
            put("migrationWarnings", JSONArray(migrationWarnings))
        }
    }

    companion object {
        fun fromJson(json: JSONObject): StateManifest {
            return StateManifest(
                schemaVersion = json.getInt("schemaVersion"),
                language = json.getString("language"),
                lastAppliedChapter = json.getInt("lastAppliedChapter"),
                projectionVersion = json.getInt("projectionVersion"),
                migrationWarnings = json.optJSONArray("migrationWarnings")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList()
            )
        }
    }
}

data class HookRecord(
    val hookId: String,
    val startChapter: Int,
    val type: String,
    val status: String, // "open", "progressing", "deferred", "resolved"
    val lastAdvancedChapter: Int,
    val expectedPayoff: String = "",
    val payoffTiming: String? = null, // "immediate", "near-term", "mid-arc", "slow-burn", "endgame"
    val notes: String = "",
    val dependsOn: List<String> = emptyList(),
    val paysOffInArc: String? = null,
    val coreHook: Boolean = false,
    val halfLifeChapters: Int? = null,
    val advancedCount: Int = 0,
    val promoted: Boolean = false
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("hookId", hookId)
            put("startChapter", startChapter)
            put("type", type)
            put("status", status)
            put("lastAdvancedChapter", lastAdvancedChapter)
            put("expectedPayoff", expectedPayoff)
            payoffTiming?.let { put("payoffTiming", it) }
            put("notes", notes)
            if (dependsOn.isNotEmpty()) {
                put("dependsOn", JSONArray(dependsOn))
            }
            paysOffInArc?.let { put("paysOffInArc", it) }
            put("coreHook", coreHook)
            halfLifeChapters?.let { put("halfLifeChapters", it) }
            put("advancedCount", advancedCount)
            put("promoted", promoted)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): HookRecord {
            return HookRecord(
                hookId = json.getString("hookId"),
                startChapter = json.getInt("startChapter"),
                type = json.getString("type"),
                status = json.getString("status"),
                lastAdvancedChapter = json.getInt("lastAdvancedChapter"),
                expectedPayoff = json.optString("expectedPayoff", ""),
                payoffTiming = json.optString("payoffTiming", null),
                notes = json.optString("notes", ""),
                dependsOn = json.optJSONArray("dependsOn")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                paysOffInArc = json.optString("paysOffInArc", null),
                coreHook = json.optBoolean("coreHook", false),
                halfLifeChapters = if (json.has("halfLifeChapters")) json.getInt("halfLifeChapters") else null,
                advancedCount = json.optInt("advancedCount", 0),
                promoted = json.optBoolean("promoted", false)
            )
        }
    }
}

data class HooksState(
    val hooks: List<HookRecord> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("hooks", JSONArray().apply {
                hooks.forEach { hook ->
                    put(hook.toJson())
                }
            })
        }
    }

    companion object {
        fun fromJson(json: JSONObject): HooksState {
            val hooksArray = json.optJSONArray("hooks") ?: JSONArray()
            return HooksState(
                hooks = (0 until hooksArray.length()).map { i ->
                    HookRecord.fromJson(hooksArray.getJSONObject(i))
                }
            )
        }
    }
}

data class ChapterSummaryRow(
    val chapter: Int,
    val title: String,
    val characters: String = "",
    val events: String = "",
    val stateChanges: String = "",
    val hookActivity: String = "",
    val mood: String = "",
    val chapterType: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("chapter", chapter)
            put("title", title)
            put("characters", characters)
            put("events", events)
            put("stateChanges", stateChanges)
            put("hookActivity", hookActivity)
            put("mood", mood)
            put("chapterType", chapterType)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ChapterSummaryRow {
            return ChapterSummaryRow(
                chapter = json.getInt("chapter"),
                title = json.getString("title"),
                characters = json.optString("characters", ""),
                events = json.optString("events", ""),
                stateChanges = json.optString("stateChanges", ""),
                hookActivity = json.optString("hookActivity", ""),
                mood = json.optString("mood", ""),
                chapterType = json.optString("chapterType", "")
            )
        }
    }
}

data class ChapterSummariesState(
    val rows: List<ChapterSummaryRow> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("rows", JSONArray().apply {
                rows.forEach { row ->
                    put(row.toJson())
                }
            })
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ChapterSummariesState {
            val rowsArray = json.optJSONArray("rows") ?: JSONArray()
            return ChapterSummariesState(
                rows = (0 until rowsArray.length()).map { i ->
                    ChapterSummaryRow.fromJson(rowsArray.getJSONObject(i))
                }
            )
        }
    }
}

data class CurrentStateFact(
    val subject: String,
    val predicate: String,
    val objectValue: String,
    val validFromChapter: Int,
    val validUntilChapter: Int? = null,
    val sourceChapter: Int
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("subject", subject)
            put("predicate", predicate)
            put("object", objectValue)
            put("validFromChapter", validFromChapter)
            validUntilChapter?.let { put("validUntilChapter", it) }
            put("sourceChapter", sourceChapter)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): CurrentStateFact {
            return CurrentStateFact(
                subject = json.getString("subject"),
                predicate = json.getString("predicate"),
                objectValue = json.getString("object"),
                validFromChapter = json.getInt("validFromChapter"),
                validUntilChapter = if (json.has("validUntilChapter") && !json.isNull("validUntilChapter")) {
                    json.getInt("validUntilChapter")
                } else null,
                sourceChapter = json.getInt("sourceChapter")
            )
        }
    }
}

data class CurrentStateState(
    val chapter: Int,
    val facts: List<CurrentStateFact> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("chapter", chapter)
            put("facts", JSONArray().apply {
                facts.forEach { fact ->
                    put(fact.toJson())
                }
            })
        }
    }

    companion object {
        fun fromJson(json: JSONObject): CurrentStateState {
            val factsArray = json.optJSONArray("facts") ?: JSONArray()
            return CurrentStateState(
                chapter = json.getInt("chapter"),
                facts = (0 until factsArray.length()).map { i ->
                    CurrentStateFact.fromJson(factsArray.getJSONObject(i))
                }
            )
        }
    }
}

data class CurrentStatePatch(
    val currentLocation: String? = null,
    val protagonistState: String? = null,
    val currentGoal: String? = null,
    val currentConstraint: String? = null,
    val currentAlliances: String? = null,
    val currentConflict: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            currentLocation?.let { put("currentLocation", it) }
            protagonistState?.let { put("protagonistState", it) }
            currentGoal?.let { put("currentGoal", it) }
            currentConstraint?.let { put("currentConstraint", it) }
            currentAlliances?.let { put("currentAlliances", it) }
            currentConflict?.let { put("currentConflict", it) }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): CurrentStatePatch {
            return CurrentStatePatch(
                currentLocation = json.optString("currentLocation", null),
                protagonistState = json.optString("protagonistState", null),
                currentGoal = json.optString("currentGoal", null),
                currentConstraint = json.optString("currentConstraint", null),
                currentAlliances = json.optString("currentAlliances", null),
                currentConflict = json.optString("currentConflict", null)
            )
        }
    }
}

data class HookOps(
    val upsert: List<HookRecord> = emptyList(),
    val mention: List<String> = emptyList(),
    val resolve: List<String> = emptyList(),
    val defer: List<String> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("upsert", JSONArray().apply {
                upsert.forEach { hook ->
                    put(hook.toJson())
                }
            })
            put("mention", JSONArray(mention))
            put("resolve", JSONArray(resolve))
            put("defer", JSONArray(defer))
        }
    }

    companion object {
        fun fromJson(json: JSONObject): HookOps {
            return HookOps(
                upsert = json.optJSONArray("upsert")?.let { array ->
                    (0 until array.length()).map { i ->
                        HookRecord.fromJson(array.getJSONObject(i))
                    }
                } ?: emptyList(),
                mention = json.optJSONArray("mention")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                resolve = json.optJSONArray("resolve")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                defer = json.optJSONArray("defer")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList()
            )
        }
    }
}

data class NewHookCandidate(
    val type: String,
    val expectedPayoff: String = "",
    val payoffTiming: String? = null,
    val notes: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("type", type)
            put("expectedPayoff", expectedPayoff)
            payoffTiming?.let { put("payoffTiming", it) }
            put("notes", notes)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): NewHookCandidate {
            return NewHookCandidate(
                type = json.getString("type"),
                expectedPayoff = json.optString("expectedPayoff", ""),
                payoffTiming = json.optString("payoffTiming", null),
                notes = json.optString("notes", "")
            )
        }
    }
}

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
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("chapter", chapter)
            currentStatePatch?.let { put("currentStatePatch", it.toJson()) }
            put("hookOps", hookOps.toJson())
            put("newHookCandidates", JSONArray().apply {
                newHookCandidates.forEach { candidate ->
                    put(candidate.toJson())
                }
            })
            chapterSummary?.let { put("chapterSummary", it.toJson()) }
            put("subplotOps", JSONArray(subplotOps))
            put("emotionalArcOps", JSONArray(emotionalArcOps))
            put("characterMatrixOps", JSONArray(characterMatrixOps))
            put("notes", JSONArray(notes))
        }
    }

    companion object {
        fun fromJson(json: JSONObject): RuntimeStateDelta {
            return RuntimeStateDelta(
                chapter = json.getInt("chapter"),
                currentStatePatch = json.optJSONObject("currentStatePatch")?.let {
                    CurrentStatePatch.fromJson(it)
                },
                hookOps = json.optJSONObject("hookOps")?.let {
                    HookOps.fromJson(it)
                } ?: HookOps(),
                newHookCandidates = json.optJSONArray("newHookCandidates")?.let { array ->
                    (0 until array.length()).map { i ->
                        NewHookCandidate.fromJson(array.getJSONObject(i))
                    }
                } ?: emptyList(),
                chapterSummary = json.optJSONObject("chapterSummary")?.let {
                    ChapterSummaryRow.fromJson(it)
                },
                notes = json.optJSONArray("notes")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList()
            )
        }
    }
}

data class RuntimeStateSnapshot(
    val manifest: StateManifest,
    val currentState: CurrentStateState,
    val hooks: HooksState,
    val chapterSummaries: ChapterSummariesState
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("manifest", manifest.toJson())
            put("currentState", currentState.toJson())
            put("hooks", hooks.toJson())
            put("chapterSummaries", chapterSummaries.toJson())
        }
    }

    companion object {
        fun fromJson(json: JSONObject): RuntimeStateSnapshot {
            return RuntimeStateSnapshot(
                manifest = StateManifest.fromJson(json.getJSONObject("manifest")),
                currentState = CurrentStateState.fromJson(json.getJSONObject("currentState")),
                hooks = HooksState.fromJson(json.getJSONObject("hooks")),
                chapterSummaries = ChapterSummariesState.fromJson(json.getJSONObject("chapterSummaries"))
            )
        }
    }
}

data class RuntimeStateArtifacts(
    val snapshot: RuntimeStateSnapshot,
    val resolvedDelta: RuntimeStateDelta,
    val currentStateMarkdown: String,
    val hooksMarkdown: String,
    val chapterSummariesMarkdown: String
)

data class NarrativeMemorySeed(
    val summaries: List<StoredSummary>,
    val hooks: List<StoredHook>
)

/**
 * RuntimeStateStore - Main class for managing runtime state.
 */
class RuntimeStateStore(private val context: Context) {

    companion object {
        private const val STATE_DIR = "state"
        private const val STORY_DIR = "story"
        private const val SNAPSHOTS_DIR = "snapshots"
    }

    suspend fun loadRuntimeStateSnapshot(bookDir: File): RuntimeStateSnapshot = withContext(Dispatchers.IO) {
        val stateDir = File(bookDir, "$STORY_DIR/$STATE_DIR")

        val manifestFile = File(stateDir, "manifest.json")
        val currentStateFile = File(stateDir, "current_state.json")
        val hooksFile = File(stateDir, "hooks.json")
        val chapterSummariesFile = File(stateDir, "chapter_summaries.json")

        val manifest = StateManifest.fromJson(JSONObject(manifestFile.readText()))
        val currentState = CurrentStateState.fromJson(JSONObject(currentStateFile.readText()))
        val hooks = HooksState.fromJson(JSONObject(hooksFile.readText()))
        val chapterSummaries = ChapterSummariesState.fromJson(JSONObject(chapterSummariesFile.readText()))

        RuntimeStateSnapshot(
            manifest = manifest,
            currentState = currentState,
            hooks = hooks,
            chapterSummaries = chapterSummaries
        )
    }

    suspend fun buildRuntimeStateArtifacts(
        bookDir: File,
        delta: RuntimeStateDelta,
        language: String,
        allowReapply: Boolean = false
    ): RuntimeStateArtifacts = withContext(Dispatchers.IO) {
        val snapshot = loadRuntimeStateSnapshot(bookDir)

        // Apply delta to snapshot
        val resolvedDelta = delta // In a full implementation, this would arbitrate hooks
        val next = applyRuntimeStateDelta(snapshot, resolvedDelta, allowReapply)

        RuntimeStateArtifacts(
            snapshot = next,
            resolvedDelta = resolvedDelta,
            currentStateMarkdown = renderCurrentStateProjection(next.currentState, language),
            hooksMarkdown = renderHooksProjection(next.hooks, language, resolvedDelta.chapter),
            chapterSummariesMarkdown = renderChapterSummariesProjection(next.chapterSummaries, language)
        )
    }

    suspend fun saveRuntimeStateSnapshot(
        bookDir: File,
        snapshot: RuntimeStateSnapshot
    ) = withContext(Dispatchers.IO) {
        val stateDir = File(bookDir, "$STORY_DIR/$STATE_DIR")
        stateDir.mkdirs()

        File(stateDir, "manifest.json").writeText(snapshot.manifest.toJson().toString(2))
        File(stateDir, "current_state.json").writeText(snapshot.currentState.toJson().toString(2))
        File(stateDir, "hooks.json").writeText(snapshot.hooks.toJson().toString(2))
        File(stateDir, "chapter_summaries.json").writeText(snapshot.chapterSummaries.toJson().toString(2))
    }

    suspend fun loadNarrativeMemorySeed(bookDir: File): NarrativeMemorySeed = withContext(Dispatchers.IO) {
        val snapshot = loadRuntimeStateSnapshot(bookDir)

        NarrativeMemorySeed(
            summaries = snapshot.chapterSummaries.rows.map { row ->
                StoredSummary(
                    chapter = row.chapter,
                    title = row.title,
                    characters = row.characters,
                    events = row.events,
                    stateChanges = row.stateChanges,
                    hookActivity = row.hookActivity,
                    mood = row.mood,
                    chapterType = row.chapterType
                )
            },
            hooks = snapshot.hooks.hooks.map { hook ->
                StoredHook(
                    hookId = hook.hookId,
                    startChapter = hook.startChapter,
                    type = hook.type,
                    status = hook.status,
                    lastAdvancedChapter = hook.lastAdvancedChapter,
                    expectedPayoff = hook.expectedPayoff,
                    payoffTiming = hook.payoffTiming,
                    notes = hook.notes,
                    dependsOn = hook.dependsOn,
                    paysOffInArc = hook.paysOffInArc,
                    coreHook = hook.coreHook,
                    halfLifeChapters = hook.halfLifeChapters,
                    advancedCount = hook.advancedCount,
                    promoted = hook.promoted
                )
            }
        )
    }

    suspend fun loadSnapshotCurrentStateFacts(
        bookDir: File,
        chapterNumber: Int
    ): List<CurrentStateFact> = withContext(Dispatchers.IO) {
        val snapshotDir = File(bookDir, "$STORY_DIR/$SNAPSHOTS_DIR/$chapterNumber")
        val structuredStateFile = File(snapshotDir, "$STATE_DIR/current_state.json")

        if (structuredStateFile.exists()) {
            val currentState = CurrentStateState.fromJson(JSONObject(structuredStateFile.readText()))
            currentState.facts
        } else {
            // Parse from markdown if structured state doesn't exist
            val markdownFile = File(snapshotDir, "current_state.md")
            if (markdownFile.exists()) {
                parseCurrentStateFacts(markdownFile.readText(), chapterNumber)
            } else {
                emptyList()
            }
        }
    }

    private fun applyRuntimeStateDelta(
        snapshot: RuntimeStateSnapshot,
        delta: RuntimeStateDelta,
        allowReapply: Boolean
    ): RuntimeStateSnapshot {
        // Apply current state patch
        val newCurrentState = if (delta.currentStatePatch != null) {
            // In a full implementation, this would merge the patch with existing state
            snapshot.currentState
        } else {
            snapshot.currentState
        }

        // Apply hook operations
        val newHooks = applyHookOps(snapshot.hooks, delta.hookOps, delta.newHookCandidates)

        // Apply chapter summary
        val newChapterSummaries = if (delta.chapterSummary != null) {
            val existingRows = snapshot.chapterSummaries.rows.toMutableList()
            val existingIndex = existingRows.indexOfFirst { it.chapter == delta.chapterSummary.chapter }
            if (existingIndex >= 0) {
                existingRows[existingIndex] = delta.chapterSummary
            } else {
                existingRows.add(delta.chapterSummary)
            }
            snapshot.chapterSummaries.copy(rows = existingRows.sortedBy { it.chapter })
        } else {
            snapshot.chapterSummaries
        }

        return snapshot.copy(
            currentState = newCurrentState,
            hooks = newHooks,
            chapterSummaries = newChapterSummaries,
            manifest = snapshot.manifest.copy(
                lastAppliedChapter = delta.chapter
            )
        )
    }

    private fun applyHookOps(
        hooksState: HooksState,
        hookOps: HookOps,
        newHookCandidates: List<NewHookCandidate>
    ): HooksState {
        val hooks = hooksState.hooks.toMutableList()

        // Upsert hooks
        hookOps.upsert.forEach { hook ->
            val existingIndex = hooks.indexOfFirst { it.hookId == hook.hookId }
            if (existingIndex >= 0) {
                hooks[existingIndex] = hook
            } else {
                hooks.add(hook)
            }
        }

        // Resolve hooks
        hookOps.resolve.forEach { hookId ->
            val index = hooks.indexOfFirst { it.hookId == hookId }
            if (index >= 0) {
                hooks[index] = hooks[index].copy(status = "resolved")
            }
        }

        // Defer hooks
        hookOps.defer.forEach { hookId ->
            val index = hooks.indexOfFirst { it.hookId == hookId }
            if (index >= 0) {
                hooks[index] = hooks[index].copy(status = "deferred")
            }
        }

        // Add new hook candidates
        newHookCandidates.forEach { candidate ->
            val newHook = HookRecord(
                hookId = "hook_${System.currentTimeMillis()}_${(Math.random() * 1000).toInt()}",
                startChapter = hooksState.hooks.firstOrNull()?.lastAdvancedChapter ?: 0,
                type = candidate.type,
                status = "open",
                lastAdvancedChapter = hooksState.hooks.firstOrNull()?.lastAdvancedChapter ?: 0,
                expectedPayoff = candidate.expectedPayoff,
                payoffTiming = candidate.payoffTiming,
                notes = candidate.notes
            )
            hooks.add(newHook)
        }

        return HooksState(hooks = hooks)
    }

    private fun renderCurrentStateProjection(currentState: CurrentStateState, language: String): String {
        // In a full implementation, this would render the current state to markdown
        return "# Current State\n\nChapter: ${currentState.chapter}\n\nFacts:\n${currentState.facts.joinToString("\n") { "- ${it.subject} ${it.predicate} ${it.objectValue}" }}"
    }

    private fun renderHooksProjection(hooks: HooksState, language: String, currentChapter: Int): String {
        // In a full implementation, this would render hooks to markdown
        return "# Hooks\n\n${hooks.hooks.joinToString("\n") { "- [${it.status}] ${it.hookId}: ${it.expectedPayoff}" }}"
    }

    private fun renderChapterSummariesProjection(summaries: ChapterSummariesState, language: String): String {
        // In a full implementation, this would render chapter summaries to markdown
        return "# Chapter Summaries\n\n${summaries.rows.joinToString("\n") { "- Chapter ${it.chapter}: ${it.title}" }}"
    }

    private fun parseCurrentStateFacts(markdown: String, chapterNumber: Int): List<CurrentStateFact> {
        // Simple parser for current state facts from markdown
        val facts = mutableListOf<CurrentStateFact>()
        val lines = markdown.lines()

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("- ") && trimmed.contains(":")) {
                val parts = trimmed.substring(2).split(":", limit = 2)
                if (parts.size == 2) {
                    facts.add(
                        CurrentStateFact(
                            subject = parts[0].trim(),
                            predicate = "is",
                            objectValue = parts[1].trim(),
                            validFromChapter = chapterNumber,
                            sourceChapter = chapterNumber
                        )
                    )
                }
            }
        }

        return facts
    }
}
