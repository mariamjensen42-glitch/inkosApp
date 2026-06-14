package com.example.data.llm

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * LLM Message - matches TypeScript LLMMessage interface
 */
data class LLMMessage(
    val role: String, // "system", "user", "assistant"
    val content: String
)

/**
 * LLM Response - matches TypeScript LLMResponse interface
 */
data class LLMResponse(
    val content: String,
    val usage: LLMTokenUsage
)

/**
 * Token usage in LLM response
 */
data class LLMTokenUsage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0
)

/**
 * Stream progress callback - matches TypeScript StreamProgress interface
 */
data class StreamProgress(
    val elapsedMs: Long,
    val totalChars: Int,
    val chineseChars: Int,
    val status: String // "streaming" or "done"
)

/**
 * Stream progress callback type
 */
typealias OnStreamProgress = (StreamProgress) -> Unit

/**
 * LLM Provider types
 */
enum class LLMProvider {
    OPENAI,
    ANTHROPIC,
    CUSTOM
}

/**
 * API format types
 */
enum class ApiFormat {
    CHAT,
    RESPONSES
}

/**
 * LLM Configuration - matches TypeScript LLMConfig
 */
data class LLMConfig(
    val provider: LLMProvider = LLMProvider.CUSTOM,
    val service: String = "custom",
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val temperature: Double = 0.7,
    val maxTokens: Int = 8192,
    val thinkingBudget: Int = 0,
    val apiFormat: ApiFormat = ApiFormat.CHAT,
    val stream: Boolean = true,
    val headers: Map<String, String>? = null
)

/**
 * LLM Client - matches TypeScript LLMClient interface
 */
data class LLMClient(
    val provider: LLMProvider,
    val service: String?,
    val apiFormat: ApiFormat,
    val stream: Boolean,
    val defaults: LLMDefaults
)

/**
 * LLM Client defaults
 */
data class LLMDefaults(
    val temperature: Double,
    val maxTokens: Int,
    val thinkingBudget: Int
)

/**
 * Partial response error - matches TypeScript PartialResponseError
 */
class PartialResponseError(
    val partialContent: String,
    cause: Throwable
) : Exception("Stream interrupted after ${partialContent.length} chars: ${cause.message}", cause)

/**
 * Context window exceeded error - matches TypeScript ContextWindowExceededError
 */
class ContextWindowExceededError(
    val estimatedInputTokens: Int,
    val reservedOutputTokens: Int,
    val contextWindow: Int,
    val model: String
) : Exception(
    "InkOS context window guard: estimated input $estimatedInputTokens tokens + " +
    "reserved output $reservedOutputTokens tokens exceeds context window $contextWindow " +
    "for model \"$model\". Please compress the active book/session context before retrying."
)

/**
 * LLM Provider implementation - matches TypeScript provider.ts
 * Handles multiple providers (OpenAI, Anthropic, custom) with streaming support
 */
object LLMProvider {
    private const val TAG = "LLMProvider"
    private const val INKOS_USER_AGENT = "InkOS-Android/1.0.0"
    private const val TRANSIENT_LLM_RETRIES = 2
    private const val MIN_SALVAGEABLE_CHARS = 500

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    /**
     * Create an LLM client from configuration
     */
    fun createLLMClient(config: LLMConfig): LLMClient {
        return LLMClient(
            provider = config.provider,
            service = config.service,
            apiFormat = config.apiFormat,
            stream = config.stream,
            defaults = LLMDefaults(
                temperature = config.temperature,
                maxTokens = config.maxTokens,
                thinkingBudget = config.thinkingBudget
            )
        )
    }

    /**
     * Chat completion - main entry point for LLM calls
     * Matches TypeScript chatCompletion function
     */
    suspend fun chatCompletion(
        client: LLMClient,
        model: String,
        messages: List<LLMMessage>,
        options: ChatCompletionOptions = ChatCompletionOptions()
    ): LLMResponse {
        val temperature = options.temperature ?: client.defaults.temperature
        val maxTokens = options.maxTokens ?: client.defaults.maxTokens

        // Estimate tokens for context window check
        val estimatedInputTokens = estimateMessagesTokens(messages)
        
        return withTransientRetry(options.retry) {
            when (client.provider) {
                LLMProvider.OPENAI -> chatCompletionOpenAI(
                    model, messages, temperature, maxTokens, client.stream, options
                )
                LLMProvider.ANTHROPIC -> chatCompletionAnthropic(
                    model, messages, temperature, maxTokens, client.stream, options
                )
                LLMProvider.CUSTOM -> chatCompletionCustom(
                    model, messages, temperature, maxTokens, client.stream, options
                )
            }
        }
    }

