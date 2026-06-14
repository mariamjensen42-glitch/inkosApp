package com.example.data.play

import android.content.Context
import com.example.data.agents.BaseAgent
import com.example.data.models.BookConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.UUID

/**
 * PlayRunner - Runs interactive fiction sessions.
 *
 * This is the Kotlin Android equivalent of the TypeScript PlayRunner class.
 * It manages:
 * - Interactive fiction sessions
 * - Action interpretation
 * - World mutation
 * - Scene rendering
 * - State reconciliation
 */

// Data classes for play system

data class PlayActionIntent(
    val actionType: String, // "move", "talk", "examine", "use", "interact", "wait", "system"
    val target: String? = null,
    val details: Map<String, Any?> = emptyMap(),
    val confidence: Double = 1.0
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("actionType", actionType)
            target?.let { put("target", it) }
            put("details", JSONObject(details))
            put("confidence", confidence)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): PlayActionIntent {
            return PlayActionIntent(
                actionType = json.getString("actionType"),
                target = json.optString("target", null),
                details = json.optJSONObject("details")?.let { obj ->
                    val map = mutableMapOf<String, Any?>()
                    obj.keys().forEach { key -> map[key] = obj.get(key) }
                    map
                } ?: emptyMap(),
                confidence = json.optDouble("confidence", 1.0)
            )
        }
    }
}

data class PlayMutation(
    val entitiesAdded: List<PlayEntity> = emptyList(),
    val entitiesUpdated: List<PlayEntity> = emptyList(),
    val entitiesRemoved: List<String> = emptyList(),
    val edgesAdded: List<PlayEdge> = emptyList(),
    val edgesRemoved: List<String> = emptyList(),
    val stateSlotsUpdated: List<PlayStateSlot> = emptyList(),
    val summary: String = ""
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("entitiesAdded", entitiesAdded.size)
            put("entitiesUpdated", entitiesUpdated.size)
            put("entitiesRemoved", entitiesRemoved.size)
            put("edgesAdded", edgesAdded.size)
            put("edgesRemoved", edgesRemoved.size)
            put("stateSlotsUpdated", stateSlotsUpdated.size)
            put("summary", summary)
        }
    }
}

data class PlaySceneRender(
    val sceneText: String,
    val hud: Map<String, Any?> = emptyMap(),
    val choices: List<String> = emptyList(),
    val mood: String = "",
    val location: String = ""
)

data class PlayStepResult(
    val action: PlayActionIntent,
    val mutation: PlayMutation,
    val sceneText: String,
    val hud: Map<String, Any?> = emptyMap(),
    val choices: List<String> = emptyList(),
    val mood: String = "",
    val location: String = ""
)

data class PlayWorld(
    val worldId: String,
    val name: String,
    val description: String,
    val rules: String,
    val characters: List<PlayEntity> = emptyList(),
    val locations: List<PlayEntity> = emptyList(),
    val items: List<PlayEntity> = emptyList()
)

data class PlayRunState(
    val runId: String,
    val worldId: String,
    val turn: Int = 0,
    val currentScene: String = "",
    val currentLocation: String = "",
    val mood: String = "",
    val history: List<PlayStepResult> = emptyList()
)

/**
 * PlayRunner - Main class for running interactive fiction sessions.
 */
