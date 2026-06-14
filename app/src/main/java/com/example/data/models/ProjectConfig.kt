package com.example.data.models

import org.json.JSONObject

/**
 * ProjectConfig - Project configuration models.
 *
 * This is the Kotlin Android equivalent of the TypeScript project.ts models.
 * It contains:
 * - LLMConfig - LLM configuration
 * - NotifyChannel - Notification channel configuration
 * - DetectionConfig - AI detection configuration
 * - ProjectConfig - Main project configuration
 */

// LLM Configuration

data class LLMServiceEntry(
    val service: String,
    val name: String? = null,
    val baseUrl: String? = null,
    val temperature: Double? = null,
    val apiFormat: String? = null, // "chat", "responses"
    val stream: Boolean? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("service", service)
            name?.let { put("name", it) }
            baseUrl?.let { put("baseUrl", it) }
            temperature?.let { put("temperature", it) }
            apiFormat?.let { put("apiFormat", it) }
            stream?.let { put("stream", it) }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): LLMServiceEntry {
            return LLMServiceEntry(
                service = json.getString("service"),
                name = json.optString("name", null),
                baseUrl = json.optString("baseUrl", null),
                temperature = json.optDouble("temperature", Double.NaN).takeIf { !it.isNaN() },
                apiFormat = json.optString("apiFormat", null),
                stream = if (json.has("stream") && !json.isNull("stream")) json.getBoolean("stream") else null
            )
        }
    }
}

data class LLMCoverConfig(
    val service: String, // "kkaiapi", "openai", "google"
    val model: String
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("service", service)
            put("model", model)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): LLMCoverConfig {
            return LLMCoverConfig(
                service = json.getString("service"),
                model = json.getString("model")
            )
        }
    }
}

data class LLMConfig(
    val provider: String, // "anthropic", "openai", "custom"
    val service: String = "custom",
    val configSource: String = "env", // "env", "studio"
    val baseUrl: String,
    val apiKey: String = "",
    val model: String,
    val proxyUrl: String? = null,
    val temperature: Double = 0.7,
    val thinkingBudget: Int = 0,
    val extra: Map<String, Any?>? = null,
    val headers: Map<String, String>? = null,
    val apiFormat: String = "chat", // "chat", "responses"
    val stream: Boolean = true,
    val services: List<LLMServiceEntry>? = null,
    val defaultModel: String? = null,
    val cover: LLMCoverConfig? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("provider", provider)
            put("service", service)
            put("configSource", configSource)
            put("baseUrl", baseUrl)
            put("apiKey", apiKey)
            put("model", model)
            proxyUrl?.let { put("proxyUrl", it) }
            put("temperature", temperature)
            put("thinkingBudget", thinkingBudget)
            extra?.let { put("extra", JSONObject(it)) }
            headers?.let { put("headers", JSONObject(it)) }
            put("apiFormat", apiFormat)
            put("stream", stream)
            services?.let {
                put("services", org.json.JSONArray().apply {
                    it.forEach { service -> put(service.toJson()) }
                })
            }
            defaultModel?.let { put("defaultModel", it) }
            cover?.let { put("cover", it.toJson()) }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): LLMConfig {
            return LLMConfig(
                provider = json.getString("provider"),
                service = json.optString("service", "custom"),
                configSource = json.optString("configSource", "env"),
                baseUrl = json.getString("baseUrl"),
                apiKey = json.optString("apiKey", ""),
                model = json.getString("model"),
                proxyUrl = json.optString("proxyUrl", null),
                temperature = json.optDouble("temperature", 0.7),
                thinkingBudget = json.optInt("thinkingBudget", 0),
                extra = json.optJSONObject("extra")?.let { obj ->
                    val map = mutableMapOf<String, Any?>()
                    obj.keys().forEach { key -> map[key] = obj.get(key) }
                    map
                },
                headers = json.optJSONObject("headers")?.let { obj ->
                    val map = mutableMapOf<String, String>()
                    obj.keys().forEach { key -> map[key] = obj.getString(key) }
                    map
                },
                apiFormat = json.optString("apiFormat", "chat"),
                stream = json.optBoolean("stream", true),
                services = json.optJSONArray("services")?.let { array ->
                    (0 until array.length()).map { i ->
                        LLMServiceEntry.fromJson(array.getJSONObject(i))
                    }
                },
                defaultModel = json.optString("defaultModel", null),
                cover = json.optJSONObject("cover")?.let { LLMCoverConfig.fromJson(it) }
            )
        }
    }
}

// Notification Channel

sealed class NotifyChannel {
    abstract val type: String