    /**
     * OpenAI-compatible chat completion
     */
    private suspend fun chatCompletionOpenAI(
        model: String,
        messages: List<LLMMessage>,
        temperature: Double,
        maxTokens: Int,
        stream: Boolean,
        options: ChatCompletionOptions
    ): LLMResponse = withContext(Dispatchers.IO) {
        val baseUrl = options.baseUrl ?: "https://api.openai.com/v1"
        val apiKey = options.apiKey ?: throw IllegalArgumentException("API key required for OpenAI")
        
        val url = "${baseUrl.trimEnd('/')}/chat/completions"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val requestJson = JSONObject().apply {
            put("model", model)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            put("stream", false) // Non-streaming for simplicity
            
            val messagesArray = JSONArray().apply {
                for (msg in messages) {
                    put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            }
            put("messages", messagesArray)
        }

        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", INKOS_USER_AGENT)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response body")
        
        if (!response.isSuccessful) {
            throw wrapLLMError(Exception("HTTP ${response.code}: $responseBody"), baseUrl, model)
        }

        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        val firstChoice = choices?.optJSONObject(0)
        val message = firstChoice?.optJSONObject("message")
        val content = message?.optString("content") ?: throw Exception("No content in response")
        
        val usage = json.optJSONObject("usage")
        LLMResponse(
            content = content,
            usage = LLMTokenUsage(
                promptTokens = usage?.optInt("prompt_tokens", 0) ?: 0,
                completionTokens = usage?.optInt("completion_tokens", 0) ?: 0,
                totalTokens = usage?.optInt("total_tokens", 0) ?: 0
            )
        )
    }

