package com.example.data.notify

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * NotificationDispatcher - Dispatches notifications to various channels.
 */

data class NotifyMessage(
    val title: String,
    val body: String
)

data class NotifyChannel(
    val type: String,
    val botToken: String? = null,
    val chatId: String? = null,
    val webhookUrl: String? = null,
    val url: String? = null,
    val secret: String? = null
)

class NotificationDispatcher(private val context: Context) {

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
                }
            } catch (e: Exception) {
                android.util.Log.e("NotificationDispatcher", "${channel.type} failed: ${e.message}")
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
}
