package com.example.data.state

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * StateManager - Manages book state and control documents.
 */

class StateManager(private val context: Context, private val projectRoot: java.io.File) {

    companion object {
        private const val BOOKS_DIR = "books"
        private const val STORY_DIR = "story"
    }

    val booksDir: java.io.File
        get() = java.io.File(projectRoot, BOOKS_DIR)

    fun bookDir(bookId: String): java.io.File {
        return java.io.File(booksDir, bookId)
    }

    suspend fun listBooks(): List<String> = withContext(Dispatchers.IO) {
        try {
            booksDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getNextChapterNumber(bookId: String): Int = withContext(Dispatchers.IO) {
        val chaptersDir = java.io.File(bookDir(bookId), "chapters")
        val chapterNumbers = mutableSetOf<Int>()

        try {
            chaptersDir.listFiles()?.forEach { file ->
                val match = Regex("^(\\d+)_.*\\.md$").find(file.name)
                if (match != null) {
                    chapterNumbers.add(match.groupValues[1].toInt())
                }
            }
        } catch (e: Exception) {
            // Directory doesn't exist
        }

        (chapterNumbers.maxOrNull() ?: 0) + 1
    }

    suspend fun loadBookConfig(bookId: String): BookConfig = withContext(Dispatchers.IO) {
        val configPath = java.io.File(bookDir(bookId), "book.json")
        val raw = configPath.readText()
        BookConfig.fromJson(org.json.JSONObject(raw))
    }

    suspend fun saveBookConfig(bookId: String, config: BookConfig) = withContext(Dispatchers.IO) {
        val bookDir = bookDir(bookId)
        bookDir.mkdirs()
        java.io.File(bookDir, "book.json").writeText(config.toJson().toString(2))
    }
}

/**
 * BookConfig - Book configuration data class
 */
data class BookConfig(
    val id: String,
    val title: String,
    val genre: String = "",
    val language: String = "zh",
    val status: String = "DRAFT",
    val currentChapterIndex: Int = 1,
    val totalChapters: Int = 12,
    val targetWordsPerChapter: Int = 1000
) {
    fun toJson(): org.json.JSONObject {
        return org.json.JSONObject().apply {
            put("id", id)
            put("title", title)
            put("genre", genre)
            put("language", language)
            put("status", status)
            put("currentChapterIndex", currentChapterIndex)
            put("totalChapters", totalChapters)
            put("targetWordsPerChapter", targetWordsPerChapter)
        }
    }

    companion object {
        fun fromJson(json: org.json.JSONObject): BookConfig {
            return BookConfig(
                id = json.getString("id"),
                title = json.getString("title"),
                genre = json.optString("genre", ""),
                language = json.optString("language", "zh"),
                status = json.optString("status", "DRAFT"),
                currentChapterIndex = json.optInt("currentChapterIndex", 1),
                totalChapters = json.optInt("totalChapters", 12),
                targetWordsPerChapter = json.optInt("targetWordsPerChapter", 1000)
            )
        }
    }
}
