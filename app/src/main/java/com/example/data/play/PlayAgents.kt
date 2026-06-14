package com.example.data.play

import com.example.data.agents.BaseAgent
import com.example.data.models.AgentContext
import com.example.data.models.LLMResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * PlayAgents - Agents for the Play system.
 *
 * This is the Kotlin Android equivalent of the TypeScript PlayAgents module.
 * It contains:
 * - PlayActionInterpreterAgent - Interprets player actions
 * - PlayWorldMutatorAgent - Proposes world mutations
 * - PlaySceneRendererAgent - Renders scenes
 * - PlaySceneReconcilerAgent - Reconciles scene state
 */

// Data classes for agent inputs

data class PlayActionInterpreterInput(
    val input: String,
    val sceneBrief: String,
    val language: String = "zh"
)

data class PlayWorldMutatorInput(
    val turn: Int,
    val input: String,
    val action: PlayActionIntent,
    val context: String,
    val language: String = "zh"
)

data class PlaySceneRenderInput(
    val input: String,
    val action: PlayActionIntent,
    val mutationSummary: String,
    val stateBrief: String,
    val replayContext: String? = null,
    val language: String = "zh",
    val worldPremise: String? = null
)

data class PlaySceneReconcileInput(
    val turn: Int,
    val input: String,
    val action: PlayActionIntent,
    val mutation: PlayMutation,
    val sceneText: String,
    val context: String,
    val stateBrief: String,
    val language: String = "zh",
    val worldPremise: String? = null
)

data class PlaySceneRender(
    val sceneText: String,
    val suggestedActions: List<String> = emptyList()
)

/**
 * PlayActionInterpreterAgent - Interprets player actions.
 */
class PlayActionInterpreterAgent(ctx: AgentContext) : BaseAgent(ctx) {

    override val name: String = "play-action-interpreter"

    suspend fun interpret(input: PlayActionInterpreterInput): PlayActionIntent = withContext(Dispatchers.IO) {
        val prompt = buildString {
            appendLine("You are a play action interpreter. Analyze the player's input and determine their intended action.")
            appendLine()
            appendLine("Current scene:")
            appendLine(input.sceneBrief)
            appendLine()
            appendLine("Player input:")
            appendLine(input.input)
            appendLine()
            appendLine("Determine the action type and target. Respond with JSON:")
            appendLine("""{"actionType": "move|talk|examine|use|interact|wait|system", "target": "...", "details": {...}}""")
        }

        val response = chat(prompt, input.language)
        parseActionResponse(response)
    }

    private fun parseActionResponse(response: String): PlayActionIntent {
        return try {
            val json = JSONObject(response)
            PlayActionIntent(
                actionType = json.optString("actionType", "interact"),
                target = json.optString("target", null),
                details = json.optJSONObject("details")?.let { obj ->
                    val map = mutableMapOf<String, Any?>()
                    obj.keys().forEach { key -> map[key] = obj.get(key) }
                    map
                } ?: emptyMap(),
                confidence = json.optDouble("confidence", 1.0)
            )
        } catch (e: Exception) {
            PlayActionIntent(
                actionType = "interact",
                details = mapOf("raw" to response)
            )
        }
    }
}

/**
 * PlayWorldMutatorAgent - Proposes world mutations.
 */
class PlayWorldMutatorAgent(ctx: AgentContext) : BaseAgent(ctx) {

    override val name: String = "play-world-mutator"

    suspend fun proposeMutation(input: PlayWorldMutatorInput): PlayMutation = withContext(Dispatchers.IO) {
        val prompt = buildString {
            appendLine("You are a play world mutator. Based on the player's action, propose changes to the world state.")
            appendLine()
            appendLine("Turn: ${input.turn}")
            appendLine("Player action: ${input.action.actionType}")
            input.action.target?.let { appendLine("Target: $it") }
            appendLine("Context:")
            appendLine(input.context)
            appendLine()
            appendLine("Propose mutations to the world state. Respond with JSON describing changes:")
            appendLine("""{"summary": "...", "entitiesAdded": [...], "entitiesUpdated": [...], "edgesAdded": [...]}""")
        }

        val response = chat(prompt, input.language)
        parseMutationResponse(response)
    }