    data class Telegram(
        val botToken: String,
        val chatId: String
    ) : NotifyChannel() {
        override val type = "telegram"

        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("type", type)
                put("botToken", botToken)
                put("chatId", chatId)
            }
        }
    }

    data class WechatWork(
        val webhookUrl: String
    ) : NotifyChannel() {
        override val type = "wechat-work"

        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("type", type)
                put("webhookUrl", webhookUrl)
            }
        }
    }

    data class Feishu(
        val webhookUrl: String
    ) : NotifyChannel() {
        override val type = "feishu"

        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("type", type)
                put("webhookUrl", webhookUrl)
            }
        }
    }

    data class Webhook(
        val url: String,
        val secret: String? = null,
        val events: List<String> = emptyList()
    ) : NotifyChannel() {
        override val type = "webhook"

        fun toJson(): JSONObject {
            return JSONObject().apply {
                put("type", type)
                put("url", url)
                secret?.let { put("secret", it) }
                put("events", org.json.JSONArray(events))
            }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): NotifyChannel {
            return when (json.getString("type")) {
                "telegram" -> Telegram(
                    botToken = json.getString("botToken"),
                    chatId = json.getString("chatId")
                )
                "wechat-work" -> WechatWork(
                    webhookUrl = json.getString("webhookUrl")
                )
                "feishu" -> Feishu(
                    webhookUrl = json.getString("webhookUrl")
                )
                "webhook" -> Webhook(
                    url = json.getString("url"),
                    secret = json.optString("secret", null),
                    events = json.optJSONArray("events")?.let { array ->
                        (0 until array.length()).map { array.getString(it) }
                    } ?: emptyList()
                )
                else -> throw IllegalArgumentException("Unknown notify channel type: ${json.getString("type")}")
            }
        }
    }
}

// Detection Configuration

data class DetectionConfig(
    val provider: String = "custom", // "gptzero", "originality", "custom"
    val apiUrl: String,
    val apiKeyEnv: String,
    val threshold: Double = 0.5,
    val enabled: Boolean = false,
    val autoRewrite: Boolean = false,
    val maxRetries: Int = 3
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("provider", provider)
            put("apiUrl", apiUrl)
            put("apiKeyEnv", apiKeyEnv)
            put("threshold", threshold)
            put("enabled", enabled)
            put("autoRewrite", autoRewrite)
            put("maxRetries", maxRetries)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DetectionConfig {
            return DetectionConfig(
                provider = json.optString("provider", "custom"),
                apiUrl = json.getString("apiUrl"),
                apiKeyEnv = json.getString("apiKeyEnv"),
                threshold = json.optDouble("threshold", 0.5),
                enabled = json.optBoolean("enabled", false),
                autoRewrite = json.optBoolean("autoRewrite", false),
                maxRetries = json.optInt("maxRetries", 3)
            )
        }
    }
}

// Quality Gates

data class QualityGates(
    val maxAuditRetries: Int = 2,
    val pauseAfterConsecutiveFailures: Int = 3,
    val retryTemperatureStep: Double = 0.1
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("maxAuditRetries", maxAuditRetries)
            put("pauseAfterConsecutiveFailures", pauseAfterConsecutiveFailures)
            put("retryTemperatureStep", retryTemperatureStep)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): QualityGates {
            return QualityGates(
                maxAuditRetries = json.optInt("maxAuditRetries", 2),
                pauseAfterConsecutiveFailures = json.optInt("pauseAfterConsecutiveFailures", 3),
                retryTemperatureStep = json.optDouble("retryTemperatureStep", 0.1)
            )
        }
    }
}

// Foundation Configuration

data class FoundationConfig(
    val reviewRetries: Int = 2
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("reviewRetries", reviewRetries)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): FoundationConfig {
            return FoundationConfig(
                reviewRetries = json.optInt("reviewRetries", 2)
            )
        }
    }
}

// Writing Configuration

data class WritingConfig(
    val reviewRetries: Int = 1,
    val reviewMode: String = "auto" // "auto", "manual"
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("reviewRetries", reviewRetries)
            put("reviewMode", reviewMode)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): WritingConfig {
            return WritingConfig(
                reviewRetries = json.optInt("reviewRetries", 1),
                reviewMode = json.optString("reviewMode", "auto")
            )
        }
    }
}

// Agent LLM Override

data class AgentLLMOverride(
    val model: String,
    val provider: String? = null, // "anthropic", "openai", "custom"
    val baseUrl: String? = null,
    val apiKeyEnv: String? = null,
    val stream: Boolean? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("model", model)
            provider?.let { put("provider", it) }
            baseUrl?.let { put("baseUrl", it) }
            apiKeyEnv?.let { put("apiKeyEnv", it) }
            stream?.let { put("stream", it) }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): AgentLLMOverride {
            return AgentLLMOverride(
                model = json.getString("model"),
                provider = json.optString("provider", null),
                baseUrl = json.optString("baseUrl", null),
                apiKeyEnv = json.optString("apiKeyEnv", null),
                stream = if (json.has("stream") && !json.isNull("stream")) json.getBoolean("stream") else null
            )
        }
    }
}