class PlayRunner(
    private val context: Context,
    private val projectRoot: File,
    private val worldId: String,
    private val runId: String = UUID.randomUUID().toString()
) {
    private val playDB = PlayDB(context, runId)
    private var currentState = PlayRunState(runId = runId, worldId = worldId)

    suspend fun initialize(world: PlayWorld) = withContext(Dispatchers.IO) {
        // Seed the play graph with world entities
        val entities = mutableListOf<PlayEntity>()
        val edges = mutableListOf<PlayEdge>()

        // Add characters
        world.characters.forEach { character ->
            entities.add(character)
        }

        // Add locations
        world.locations.forEach { location ->
            entities.add(location)
        }

        // Add items
        world.items.forEach { item ->
            entities.add(item)
        }

        // Create snapshot and replace database
        val snapshot = PlayGraphSnapshot(
            entities = entities,
            edges = edges,
            stateSlots = emptyList(),
            events = emptyList()
        )
        playDB.replaceWithSnapshot(snapshot)

        // Set initial state
        currentState = currentState.copy(
            currentScene = world.description,
            currentLocation = world.locations.firstOrNull()?.label ?: "Unknown"
        )
    }

    suspend fun step(input: String): PlayStepResult = withContext(Dispatchers.IO) {
        val turn = currentState.turn + 1

        // 1. Interpret the action
        val action = interpretAction(input)

        // 2. Propose world mutation
        val mutation = proposeMutation(turn, input, action)

        // 3. Apply mutation to database
        applyMutation(mutation)

        // 4. Record event
        val event = PlayEvent(
            id = "event_${turn}_${UUID.randomUUID().toString().take(8)}",
            turn = turn,
            actionKind = action.actionType,
            rawInput = input,
            outcomeSummary = mutation.summary,
            createdAt = System.currentTimeMillis()
        )
        playDB.recordEvent(event)

        // 5. Render scene
        val scene = renderScene(input, action, mutation)

        // 6. Update state
        val result = PlayStepResult(
            action = action,
            mutation = mutation,
            sceneText = scene.sceneText,
            hud = scene.hud,
            choices = scene.choices,
            mood = scene.mood,
            location = scene.location
        )

        currentState = currentState.copy(
            turn = turn,
            currentScene = scene.sceneText,
            currentLocation = scene.location,
            mood = scene.mood,
            history = currentState.history + result
        )

        result
    }

    suspend fun getState(): PlayRunState = withContext(Dispatchers.IO) {
        currentState
    }

    suspend fun getSnapshot(): PlayGraphSnapshot = withContext(Dispatchers.IO) {
        playDB.snapshot()
    }

    suspend fun close() {
        playDB.close()
    }

    private suspend fun interpretAction(input: String): PlayActionIntent {
        // Simple action interpretation
        val lowerInput = input.lowercase().trim()

        return when {
            lowerInput.startsWith("go ") || lowerInput.startsWith("move ") -> {
                val target = input.substringAfter("go ").substringAfter("move ").trim()
                PlayActionIntent(actionType = "move", target = target)
            }
            lowerInput.startsWith("talk ") || lowerInput.startsWith("speak ") -> {
                val target = input.substringAfter("talk ").substringAfter("speak ").trim()
                PlayActionIntent(actionType = "talk", target = target)
            }
            lowerInput.startsWith("examine ") || lowerInput.startsWith("look ") -> {
                val target = input.substringAfter("examine ").substringAfter("look ").trim()
                PlayActionIntent(actionType = "examine", target = target)
            }
            lowerInput.startsWith("use ") -> {
                val target = input.substringAfter("use ").trim()
                PlayActionIntent(actionType = "use", target = target)
            }
            lowerInput == "wait" || lowerInput == "rest" -> {
                PlayActionIntent(actionType = "wait")
            }
            lowerInput == "inventory" || lowerInput == "i" -> {
                PlayActionIntent(actionType = "system", details = mapOf("command" to "inventory"))
            }
            else -> {
                PlayActionIntent(actionType = "interact", details = mapOf("raw" to input))
            }
        }
    }

    private suspend fun proposeMutation(
        turn: Int,
        input: String,
        action: PlayActionIntent
    ): PlayMutation {
        // Simple mutation proposal
        return when (action.actionType) {
            "move" -> {
                PlayMutation(
                    summary = "Player moves to ${action.target}",
                    stateSlotsUpdated = listOf(
                        PlayStateSlot(
                            id = "location_${turn}",
                            ownerEntityId = "player",
                            kind = "attribute",
                            label = "location",
                            value = action.target,
                            updatedEventId = "event_${turn}"
                        )
                    )
                )
            }
            "talk" -> {
                PlayMutation(
                    summary = "Player talks to ${action.target}"
                )
            }
            "examine" -> {
                PlayMutation(
                    summary = "Player examines ${action.target}"
                )
            }
            "use" -> {
                PlayMutation(
                    summary = "Player uses ${action.target}"
                )
            }
            else -> {
                PlayMutation(
                    summary = "Player performs: $input"
                )
            }
        }
    }

    private suspend fun applyMutation(mutation: PlayMutation) {
        // Apply entities
        mutation.entitiesAdded.forEach { entity ->
            playDB.upsertEntity(entity)
        }

        mutation.entitiesUpdated.forEach { entity ->
            playDB.upsertEntity(entity)
        }

        // Apply edges
        mutation.edgesAdded.forEach { edge ->
            playDB.upsertEdge(edge)
        }

        // Apply state slots
        mutation.stateSlotsUpdated.forEach { slot ->
            playDB.upsertStateSlot(slot)
        }
    }

    private suspend fun renderScene(
        input: String,
        action: PlayActionIntent,
        mutation: PlayMutation
    ): PlaySceneRender {
        // Simple scene rendering
        val sceneText = buildString {
            appendLine("You ${action.actionType}")
            action.target?.let { appendLine("Target: $it") }
            appendLine()
            appendLine(mutation.summary)
            appendLine()
            appendLine("Current location: ${currentState.currentLocation}")
            if (currentState.mood.isNotEmpty()) {
                appendLine("Mood: ${currentState.mood}")
            }
        }

        val hud = mapOf(
            "turn" to currentState.turn,
            "location" to currentState.currentLocation,
            "mood" to currentState.mood
        )

        val choices = listOf(
            "Look around",
            "Check inventory",
            "Wait"
        )

        return PlaySceneRender(
            sceneText = sceneText,
            hud = hud,
            choices = choices,
            mood = currentState.mood,
            location = currentState.currentLocation
        )
    }
}
