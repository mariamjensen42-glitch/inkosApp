package com.example.data.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

/**
 * WebSearch - Web search utilities.
 */

data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String
)

class WebSearch {

    companion object {
        private const val TAVILY_API_URL = "https://api.tavily.com/search"
        private const val TIMEOUT = 15000

        suspend fun searchWeb(query: String, maxResults: Int = 5): List<SearchResult> = withContext(Dispatchers.IO) {
            val apiKey = System.getenv("TAVILY_API_KEY")
                ?: throw IllegalStateException("TAVILY_API_KEY not set")

            val url = URL(TAVILY_API_URL)
            val connection = url.openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.connectTimeout = TIMEOUT
                connection.readTimeout = TIMEOUT

                val requestBody = JSONObject().apply {
                    put("api_key", apiKey)
                    put("query", query)
                    put("max_results", maxResults)
                    put("search_depth", "basic")
                }

                connection.outputStream.use { os ->
                    os.write(requestBody.toString().toByteArray())
                }

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    val errorText = try {
                        BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    } catch (e: Exception) {
                        ""
                    }
                    throw RuntimeException("Tavily search failed: $responseCode $errorText")
                }

                val responseText = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val response = JSONObject(responseText)
                val results = response.optJSONArray("results") ?: return@withContext emptyList()

                (0 until results.length()).map { i ->
                    val result = results.getJSONObject(i)
                    SearchResult(
                        title = result.optString("title", ""),
                        url = result.optString("url", ""),
                        snippet = result.optString("content", "")
                    )
                }
            } finally {
                connection.disconnect()
            }
        }

        suspend fun fetchUrl(url: String, maxChars: Int = 8000): String = withContext(Dispatchers.IO) {
            val connection = URL(url).openConnection() as HttpURLConnection

            try {
                connection.requestMethod = "GET"
                connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                connection.setRequestProperty("Accept", "text/html, application/json, text/plain")
                connection.connectTimeout = TIMEOUT
                connection.readTimeout = TIMEOUT

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw RuntimeException("Fetch failed: $responseCode ${connection.responseMessage}")
                }

                val contentType = connection.contentType ?: ""
                val text = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }

                if (contentType.contains("html")) {
                    text
                        .replace(Regex("<script[\\s\\S]*?</script>", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("<style[\\s\\S]*?</style>", RegexOption.IGNORE_CASE), "")
                        .replace(Regex("<[^>]*>"), " ")
                        .replace(Regex("\\s+"), " ")
                        .trim()
                        .take(maxChars)
                } else {
                    text.take(maxChars)
                }
            } finally {
                connection.disconnect()
            }
        }
    }
}
