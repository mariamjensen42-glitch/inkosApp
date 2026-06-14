package com.example.data.play

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * PlayStore - Manages play worlds and runs.
 *
 * This is the Kotlin Android equivalent of the TypeScript PlayStore class.
 * It handles:
 * - Creating and managing play worlds
 * - Creating and managing play runs
 * - Saving and loading play state
 * - Managing transcripts
 */

// Data classes

data class PlayWorld(
    val id: String,
    val title: String,
    val premise: String = "",
    val worldContract: String = "",
    val visualContract: String = "",
    val mode: String = "open", // "open", "guided"
    val language: String = "zh", // "zh", "en"
    val createdAt: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
    val updatedAt: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("premise", premise)
            put("worldContract", worldContract)
            put("visualContract", visualContract)
            put("mode", mode)
            put("language", language)
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): PlayWorld {
            return PlayWorld(
                id = json.getString("id"),
                title = json.getString("title"),
                premise = json.optString("premise", ""),
                worldContract = json.optString("worldContract", ""),
                visualContract = json.optString("visualContract", ""),
                mode = json.optString("mode", "open"),
                language = json.optString("language", "zh"),
                createdAt = json.optString("createdAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now())),
                updatedAt = json.optString("updatedAt", DateTimeFormatter.ISO_INSTANT.format(Instant.now()))
            )
        }
    }
}

data class PlayTranscriptTurn(
    val role: String, // "user", "assistant", "system", "tool"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("role", role)
            put("content", content)
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): PlayTranscriptTurn {
            return PlayTranscriptTurn(
                role = json.getString("role"),
                content = json.getString("content"),
                timestamp = json.optLong("timestamp", System.currentTimeMillis())
            )
        }
    }
}

data class PlayRunSummary(
    val id: String,
    val updatedAt: String,
    val eventCount: Int,
    val transcriptCount: Int
)

data class PlayRunSnapshot(
    val id: String,
    val turn: Int,
    val createdAt: String,
    val events: List<PlayEvent>,
    val transcript: List<PlayTranscriptTurn>,
    val currentState: PlayRunState,
    val sceneProjection: String,
    val stateProjection: String,
    val graph: PlayGraphSnapshot
)

/**
 * PlayStore - Main class for managing play worlds and runs.
 */
class PlayStore(private val context: Context, private val projectRoot: File) {

    companion object {
        private const val WORLDS_DIR = "worlds"
    }

    fun worldDir(worldId: String): File {
        return File(projectRoot, "$WORLDS_DIR/$worldId")
    }

    fun runDir(worldId: String, runId: String): File {
        return File(worldDir(worldId), "runs/$runId")
    }

    suspend fun ensureWorld(worldId: String) = withContext(Dispatchers.IO) {
        worldDir(worldId).mkdirs()
    }

    suspend fun createWorld(world: PlayWorld): PlayWorld = withContext(Dispatchers.IO) {
        val safeWorld = world.copy(
            id = assertSafeSegment(world.id),
            createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            updatedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        )

        ensureWorld(safeWorld.id)

        val worldFile = File(worldDir(safeWorld.id), "world.json")
        worldFile.writeText(safeWorld.toJson().toString(2))

        safeWorld
    }

    suspend fun updateWorld(worldId: String, patch: Map<String, Any?>): PlayWorld = withContext(Dispatchers.IO) {
        val current = loadWorld(worldId)
            ?: throw IllegalStateException("Play world not found: $worldId")

        val updated = current.copy(
            premise = patch["premise"] as? String ?: current.premise,
            worldContract = patch["worldContract"] as? String ?: current.worldContract,
            visualContract = patch["visualContract"] as? String ?: current.visualContract,
            mode = patch["mode"] as? String ?: current.mode,
            updatedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
        )

        val worldFile = File(worldDir(worldId), "world.json")
        worldFile.writeText(updated.toJson().toString(2))

        updated
    }

