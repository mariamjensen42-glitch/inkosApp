package com.example.data.agents

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant

/**
 * DetectorAgent - Detects AI-generated content.
 *
 * This is the Kotlin Android equivalent of the TypeScript DetectorAgent module.
 * It handles:
 * - Detecting AI-generated content via external APIs
 * - Supporting GPTZero, Originality, and custom endpoints
 * - Returning normalized scores (0 = human, 1 = AI)
 */

data class DetectionConfig(
    val provider: String, // "gptzero", "originality", "custom"
    val apiUrl: String,
    val apiKeyEnv: String
)

data class DetectionResult(
    val score: Double, // 0-1, higher = more likely AI
    val provider: String,
    val detectedAt: String,
    val raw: Map<String, Any?>? = null
)

/**
 * DetectorAgent - Main class for AI content detection.
 */
class DetectorAgent {

    companion object {
        private const val TIMEOUT = 15000

        /**
         * Detect AI-generated content by calling an external detection API.
         * Returns a normalized score between 0 (human) and 1 (AI).
         */
        suspend fun detectAIContent(config: DetectionConfig, content: String): DetectionResult = withContext(Dispatchers.IO) {
            val apiKey = System.getenv(config.apiKeyEnv)
                ?: throw IllegalStateException("Detection API key not found. Set ${config.apiKeyEnv} in your environment.")

            val detectedAt = Instant.now().toString()

            when (config.provider) {
                "gptzero" -> detectGPTZero(config.apiUrl, apiKey, content, detectedAt)
                "originality" -> detectOriginality(config.apiUrl, apiKey, content, detectedAt)
                "custom" -> detectCustom(config.apiUrl, apiKey, content, detectedAt)
                else -> throw IllegalArgumentException("Unsupported detection provider: ${config.provider}")
            }
        }

        private suspend fun detectGPTZero(
            apiUrl: String,
            apiKey: String,
            content: String,
            detectedAt: String
        ): DetectionResult {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("X-Api-Key", apiKey)
                connection.doOutput = true
                connection.connectTimeout = TIMEOUT
                connection.readTimeout = TIMEOUT

                val requestBody = JSONObject().apply {
                    put("document", content)
                }

                connection.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    val errorBody = try {
                        BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    } catch (e: Exception) {
                        ""
                    }
                    throw RuntimeException("GPTZero API failed: $responseCode $errorBody")
                }

                val responseText = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val data = JSONObject(responseText)
                val documents = data.optJSONArray("documents")
                val score = documents?.optJSONObject(0)?.optDouble("completely_generated_prob", 0.0) ?: 0.0

                return DetectionResult(
                    score = score,
                    provider = "gptzero",
                    detectedAt = detectedAt,
                    raw = data.toMap()
                )
            } finally {
                connection.disconnect()
            }
        }

        private suspend fun detectOriginality(
            apiUrl: String,
            apiKey: String,
            content: String,
            detectedAt: String
        ): DetectionResult {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.doOutput = true
                connection.connectTimeout = TIMEOUT
                connection.readTimeout = TIMEOUT

                val requestBody = JSONObject().apply {
                    put("content", content)
                }

                connection.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    val errorBody = try {
                        BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    } catch (e: Exception) {
                        ""
                    }
                    throw RuntimeException("Originality API failed: $responseCode $errorBody")
                }

                val responseText = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val data = JSONObject(responseText)
                val scoreObj = data.optJSONObject("score")
                val score = scoreObj?.optDouble("ai", 0.0) ?: 0.0

                return DetectionResult(
                    score = score,
                    provider = "originality",
                    detectedAt = detectedAt,
                    raw = data.toMap()
                )
            } finally {
                connection.disconnect()
            }
        }

        private suspend fun detectCustom(
            apiUrl: String,
            apiKey: String,
            content: String,
            detectedAt: String
        ): DetectionResult {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $apiKey")
                connection.doOutput = true
                connection.connectTimeout = TIMEOUT
                connection.readTimeout = TIMEOUT

                val requestBody = JSONObject().apply {
                    put("content", content)
                }

                connection.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    val errorBody = try {
                        BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    } catch (e: Exception) {
                        ""
                    }
                    throw RuntimeException("Detection API failed: $responseCode $errorBody")
                }

                val responseText = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val data = JSONObject(responseText)
                // Custom endpoint must return { score: number } at minimum
                val score = if (data.has("score") && !data.isNull("score")) {
                    data.getDouble("score")
                } else {
                    0.0
                }

                return DetectionResult(
                    score = score,
                    provider = "custom",
                    detectedAt = detectedAt,
                    raw = data.toMap()
                )
            } finally {
                connection.disconnect()
            }
        }

        private fun JSONObject.toMap(): Map<String, Any?> {
            val map = mutableMapOf<String, Any?>()
            keys().forEach { key ->
                map[key] = get(key)
            }
            return map
        }
    }
}