// Daemon Configuration

data class DaemonSchedule(
    val radarCron: String = "0 */6 * * *",
    val writeCron: String = "*/15 * * * *"
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("radarCron", radarCron)
            put("writeCron", writeCron)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DaemonSchedule {
            return DaemonSchedule(
                radarCron = json.optString("radarCron", "0 */6 * * *"),
                writeCron = json.optString("writeCron", "*/15 * * * *")
            )
        }
    }
}

data class DaemonConfig(
    val schedule: DaemonSchedule = DaemonSchedule(),
    val maxConcurrentBooks: Int = 3,
    val chaptersPerCycle: Int = 1,
    val retryDelayMs: Int = 30000,
    val cooldownAfterChapterMs: Int = 10000,
    val maxChaptersPerDay: Int = 50,
    val qualityGates: QualityGates = QualityGates()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("schedule", schedule.toJson())
            put("maxConcurrentBooks", maxConcurrentBooks)
            put("chaptersPerCycle", chaptersPerCycle)
            put("retryDelayMs", retryDelayMs)
            put("cooldownAfterChapterMs", cooldownAfterChapterMs)
            put("maxChaptersPerDay", maxChaptersPerDay)
            put("qualityGates", qualityGates.toJson())
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DaemonConfig {
            return DaemonConfig(
                schedule = json.optJSONObject("schedule")?.let { DaemonSchedule.fromJson(it) } ?: DaemonSchedule(),
                maxConcurrentBooks = json.optInt("maxConcurrentBooks", 3),
                chaptersPerCycle = json.optInt("chaptersPerCycle", 1),
                retryDelayMs = json.optInt("retryDelayMs", 30000),
                cooldownAfterChapterMs = json.optInt("cooldownAfterChapterMs", 10000),
                maxChaptersPerDay = json.optInt("maxChaptersPerDay", 50),
                qualityGates = json.optJSONObject("qualityGates")?.let { QualityGates.fromJson(it) } ?: QualityGates()
            )
        }
    }
}

// Main Project Configuration

data class ProjectConfig(
    val name: String,
    val version: String = "0.1.0",
    val language: String = "zh", // "zh", "en"
    val llm: LLMConfig,
    val notify: List<NotifyChannel> = emptyList(),
    val detection: DetectionConfig? = null,
    val foundation: FoundationConfig = FoundationConfig(),
    val writing: WritingConfig = WritingConfig(),
    val modelOverrides: Map<String, Any?>? = null,
    val inputGovernanceMode: String = "v2", // "legacy", "v2"
    val daemon: DaemonConfig = DaemonConfig()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("version", version)
            put("language", language)
            put("llm", llm.toJson())
            put("notify", org.json.JSONArray().apply {
                notify.forEach { channel ->
                    when (channel) {
                        is NotifyChannel.Telegram -> put(channel.toJson())
                        is NotifyChannel.WechatWork -> put(channel.toJson())
                        is NotifyChannel.Feishu -> put(channel.toJson())
                        is NotifyChannel.Webhook -> put(channel.toJson())
                    }
                }
            })
            detection?.let { put("detection", it.toJson()) }
            put("foundation", foundation.toJson())
            put("writing", writing.toJson())
            modelOverrides?.let { put("modelOverrides", JSONObject(it)) }
            put("inputGovernanceMode", inputGovernanceMode)
            put("daemon", daemon.toJson())
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ProjectConfig {
            return ProjectConfig(
                name = json.getString("name"),
                version = json.optString("version", "0.1.0"),
                language = json.optString("language", "zh"),
                llm = LLMConfig.fromJson(json.getJSONObject("llm")),
                notify = json.optJSONArray("notify")?.let { array ->
                    (0 until array.length()).map { i ->
                        NotifyChannel.fromJson(array.getJSONObject(i))
                    }
                } ?: emptyList(),
                detection = json.optJSONObject("detection")?.let { DetectionConfig.fromJson(it) },
                foundation = json.optJSONObject("foundation")?.let { FoundationConfig.fromJson(it) } ?: FoundationConfig(),
                writing = json.optJSONObject("writing")?.let { WritingConfig.fromJson(it) } ?: WritingConfig(),
                modelOverrides = json.optJSONObject("modelOverrides")?.let { obj ->
                    val map = mutableMapOf<String, Any?>()
                    obj.keys().forEach { key -> map[key] = obj.get(key) }
                    map
                },
                inputGovernanceMode = json.optString("inputGovernanceMode", "v2"),
                daemon = json.optJSONObject("daemon")?.let { DaemonConfig.fromJson(it) } ?: DaemonConfig()
            )
        }
    }
}