    suspend fun loadWorld(worldId: String): PlayWorld? = withContext(Dispatchers.IO) {
        val worldFile = File(worldDir(worldId), "world.json")
        if (worldFile.exists()) {
            try {
                val json = JSONObject(worldFile.readText())
                PlayWorld.fromJson(json)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun listWorlds(): List<PlayWorld> = withContext(Dispatchers.IO) {
        val worldsDir = File(projectRoot, WORLDS_DIR)
        if (!worldsDir.exists()) {
            return@withContext emptyList()
        }

        worldsDir.listFiles()?.filter { it.isDirectory }?.mapNotNull { dir ->
            loadWorld(dir.name)
        } ?: emptyList()
    }

    suspend fun deleteWorld(worldId: String) = withContext(Dispatchers.IO) {
        worldDir(worldId).deleteRecursively()
    }

    suspend fun createRun(worldId: String, runId: String = UUID.randomUUID().toString()): String = withContext(Dispatchers.IO) {
        val runDir = runDir(worldId, runId)
        runDir.mkdirs()

        // Create initial state
        val stateFile = File(runDir, "state.json")
        val initialState = PlayRunState(
            runId = runId,
            worldId = worldId,
            turn = 0,
            currentScene = "",
            currentLocation = "",
            mood = "",
            history = emptyList()
        )
        stateFile.writeText(initialState.toJson().toString(2))

        // Create empty transcript
        val transcriptFile = File(runDir, "transcript.json")
        transcriptFile.writeText("[]")

        // Create empty events
        val eventsFile = File(runDir, "events.json")
        eventsFile.writeText("[]")

        runId
    }

    suspend fun loadRun(worldId: String, runId: String): PlayRunState? = withContext(Dispatchers.IO) {
        val stateFile = File(runDir(worldId, runId), "state.json")
        if (stateFile.exists()) {
            try {
                val json = JSONObject(stateFile.readText())
                PlayRunState.fromJson(json)
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    suspend fun saveRun(worldId: String, runId: String, state: PlayRunState) = withContext(Dispatchers.IO) {
        val stateFile = File(runDir(worldId, runId), "state.json")
        stateFile.writeText(state.toJson().toString(2))
    }

    suspend fun listRuns(worldId: String): List<PlayRunSummary> = withContext(Dispatchers.IO) {
        val runsDir = File(worldDir(worldId), "runs")
        if (!runsDir.exists()) {
            return@withContext emptyList()
        }

        runsDir.listFiles()?.filter { it.isDirectory }?.mapNotNull { dir ->
            val runId = dir.name
            val state = loadRun(worldId, runId)
            if (state != null) {
                val transcript = loadTranscript(worldId, runId)
                val events = loadEvents(worldId, runId)
                PlayRunSummary(
                    id = runId,
                    updatedAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
                    eventCount = events.size,
                    transcriptCount = transcript.size
                )
            } else {
                null
            }
        } ?: emptyList()
    }

    suspend fun deleteRun(worldId: String, runId: String) = withContext(Dispatchers.IO) {
        runDir(worldId, runId).deleteRecursively()
    }

    suspend fun loadTranscript(worldId: String, runId: String): List<PlayTranscriptTurn> = withContext(Dispatchers.IO) {
        val transcriptFile = File(runDir(worldId, runId), "transcript.json")
        if (transcriptFile.exists()) {
            try {
                val jsonArray = org.json.JSONArray(transcriptFile.readText())
                (0 until jsonArray.length()).map { i ->
                    PlayTranscriptTurn.fromJson(jsonArray.getJSONObject(i))
                }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun saveTranscript(worldId: String, runId: String, transcript: List<PlayTranscriptTurn>) = withContext(Dispatchers.IO) {
        val transcriptFile = File(runDir(worldId, runId), "transcript.json")
        val jsonArray = org.json.JSONArray()
        transcript.forEach { turn ->
            jsonArray.put(turn.toJson())
        }
        transcriptFile.writeText(jsonArray.toString(2))
    }

    suspend fun appendTranscriptTurn(worldId: String, runId: String, turn: PlayTranscriptTurn) = withContext(Dispatchers.IO) {
        val transcript = loadTranscript(worldId, runId).toMutableList()
        transcript.add(turn)
        saveTranscript(worldId, runId, transcript)
    }

    suspend fun loadEvents(worldId: String, runId: String): List<PlayEvent> = withContext(Dispatchers.IO) {
        val eventsFile = File(runDir(worldId, runId), "events.json")
        if (eventsFile.exists()) {
            try {
                val jsonArray = org.json.JSONArray(eventsFile.readText())
                (0 until jsonArray.length()).map { i ->
                    val json = jsonArray.getJSONObject(i)
                    PlayEvent(
                        id = json.getString("id"),
                        turn = json.getInt("turn"),
                        actionKind = json.getString("actionKind"),
                        rawInput = json.getString("rawInput"),
                        outcomeSummary = json.optString("outcomeSummary", ""),
                        createdAt = json.optLong("createdAt", System.currentTimeMillis())
                    )
                }
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun saveEvents(worldId: String, runId: String, events: List<PlayEvent>) = withContext(Dispatchers.IO) {
        val eventsFile = File(runDir(worldId, runId), "events.json")
        val jsonArray = org.json.JSONArray()
        events.forEach { event ->
            jsonArray.put(org.json.JSONObject().apply {
                put("id", event.id)
                put("turn", event.turn)
                put("actionKind", event.actionKind)
                put("rawInput", event.rawInput)
                put("outcomeSummary", event.outcomeSummary)
                put("createdAt", event.createdAt)
            })
        }
        eventsFile.writeText(jsonArray.toString(2))
    }

    suspend fun appendEvent(worldId: String, runId: String, event: PlayEvent) = withContext(Dispatchers.IO) {
        val events = loadEvents(worldId, runId).toMutableList()
        events.add(event)
        saveEvents(worldId, runId, events)
    }

    suspend fun createRunSnapshot(worldId: String, runId: String): PlayRunSnapshot? = withContext(Dispatchers.IO) {
        val state = loadRun(worldId, runId) ?: return@withContext null
        val transcript = loadTranscript(worldId, runId)
        val events = loadEvents(worldId, runId)

        // Load graph snapshot from PlayDB
        val playDB = PlayDB(context, runId)
        val graph = playDB.snapshot()
        playDB.close()

        PlayRunSnapshot(
            id = runId,
            turn = state.turn,
            createdAt = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
            events = events,
            transcript = transcript,
            currentState = state,
            sceneProjection = state.currentScene,
            stateProjection = buildStateProjection(state),
            graph = graph
        )
    }

    private fun buildStateProjection(state: PlayRunState): String {
        return buildString {
            appendLine("# Current State")
            appendLine()
            appendLine("Turn: ${state.turn}")
            appendLine("Location: ${state.currentLocation}")
            if (state.mood.isNotEmpty()) {
                appendLine("Mood: ${state.mood}")
            }
            appendLine()
            if (state.history.isNotEmpty()) {
                appendLine("## Recent Actions")
                state.history.takeLast(5).forEach { result ->
                    appendLine("- ${result.action.actionType}: ${result.mutation.summary}")
                }
            }
        }
    }

    private fun assertSafeSegment(segment: String): String {
        // Ensure the segment is safe for use as a directory name
        return segment.replace(Regex("[^a-zA-Z0-9_-]"), "_")
    }
}

// Extension function for PlayRunState
fun PlayRunState.toJson(): JSONObject {
    return JSONObject().apply {
        put("runId", runId)
        put("worldId", worldId)
        put("turn", turn)
        put("currentScene", currentScene)
        put("currentLocation", currentLocation)
        put("mood", mood)
        put("history", org.json.JSONArray().apply {
            history.forEach { result ->
                put(result.toJson())
            }
        })
    }
}

fun PlayRunState.Companion.fromJson(json: JSONObject): PlayRunState {
    val historyArray = json.optJSONArray("history") ?: org.json.JSONArray()
    val history = (0 until historyArray.length()).map { i ->
        val resultJson = historyArray.getJSONObject(i)
        PlayStepResult(
            action = PlayActionIntent.fromJson(resultJson.getJSONObject("action")),
            mutation = PlayMutation(
                summary = resultJson.getJSONObject("mutation").optString("summary", "")
            ),
            sceneText = resultJson.optString("sceneText", ""),
            hud = resultJson.optJSONObject("hud")?.let { obj ->
                val map = mutableMapOf<String, Any?>()
                obj.keys().forEach { key -> map[key] = obj.get(key) }
                map
            } ?: emptyMap(),
            choices = resultJson.optJSONArray("choices")?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            } ?: emptyList(),
            mood = resultJson.optString("mood", ""),
            location = resultJson.optString("location", "")
        )
    }

    return PlayRunState(
        runId = json.getString("runId"),
        worldId = json.getString("worldId"),
        turn = json.getInt("turn"),
        currentScene = json.optString("currentScene", ""),
        currentLocation = json.optString("currentLocation", ""),
        mood = json.optString("mood", ""),
        history = history
    )
}

fun PlayStepResult.toJson(): JSONObject {
    return JSONObject().apply {
        put("action", action.toJson())
        put("mutation", mutation.toJson())
        put("sceneText", sceneText)
        put("hud", JSONObject(hud))
        put("choices", org.json.JSONArray(choices))
        put("mood", mood)
        put("location", location)
    }
}