    private fun parseMutationResponse(response: String): PlayMutation {
        return try {
            val json = JSONObject(response)
            PlayMutation(
                summary = json.optString("summary", "World state updated")
            )
        } catch (e: Exception) {
            PlayMutation(
                summary = "World state updated"
            )
        }
    }
}

/**
 * PlaySceneRendererAgent - Renders scenes.
 */
class PlaySceneRendererAgent(ctx: AgentContext) : BaseAgent(ctx) {

    override val name: String = "play-scene-renderer"

    suspend fun render(input: PlaySceneRenderInput): PlaySceneRender = withContext(Dispatchers.IO) {
        val prompt = buildString {
            appendLine("You are a play scene renderer. Create an immersive scene description.")
            appendLine()
            appendLine("Player action: ${input.action.actionType}")
            input.action.target?.let { appendLine("Target: $it") }
            appendLine("Mutation: ${input.mutationSummary}")
            appendLine("Current state:")
            appendLine(input.stateBrief)
            input.worldPremise?.let {
                appendLine()
                appendLine("World premise:")
                appendLine(it)
            }
            appendLine()
            appendLine("Render the scene with vivid description. Respond with JSON:")
            appendLine("""{"sceneText": "...", "suggestedActions": ["...", "..."]}""")
        }

        val response = chat(prompt, input.language)
        parseSceneResponse(response)
    }

    private fun parseSceneResponse(response: String): PlaySceneRender {
        return try {
            val json = JSONObject(response)
            val suggestedActions = json.optJSONArray("suggestedActions")?.let { array ->
                (0 until array.length()).map { array.getString(it) }
            } ?: emptyList()

            PlaySceneRender(
                sceneText = json.optString("sceneText", "The scene unfolds..."),
                suggestedActions = suggestedActions
            )
        } catch (e: Exception) {
            PlaySceneRender(
                sceneText = "The scene unfolds...",
                suggestedActions = emptyList()
            )
        }
    }
}

/**
 * PlaySceneReconcilerAgent - Reconciles scene state.
 */
class PlaySceneReconcilerAgent(ctx: AgentContext) : BaseAgent(ctx) {

    override val name: String = "play-scene-reconciler"

    suspend fun reconcile(input: PlaySceneReconcileInput): PlayMutation = withContext(Dispatchers.IO) {
        val prompt = buildString {
            appendLine("You are a play scene reconciler. Ensure the scene is consistent with world state.")
            appendLine()
            appendLine("Turn: ${input.turn}")
            appendLine("Player action: ${input.action.actionType}")
            appendLine("Current scene:")
            appendLine(input.sceneText)
            appendLine("Context:")
            appendLine(input.context)
            appendLine("Current state:")
            appendLine(input.stateBrief)
            input.worldPremise?.let {
                appendLine()
                appendLine("World premise:")
                appendLine(it)
            }
            appendLine()
            appendLine("Reconcile the scene and propose any necessary state corrections. Respond with JSON:")
            appendLine("""{"summary": "...", "corrections": [...]}""")
        }

        val response = chat(prompt, input.language)
        parseReconcileResponse(response)
    }

    private fun parseReconcileResponse(response: String): PlayMutation {
        return try {
            val json = JSONObject(response)
            PlayMutation(
                summary = json.optString("summary", "Scene reconciled")
            )
        } catch (e: Exception) {
            PlayMutation(
                summary = "Scene reconciled"
            )
        }
    }
}

/**
 * Helper function to chat with LLM.
 */
private suspend fun BaseAgent.chat(prompt: String, language: String): String {
    // This would use the actual LLM provider
    // For now, return a simple response
    return """{"sceneText": "The scene unfolds before you...", "suggestedActions": ["Look around", "Talk to someone"]}"""
}
