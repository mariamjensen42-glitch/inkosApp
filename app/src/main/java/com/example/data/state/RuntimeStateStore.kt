package com.example.data.state

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * RuntimeStateStore - Manages runtime state for books.
 */

data class RuntimeStateSnapshot(
    val manifest: StateManifestData,
    val currentState: CurrentStateData,
    val hooks: HooksData,
    val chapterSummaries: ChapterSummariesData
)

data class StateManifestData(
    val schemaVersion: Int = 2,
    val language: String,
    val lastAppliedChapter: Int
)

data class CurrentStateData(
    val chapter: Int,
    val facts: List<CurrentStateFactData> = emptyList()
)

data class CurrentStateFactData(
    val subject: String,
    val predicate: String,
    val objectValue: String,
    val validFromChapter: Int,
    val validUntilChapter: Int? = null,
    val sourceChapter: Int
)

data class HooksData(
    val hooks: List<HookRecordData> = emptyList()
)

data class HookRecordData(
    val hookId: String,
    val startChapter: Int,
    val type: String,
    val status: String,
    val lastAdvancedChapter: Int,
    val expectedPayoff: String = "",
    val payoffTiming: String? = null,
    val notes: String = ""
)

data class ChapterSummariesData(
    val rows: List<ChapterSummaryRowData> = emptyList()
)

data class ChapterSummaryRowData(
    val chapter: Int,
    val title: String,
    val characters: String = "",
    val events: String = "",
    val stateChanges: String = "",
    val hookActivity: String = "",
    val mood: String = "",
    val chapterType: String = ""
)

class RuntimeStateStore(private val context: Context) {

    suspend fun loadRuntimeStateSnapshot(bookDir: File): RuntimeStateSnapshot = withContext(Dispatchers.IO) {
        val stateDir = File(bookDir, "story/state")

        val manifestFile = File(stateDir, "manifest.json")
        val currentStateFile = File(stateDir, "current_state.json")
        val hooksFile = File(stateDir, "hooks.json")
        val chapterSummariesFile = File(stateDir, "chapter_summaries.json")

        val manifest = if (manifestFile.exists()) {
            val json = JSONObject(manifestFile.readText())
            StateManifestData(
                schemaVersion = json.optInt("schemaVersion", 2),
                language = json.optString("language", "zh"),
                lastAppliedChapter = json.optInt("lastAppliedChapter", 0)
            )
        } else {
            StateManifestData(language = "zh", lastAppliedChapter = 0)
        }

        val currentState = if (currentStateFile.exists()) {
            val json = JSONObject(currentStateFile.readText())
            val factsArray = json.optJSONArray("facts") ?: org.json.JSONArray()
            val facts = (0 until factsArray.length()).map { i ->
                val factJson = factsArray.getJSONObject(i)
                CurrentStateFactData(
                    subject = factJson.getString("subject"),
                    predicate = factJson.getString("predicate"),
                    objectValue = factJson.getString("object"),
                    validFromChapter = factJson.getInt("validFromChapter"),
                    validUntilChapter = if (factJson.has("validUntilChapter") && !factJson.isNull("validUntilChapter")) factJson.getInt("validUntilChapter") else null,
                    sourceChapter = factJson.getInt("sourceChapter")
                )
            }
            CurrentStateData(chapter = json.getInt("chapter"), facts = facts)
        } else {
            CurrentStateData(chapter = 0)
        }

        val hooks = if (hooksFile.exists()) {
            val json = JSONObject(hooksFile.readText())
            val hooksArray = json.optJSONArray("hooks") ?: org.json.JSONArray()
            val hooksList = (0 until hooksArray.length()).map { i ->
                val hookJson = hooksArray.getJSONObject(i)
                HookRecordData(
                    hookId = hookJson.getString("hookId"),
                    startChapter = hookJson.getInt("startChapter"),
                    type = hookJson.getString("type"),
                    status = hookJson.getString("status"),
                    lastAdvancedChapter = hookJson.getInt("lastAdvancedChapter"),
                    expectedPayoff = hookJson.optString("expectedPayoff", ""),
                    payoffTiming = hookJson.optString("payoffTiming", null),
                    notes = hookJson.optString("notes", "")
                )
            }
            HooksData(hooks = hooksList)
        } else {
            HooksData()
        }

        val chapterSummaries = if (chapterSummariesFile.exists()) {
            val json = JSONObject(chapterSummariesFile.readText())
            val rowsArray = json.optJSONArray("rows") ?: org.json.JSONArray()
            val rows = (0 until rowsArray.length()).map { i ->
                val rowJson = rowsArray.getJSONObject(i)
                ChapterSummaryRowData(
                    chapter = rowJson.getInt("chapter"),
                    title = rowJson.getString("title"),
                    characters = rowJson.optString("characters", ""),
                    events = rowJson.optString("events", ""),
                    stateChanges = rowJson.optString("stateChanges", ""),
                    hookActivity = rowJson.optString("hookActivity", ""),
                    mood = rowJson.optString("mood", ""),
                    chapterType = rowJson.optString("chapterType", "")
                )
            }
            ChapterSummariesData(rows = rows)
        } else {
            ChapterSummariesData()
        }

        RuntimeStateSnapshot(
            manifest = manifest,
            currentState = currentState,
            hooks = hooks,
            chapterSummaries = chapterSummaries
        )
    }

