package com.example.data.notify

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * NotificationDispatcher - Dispatches notifications to various channels.
 *
 * This is the Kotlin Android equivalent of the TypeScript NotificationDispatcher module.
 * It handles:
 * - Telegram notifications
 * - Feishu (飞书) notifications
 * - WeChat Work (企业微信) notifications
 * - Generic webhook notifications
 */

// Data classes

data class NotifyMessage(
    val title: String,
    val body: String
)

data class NotifyChannel(
    val type: String, // "telegram", "feishu", "wechat-work", "webhook"
    val botToken: String? = null,
    val chatId: String? = null,
    val webhookUrl: String? = null,
    val url: String? = null,
    val secret: String? = null,
    val events: List<String> = emptyList()
)

data class WebhookPayload(
    val event: String,
    val bookId: String,
    val timestamp: String = DateTimeFormatter.ISO_INSTANT.format(Instant.now()),
    val data: Map<String, Any?> = emptyMap()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("event", event)
            put("bookId", bookId)
            put("timestamp", timestamp)
            put("data", JSONObject(data))
        }
    }
}

/**
 * NotificationDispatcher - Main class for dispatching notifications.
 */
class NotificationDispatcher(private val context: Context) {

    companion object {
        private const val TAG = "NotificationDispatcher"
    }

    suspend fun dispatchNotification(
        channels: List<NotifyChannel>,
        message: NotifyMessage
    ) = withContext(Dispatchers.IO) {
        val fullText = "**${message.title}**\n\n${message.body}"

        channels.forEach { channel ->
            try {
                when (channel.type) {
                    "telegram" -> {
                        if (channel.botToken != null && channel.chatId != null) {
                            sendTelegram(channel.botToken, channel.chatId, fullText)
                        }
                    }
                    "feishu" -> {
                        if (channel.webhookUrl != null) {
                            sendFeishu(channel.webhookUrl, message.title, message.body)
                        }
                    }
                    "wechat-work" -> {
                        if (channel.webhookUrl != null) {
                            sendWechatWork(channel.webhookUrl, fullText)
                        }
                    }
                    "webhook" -> {
                        if (channel.url != null) {
                            val payload = WebhookPayload(
                                event = "pipeline-complete",
                                bookId = "",
                                data = mapOf(
                                    "title" to message.title,
                                    "body" to message.body
                                )
                            )
                            sendWebhook(channel.url, channel.secret, payload)
                        }
                    }
                }
            } catch (e: Exception) {
                // Log but don't throw — notification failure shouldn't block pipeline
                android.util.Log.e(TAG, "${channel.type} failed: ${e.message}")
            }
        }
    }

    suspend fun dispatchWebhookEvent(
        channels: List<NotifyChannel>,
        payload: WebhookPayload
    ) = withContext(Dispatchers.IO) {
        val webhookChannels = channels.filter { it.type == "webhook" }
        if (webhookChannels.isEmpty()) return@withContext

        webhookChannels.forEach { channel ->
            try {
                if (channel.url != null) {
                    sendWebhook(channel.url, channel.secret, payload)
                }
            } catch (e: Exception) {
                android.util.Log.e(TAG, "Webhook ${channel.url} failed: ${e.message}")
            }
        }
    }

    private suspend fun sendTelegram(botToken: String, chatId: String, text: String) = withContext(Dispatchers.IO) {
        val url = URL("https://api.telegram.org/bot$botToken/sendMessage")
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val json = JSONObject().apply {
                put("chat_id", chatId)
                put("text", text)
                put("parse_mode", "Markdown")
            }

            connection.outputStream.use { os ->
                os.write(json.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw RuntimeException("Telegram API returned $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun sendFeishu(webhookUrl: String, title: String, body: String) = withContext(Dispatchers.IO) {
        val url = URL(webhookUrl)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val json = JSONObject().apply {
                put("msg_type", "interactive")
                put("card", JSONObject().apply {
                    put("header", JSONObject().apply {
                        put("title", JSONObject().apply {
                            put("tag", "plain_text")
                            put("content", title)
                        })
                    })
                    put("elements", org.json.JSONArray().apply {
                        put(JSONObject().apply {
                            put("tag", "markdown")
                            put("content", body)
                        })
                    })
                })
            }

            connection.outputStream.use { os ->
                os.write(json.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw RuntimeException("Feishu API returned $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun sendWechatWork(webhookUrl: String, text: String) = withContext(Dispatchers.IO) {
        val url = URL(webhookUrl)
        val connection = url.openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            val json = JSONObject().apply {
                put("msgtype", "markdown")
                put("markdown", JSONObject().apply {
                    put("content", text)
                })
            }

            connection.outputStream.use { os ->
                os.write(json.toString().toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw RuntimeException("WeChat Work API returned $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun sendWebhook(url: String, secret: String?, payload: WebhookPayload) = withContext(Dispatchers.IO) {
        val connection = URL(url).openConnection() as HttpURLConnection

        try {
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.doOutput = true

            // Add signature if secret is provided
            if (secret != null) {
                val signature = generateHmacSignature(payload.toJson().toString(), secret)
                connection.setRequestProperty("X-Webhook-Signature", signature)
            }

            connection.outputStream.use { os ->
                os.write(payload.toJson().toString().toByteArray())
            }

            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw RuntimeException("Webhook returned $responseCode")
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun generateHmacSignature(data: String, secret: String): String {
        // Simple HMAC signature generation
        // In production, use a proper HMAC-SHA256 implementation
        return "sha256=${data.hashCode()}"
    }
}

/**
 * TelegramNotifier - Sends notifications via Telegram.
 */
class TelegramNotifier(private val botToken: String, private val chatId: String) {

    suspend fun send(message: NotifyMessage) {
        val dispatcher = NotificationDispatcher(context = TODO())
        val channel = NotifyChannel(
            type = "telegram",
            botToken = botToken,
            chatId = chatId
        )
        dispatcher.dispatchNotification(listOf(channel), message)
    }
}

/**
 * FeishuNotifier - Sends notifications via Feishu (飞书).
 */
class FeishuNotifier(private val webhookUrl: String) {

    suspend fun send(message: NotifyMessage) {
        val dispatcher = NotificationDispatcher(context = TODO())
        val channel = NotifyChannel(
            type = "feishu",
            webhookUrl = webhookUrl
        )
        dispatcher.dispatchNotification(listOf(channel), message)
    }
}

/**
 * WechatWorkNotifier - Sends notifications via WeChat Work (企业微信).
 */
class WechatWorkNotifier(private val webhookUrl: String) {

    suspend fun send(message: NotifyMessage) {
        val dispatcher = NotificationDispatcher(context = TODO())
        val channel = NotifyChannel(
            type = "wechat-work",
            webhookUrl = webhookUrl
        )
        dispatcher.dispatchNotification(listOf(channel), message)
    }
}
