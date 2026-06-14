package com.example.data.interaction

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.time.Instant

/**
 * BookSessionStore - Book session store for managing sessions.
 *
 * This is the Kotlin Android equivalent of the TypeScript book-session-store.ts module.
 * It handles:
 * - Loading and saving book sessions
 * - Listing book sessions
 * - Creating and persisting sessions
 * - Migrating legacy sessions
 */

data class BookSessionSummary(
    val sessionId: String,
    val bookId: String?,
    val sessionKind: SessionKind? = null,
    val playMode: PlayMode? = null,
    val title: String?,
    val messageCount: Int,
    val createdAt: Long,
    val updatedAt: Long
)

/**
 * BookSessionStore - Main class for managing book sessions.
 */
class BookSessionStore(private val context: Context, private val projectRoot: File) {

    companion object {
        private const val SESSIONS_DIR = "sessions"

        /**
         * Extract first user message title from messages array.
         */
        fun extractFirstUserMessageTitle(messages: List<InteractionMessage>): String? {
            for (message in messages) {
                if (message.role == "user") {
                    val oneLine = message.content.trim().replace(Regex("\\s+"), " ")
                    if (oneLine.isEmpty()) return null
                    return if (oneLine.length > 20) "${oneLine.take(20)}…" else oneLine
                }
            }
            return null
        }
    }

    private val sessionsDir: File
        get() = File(projectRoot, SESSIONS_DIR)

    suspend fun loadBookSession(sessionId: String): BookSession? = withContext(Dispatchers.IO) {
        val sessionFile = File(sessionsDir, "$sessionId.json")
        if (!sessionFile.exists()) {
            return@withContext null
        }

        try {
            val json = JSONObject(sessionFile.readText())
            BookSession.fromJson(json)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun persistBookSession(session: BookSession) = withContext(Dispatchers.IO) {
        sessionsDir.mkdirs()
        val sessionFile = File(sessionsDir, "${session.sessionId}.json")
        sessionFile.writeText(session.toJson().toString(2))
    }

    suspend fun listBookSessions(bookId: String?): List<BookSessionSummary> = withContext(Dispatchers.IO) {
        if (!sessionsDir.exists()) {
            return@withContext emptyList()
        }

        val sessionFiles = sessionsDir.listFiles()?.filter { it.extension == "json" } ?: emptyList()

        sessionFiles.mapNotNull { file ->
            try {
                val session = loadBookSession(file.nameWithoutExtension) ?: return@mapNotNull null
                if (session.bookId != bookId) return@mapNotNull null

                BookSessionSummary(
                    sessionId = session.sessionId,
                    bookId = session.bookId,
                    sessionKind = session.sessionKind,
                    playMode = session.playMode,
                    title = session.title,
                    messageCount = session.messages.size,
                    createdAt = session.createdAt,
                    updatedAt = session.updatedAt
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.updatedAt }
    }

    suspend fun renameBookSession(sessionId: String, title: String): BookSession? = withContext(Dispatchers.IO) {
        val session = loadBookSession(sessionId) ?: return@withContext null
        val updatedAt = System.currentTimeMillis()
        val updatedSession = session.copy(title = title, updatedAt = updatedAt)
        persistBookSession(updatedSession)
        updatedSession
    }

    suspend fun deleteBookSession(sessionId: String) = withContext(Dispatchers.IO) {
        val sessionFile = File(sessionsDir, "$sessionId.json")
        if (sessionFile.exists()) {
            sessionFile.delete()
        }
    }

    suspend fun migrateBookSession(sessionId: String, newBookId: String): BookSession? = withContext(Dispatchers.IO) {
        val session = loadBookSession(sessionId) ?: return@withContext null
        if (session.bookId != null) {
            throw IllegalStateException("Session \"$sessionId\" is already bound to book \"${session.bookId}\"")
        }

        val updatedAt = System.currentTimeMillis()
        val updatedSession = session.copy(
            bookId = newBookId,
            sessionKind = SessionKind.BOOK,
            updatedAt = updatedAt
        )
        persistBookSession(updatedSession)
        updatedSession
    }

    suspend fun createAndPersistBookSession(
        bookId: String?,
        sessionId: String? = null,
        sessionKind: SessionKind? = null,
        playMode: PlayMode? = null
    ): BookSession = withContext(Dispatchers.IO) {
        // If sessionId is specified and file exists, return existing session (idempotent)
        if (sessionId != null) {
            val existing = loadBookSession(sessionId)
            if (existing != null) {
                if ((sessionKind != null && existing.sessionKind != sessionKind) || 
                    (playMode != null && existing.playMode != playMode)) {
                    val updatedAt = System.currentTimeMillis()
                    val updatedSession = existing.copy(
                        sessionKind = sessionKind ?: existing.sessionKind,
                        playMode = playMode ?: existing.playMode,
                        updatedAt = updatedAt
                    )
                    persistBookSession(updatedSession)
                    return@withContext updatedSession
                }
                return@withContext existing
            }
        }

        val session = createBookSession(bookId, sessionId, sessionKind, playMode)
        persistBookSession(session)
        session
    }
}