    suspend fun saveRuntimeStateSnapshot(bookDir: File, snapshot: RuntimeStateSnapshot) = withContext(Dispatchers.IO) {
        val stateDir = File(bookDir, "story/state")
        stateDir.mkdirs()

        val manifestJson = JSONObject().apply {
            put("schemaVersion", snapshot.manifest.schemaVersion)
            put("language", snapshot.manifest.language)
            put("lastAppliedChapter", snapshot.manifest.lastAppliedChapter)
        }
        File(stateDir, "manifest.json").writeText(manifestJson.toString(2))

        val currentStateJson = JSONObject().apply {
            put("chapter", snapshot.currentState.chapter)
            put("facts", org.json.JSONArray().apply {
                snapshot.currentState.facts.forEach { fact ->
                    put(JSONObject().apply {
                        put("subject", fact.subject)
                        put("predicate", fact.predicate)
                        put("object", fact.objectValue)
                        put("validFromChapter", fact.validFromChapter)
                        fact.validUntilChapter?.let { put("validUntilChapter", it) }
                        put("sourceChapter", fact.sourceChapter)
                    })
                }
            })
        }
        File(stateDir, "current_state.json").writeText(currentStateJson.toString(2))

        val hooksJson = JSONObject().apply {
            put("hooks", org.json.JSONArray().apply {
                snapshot.hooks.hooks.forEach { hook ->
                    put(JSONObject().apply {
                        put("hookId", hook.hookId)
                        put("startChapter", hook.startChapter)
                        put("type", hook.type)
                        put("status", hook.status)
                        put("lastAdvancedChapter", hook.lastAdvancedChapter)
                        put("expectedPayoff", hook.expectedPayoff)
                        hook.payoffTiming?.let { put("payoffTiming", it) }
                        put("notes", hook.notes)
                    })
                }
            })
        }
        File(stateDir, "hooks.json").writeText(hooksJson.toString(2))

        val summariesJson = JSONObject().apply {
            put("rows", org.json.JSONArray().apply {
                snapshot.chapterSummaries.rows.forEach { row ->
                    put(JSONObject().apply {
                        put("chapter", row.chapter)
                        put("title", row.title)
                        put("characters", row.characters)
                        put("events", row.events)
                        put("stateChanges", row.stateChanges)
                        put("hookActivity", row.hookActivity)
                        put("mood", row.mood)
                        put("chapterType", row.chapterType)
                    })
                }
            })
        }
        File(stateDir, "chapter_summaries.json").writeText(summariesJson.toString(2))
    }
}
