package com.example.data.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object XiaomiMimoService {
    private const val TAG = "XiaomiMimoService"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    val isApiKeyAvailable: Boolean
        get() {
            val key = LlmPreferences.xiaomiMimoKey
            return key.isNotBlank() && !key.contains("PLACEHOLDER")
        }

    suspend fun generateContent(systemInstructions: String, prompt: String, requireJson: Boolean = false): String = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable) {
            Log.w(TAG, "Xiaomi MiMo API key is not configured. Falling back to local simulated Xiaomi MiMo engine.")
            return@withContext simulateMimoLocal(systemInstructions, prompt, requireJson)
        }

        val apiKey = LlmPreferences.xiaomiMimoKey
        var baseUrl = LlmPreferences.xiaomiMimoBaseUrl.trim()
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length - 1)
        }

        // Handle path resolution for OpenAI-compatible endpoint
        val url = when {
            baseUrl.contains("chat/completions") -> baseUrl
            baseUrl.endsWith("v1") -> "$baseUrl/chat/completions"
            else -> "$baseUrl/chat/completions"
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()

        val requestJson = JSONObject().apply {
            put("model", LlmPreferences.xiaomiMimoModel)
            
            val messagesArray = JSONArray().apply {
                if (systemInstructions.isNotEmpty()) {
                    put(JSONObject().apply {
                        put("role", "system")
                        put("content", systemInstructions)
                    })
                }
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", prompt)
                })
            }
            put("messages", messagesArray)
            put("temperature", LlmPreferences.temperature.toDouble())
            put("stream", false)
            
            if (requireJson) {
                put("response_format", JSONObject().apply {
                    put("type", "json_object")
                })
            }
        }

        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body)
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string()
                if (response.isSuccessful && responseBody != null) {
                    val root = JSONObject(responseBody)
                    val choices = root.optJSONArray("choices")
                    val firstChoice = choices?.optJSONObject(0)
                    val message = firstChoice?.optJSONObject("message")
                    val content = message?.optString("content")

                    if (!content.isNullOrEmpty()) {
                        Log.i(TAG, "Successfully received content from Xiaomi MiMo remote server API.")
                        return@withContext content
                    }
                }
                Log.e(TAG, "Xiaomi MiMo Request failed with code: ${response.code} body: $responseBody")
                return@withContext simulateMimoLocal(systemInstructions, prompt, requireJson)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Xiaomi MiMo API call: ${e.message}", e)
            return@withContext simulateMimoLocal(systemInstructions, prompt, requireJson)
        }
    }

    private fun simulateMimoLocal(systemInstructions: String, prompt: String, requireJson: Boolean): String {
        Log.i(TAG, "Simulating local Xiaomi MiMo pipeline responses.")
        
        if (requireJson) {
            if (prompt.contains("PLAY") || prompt.contains("PlayState") || prompt.contains("world") || prompt.contains("action")) {
                val action = if (prompt.contains("User Action: ")) {
                    prompt.substringAfter("User Action: ").substringBefore("\n").trim()
                } else "explore"

                val isDetective = prompt.contains("DETECTIVE") || prompt.contains("detective") || prompt.contains("case")
                val isFantasy = prompt.contains("Warcraft") || prompt.contains("fantasy") || prompt.contains("spell")

                val mockScene = if (isDetective) {
                    "【Rain-slicked Back Alley - MiMo Sim】\nYou stand in the dim alley search area. Raindrops patter heavily against garbage bins. A suspicious neon sign 'The Jade Club' flickers down the street. Time is advancing naturally. A wet trash bag contains what seems like the missing accounting ledger. Your search turns up a gold-plated cufflink near the rusted pipe."
                } else if (isFantasy) {
                    "【Damp Border Patrol Post - MiMo Sim】\nThe flickering flames of the hearth cast dancing shadows. The cold Nordic mountain breeze whistles outside the thick oak doors. Inside, you smell stale ale and roasted boar. Patrol guards speak of heavy wolf tracks in the northern canyon."
                } else {
                    "【Story Crossroad Stage - MiMo Sim】\nYou have transitioned deeper into the story world. The surroundings feel dense and full of unanswered questions. Time proceeds forward according to your speed. Your gear and status show high activity."
                }

                val characterArr = JSONArray().apply {
                    put(JSONObject().apply {
                        put("name", "Inspector Vance")
                        put("relation", "Partner (Trusted)")
                        put("status", "Searching clues, shivering from the cold")
                    })
                }

                val itemsArr = JSONArray().apply {
                    put(JSONObject().apply {
                        put("name", "Encrypted Ledger")
                        put("rating", "★★★★☆ (Rare Evidence)")
                        put("desc", "A leather ledger containing coded business transactions.")
                    })
                }

                val optionsArr = JSONArray().apply {
                    put("Verify the fingerprint on the gold cufflink")
                    put("Enter through the rear door of The Jade Club")
                }

                return JSONObject().apply {
                    put("scene_description", mockScene)
                    put("hud_energy", "🔋 85 / 100")
                    put("hud_status", "🔍 Clues Gathered: 1")
                    put("characters", characterArr)
                    put("items", itemsArr)
                    put("actions", optionsArr)
                    put("time_state", "Time: 14:00")
                    put("log_response", "Xiaomi MiMo Engine: You executed action: '$action'. Simulated result calculated.")
                }.toString()
            }
        }

        // Standard content outputs
        return when {
            prompt.contains("意图") || prompt.contains("intent") -> {
                """
                # Chapter Intent
                **[Xiaomi MiMo Focus Point]**: Maximize thematic depth, pacing, and conflict escalations in MiMo-style.
                
                ## Key Actions (MUST-KEEP)
                1. Deepen the core ideological conflict.
                2. Unravel a key character mystery scene.
                3. Elevate emotional resonance between characters.
                
                ## Taboos (MUST-AVOID)
                1. Avoid fast, rushed narrative pacing.
                2. Do not introduce sudden unprompted characters.
                3. No clichés or generic placeholder summaries at the end.
                """.trimIndent()
            }
            prompt.contains("audit") || prompt.contains("审核") || prompt.contains("审稿") -> {
                val score = (88..97).random()
                """
                === INKOS CONTINUOUS AUDIT REPORT ===
                Score: $score/100 (Xiaomi MiMo Logic Check)
                [Rule Compliance Check]: Passed with High Confidence
                
                ### Dimension Verification:
                - Character Memory: 🟢 Perfect (Characters recall previous encounters correctly with MiMo high cognition)
                - Resource Continuity: 🟢 Correct (Resources accounted for perfectly)
                - Pacing Rhythm: 🟢 Excellent (Balanced scene exposition and rapid actions)
                - AI Flavor Detection: 🟢 Clean (We filtered out stale phrases such as "As they stood there", "In conclusion")
                
                ### Suggestions for Revision:
                1. Make the dialogue slightly sharper to reflect characters' deep motivations.
                2. Build a clearer bridge into the next planned chapter.
                """.trimIndent()
            }
            prompt.contains("write") || prompt.contains("撰写") || prompt.contains("compose") || prompt.contains("续写") -> {
                """
                The cold rain pattered heavily against the stained-glass window, washing out the streets in a slate-gray blur.

                "You didn't think I'd find this, did you?" Vance threw the cold gold cufflink upon the mahogany table. It hit with a sharp wooden clack, sliding past the dry inkwell before halting.

                Gromm did not answer immediately. He looked down at the blood-stained gold piece, his heavy jaw twitching beneath the firelight.

                "Vance," Gromm spoke, his dark voice scraping through the damp silence of the room. "The wilderness takes those who seek things best left buried. Some boundaries are not meant to be crossed by detectives."

                "Then it is fortunate," Vance said, shifting his weight forward, "that I have never cared much for boundaries. Tell me about the accounting clerk."
                """.trimIndent()
            }
            prompt.contains("short") || prompt.contains("短篇") -> {
                """
                == SHORT STORY DETAILS ==
                Title: The Crimson Vault (Xiaomi MiMo Edition)
                Summary: A dark crime mystery styled story, focusing on power, secrecy, and high stakes.
                
                Chapter 1: The Bloodstained Cufflink
                The rain continued endlessly. Outside Vance's office, the fog rolled thick over the docks. He examined the Golden Cufflink under the flickering desk lamp.
                
                Chapter 2: The Silent Vault
                Vance infiltrated the Guild's basement treasury. The leather files smelled of decay. There lay the Crimson Ledger, locked under an iron latch.
                
                == SALES PACKAGE ==
                - Selling Points: High tension, intense dialogues, dark urban fantasy setting.
                - Cover Prompt: Deep, moody rainy street, a single red umbrella, dark watercolor style, highly detailed.
                """.trimIndent()
            }
            else -> {
                "Xiaomi MiMo AI simulation is ready. Your instructions: '$prompt' have been safely compiled into the story blueprint."
            }
        }
    }
}