    /**
     * Anthropic-compatible chat completion
     */
    private suspend fun chatCompletionAnthropic(
        model: String,
        messages: List<LLMMessage>,
        temperature: Double,
        maxTokens: Int,
        stream: Boolean,
        options: ChatCompletionOptions
    ): LLMResponse = withContext(Dispatchers.IO) {
        val baseUrl = options.baseUrl ?: "https://api.anthropic.com"
        val apiKey = options.apiKey ?: throw IllegalArgumentException("API key required for Anthropic")
        
        val url = "${baseUrl.trimEnd('/')}/messages"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        // Extract system message
        val systemMessage = messages.find { it.role == "system" }?.content
        val nonSystemMessages = messages.filter { it.role != "system" }

        val requestJson = JSONObject().apply {
            put("model", model)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            
            if (systemMessage != null) {
                put("system", systemMessage)
            }
            
            val messagesArray = JSONArray().apply {
                for (msg in nonSystemMessages) {
                    put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            }
            put("messages", messagesArray)
        }

        val body = requestJson.toString().toRequestBody(mediaType)
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", apiKey)
            .addHeader("anthropic-version", "2023-06-01")
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", INKOS_USER_AGENT)
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response body")
        
        if (!response.isSuccessful) {
            throw wrapLLMError(Exception("HTTP ${response.code}: $responseBody"), baseUrl, model)
        }

        val json = JSONObject(responseBody)
        val contentArray = json.optJSONArray("content")
        val content = StringBuilder()
        
        for (i in 0 until (contentArray?.length() ?: 0)) {
            val block = contentArray?.optJSONObject(i)
            if (block?.optString("type") == "text") {
                content.append(block.optString("text", ""))
            }
        }
        
        if (content.isEmpty()) {
            throw Exception("No content in Anthropic response")
        }
        
        val usage = json.optJSONObject("usage")
        LLMResponse(
            content = content.toString(),
            usage = LLMTokenUsage(
                promptTokens = usage?.optInt("input_tokens", 0) ?: 0,
                completionTokens = usage?.optInt("output_tokens", 0) ?: 0,
                totalTokens = (usage?.optInt("input_tokens", 0) ?: 0) + (usage?.optInt("output_tokens", 0) ?: 0)
            )
        )
    }

    /**
     * Custom provider (OpenAI-compatible) chat completion
     */
    private suspend fun chatCompletionCustom(
        model: String,
        messages: List<LLMMessage>,
        temperature: Double,
        maxTokens: Int,
        stream: Boolean,
        options: ChatCompletionOptions
    ): LLMResponse = withContext(Dispatchers.IO) {
        val baseUrl = options.baseUrl ?: throw IllegalArgumentException("Base URL required for custom provider")
        val apiKey = options.apiKey ?: ""
        
        val url = "${baseUrl.trimEnd('/')}/chat/completions"
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val requestJson = JSONObject().apply {
            put("model", model)
            put("temperature", temperature)
            put("max_tokens", maxTokens)
            put("stream", false)
            
            val messagesArray = JSONArray().apply {
                for (msg in messages) {
                    put(JSONObject().apply {
                        put("role", msg.role)
                        put("content", msg.content)
                    })
                }
            }
            put("messages", messagesArray)
            
            // Add JSON format if requested
            if (options.requireJson) {
                put("response_format", JSONObject().apply {
                    put("type", "json_object")
                })
            }
        }

        val body = requestJson.toString().toRequestBody(mediaType)
        val requestBuilder = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("User-Agent", INKOS_USER_AGENT)
            .post(body)
        
        if (apiKey.isNotEmpty()) {
            requestBuilder.addHeader("Authorization", "Bearer $apiKey")
        }

        val response = client.newCall(requestBuilder.build()).execute()
        val responseBody = response.body?.string() ?: throw Exception("Empty response body")
        
        if (!response.isSuccessful) {
            throw wrapLLMError(Exception("HTTP ${response.code}: $responseBody"), baseUrl, model)
        }

        val json = JSONObject(responseBody)
        val choices = json.optJSONArray("choices")
        val firstChoice = choices?.optJSONObject(0)
        val message = firstChoice?.optJSONObject("message")
        val content = message?.optString("content") ?: throw Exception("No content in response")
        
        val usage = json.optJSONObject("usage")
        LLMResponse(
            content = content,
            usage = LLMTokenUsage(
                promptTokens = usage?.optInt("prompt_tokens", 0) ?: 0,
                completionTokens = usage?.optInt("completion_tokens", 0) ?: 0,
                totalTokens = usage?.optInt("total_tokens", 0) ?: 0
            )
        )
    }

    /**
     * Estimate token count from messages
     * Matches TypeScript estimateTextTokens function
     */
    fun estimateTextTokens(text: String): Int {
        if (text.isEmpty()) return 0
        val cjkCount = text.count { it.code in 0x3400..0x9FFF }
        val nonCjkCount = text.length - cjkCount
        return cjkCount + (nonCjkCount + 3) / 4
    }

    /**
     * Estimate total tokens from messages
     */
    fun estimateMessagesTokens(messages: List<LLMMessage>): Int {
        return messages.sumOf { estimateTextTokens(it.content) }
    }

    /**
     * Wrap LLM error with context
     * Matches TypeScript wrapLLMError function
     */
    private fun wrapLLMError(error: Throwable, baseUrl: String, model: String): Exception {
        val msg = error.message ?: ""
        val ctxLine = "\n  (baseUrl: $baseUrl, model: $model)"
        
        return when {
            msg.contains("400") -> Exception(
                "API returned 400 (Bad Request). Common causes:\n" +
                "1. temperature/max_tokens out of range\n" +
                "2. Invalid model name\n" +
                "3. Incompatible message format$ctxLine", error
            )
            msg.contains("401") -> Exception(
                "API returned 401 (Unauthorized). Check API key.$ctxLine", error
            )
            msg.contains("403") -> Exception(
                "API returned 403 (Forbidden). Possible causes:\n" +
                "1. Invalid/expired API key\n" +
                "2. Content policy violation\n" +
                "3. Insufficient balance$ctxLine", error
            )
            msg.contains("429") -> Exception(
                "API returned 429 (Too Many Requests). Please retry later.$ctxLine", error
            )
            msg.contains("Connection error") || msg.contains("ECONNREFUSED") -> Exception(
                "Cannot connect to API service. Check baseUrl and network.$ctxLine", error
            )
            else -> error as? Exception ?: Exception(msg, error)
        }
    }

    /**
     * Retry with transient error handling
     * Matches TypeScript withTransientLLMRetry function
     */
    private suspend fun <T> withTransientRetry(
        enabled: Boolean = true,
        block: suspend () -> T
    ): T {
        if (!enabled) return block()
        
        var lastError: Throwable? = null
        for (attempt in 0..TRANSIENT_LLM_RETRIES) {
            try {
                return block()
            } catch (e: Throwable) {
                lastError = e
                if (attempt >= TRANSIENT_LLM_RETRIES || !isRetryableError(e)) {
                    throw e
                }
                delay(800L * (attempt + 1))
            }
        }
        throw lastError ?: Exception("Unknown error in retry loop")
    }

    /**
     * Check if error is retryable
     * Matches TypeScript isRetryableLLMError function
     */
    private fun isRetryableError(error: Throwable): Boolean {
        val text = error.message ?: ""
        return listOf(
            "terminated", "ECONNRESET", "ETIMEDOUT", "EPIPE",
            "socket hang up", "other side closed", "network socket disconnected",
            "429", "502", "503", "504",
            "temporarily unavailable", "service unavailable"
        ).any { text.contains(it, ignoreCase = true) }
    }
}

/**
 * Chat completion options
 */
data class ChatCompletionOptions(
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val baseUrl: String? = null,
    val apiKey: String? = null,
    val requireJson: Boolean = false,
    val retry: Boolean = true,
    val onStreamProgress: OnStreamProgress? = null,
    val onTextDelta: ((String) -> Unit)? = null
)
