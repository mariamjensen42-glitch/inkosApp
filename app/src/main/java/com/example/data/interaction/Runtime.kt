package com.example.data.interaction

import android.content.Context
import com.example.data.state.StateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Runtime - Interaction runtime for managing sessions and executing requests.
 *
 * This is the Kotlin Android equivalent of the TypeScript runtime.ts module.
 * It handles:
 * - Processing interaction requests
 * - Managing session state
 * - Executing tools and operations
 * - Handling automation modes
 */

typealias ReviseMode = String // "local-fix", "rewrite"
typealias RuntimeLanguage = String // "zh", "en"

interface InteractionRuntimeTools {
    suspend fun listBooks(): List<String>
    suspend fun createBook(input: CreateBookInput): Any?
    suspend fun exportBook(bookId: String, options: ExportOptions): Any?
    suspend fun chat(input: String, options: ChatOptions): Any?
    suspend fun writeNextChapter(bookId: String): Any?
    suspend fun reviseDraft(bookId: String, chapterNumber: Int, mode: ReviseMode): Any?
    suspend fun patchChapterText(bookId: String, chapterNumber: Int, targetText: String, replacementText: String): Any?
    suspend fun replaceChapterText(bookId: String, chapterNumber: Int, fullText: String): Any?
    suspend fun renameEntity(bookId: String, oldValue: String, newValue: String): Any?
    suspend fun updateCurrentFocus(bookId: String, content: String): Any?
    suspend fun updateAuthorIntent(bookId: String, content: String): Any?
    suspend fun writeTruthFile(bookId: String, fileName: String, content: String): Any?
}

data class CreateBookInput(
    val title: String,
    val genre: String? = null,
    val platform: String? = null,
    val language: String? = null, // "zh", "en"
    val chapterWordCount: Int? = null,
    val targetChapters: Int? = null,
    val blurb: String? = null,
    val worldPremise: String? = null,
    val settingNotes: String? = null,
    val protagonist: String? = null,
    val supportingCast: String? = null,
    val conflictCore: String? = null,
    val volumeOutline: String? = null,
    val constraints: String? = null,
    val authorIntent: String? = null,
    val currentFocus: String? = null
)

data class ExportOptions(
    val format: String? = null, // "txt", "md", "epub"
    val approvedOnly: Boolean? = null,
    val outputPath: String? = null
)

data class ChatOptions(
    val bookId: String? = null,
    val automationMode: AutomationMode
)

data class InteractionRuntimeResult(
    val session: InteractionSession,
    val responseText: String? = null,
    val details: Map<String, Any?>? = null
)

data class InteractionToolMetadata(
    val events: List<InteractionEvent>? = null,
    val activeChapterNumber: Int? = null,
    val currentExecution: ExecutionState? = null,
    val pendingDecision: PendingDecision? = null,
    val responseText: String? = null,
    val details: Map<String, Any?>? = null
)

/**
 * InteractionRuntime - Main class for managing interaction runtime.
 */
class InteractionRuntime(
    private val context: Context,
    private val stateManager: StateManager,
    private val tools: InteractionRuntimeTools
) {

    /**
     * Process an interaction request.
     */
    suspend fun processRequest(
        session: InteractionSession,
        request: InteractionRequest
    ): InteractionRuntimeResult = withContext(Dispatchers.IO) {
        when (request.intent) {
            InteractionIntentType.LIST_BOOKS -> handleListBooks(session)
            InteractionIntentType.CREATE_BOOK -> handleCreateBook(session, request)
            InteractionIntentType.SELECT_BOOK -> handleSelectBook(session, request)
            InteractionIntentType.WRITE_NEXT -> handleWriteNext(session, request)
            InteractionIntentType.REVISE_CHAPTER -> handleReviseChapter(session, request)
            InteractionIntentType.REWRITE_CHAPTER -> handleRewriteChapter(session, request)
            InteractionIntentType.PATCH_CHAPTER_TEXT -> handlePatchChapterText(session, request)
            InteractionIntentType.REPLACE_CHAPTER_TEXT -> handleReplaceChapterText(session, request)
            InteractionIntentType.RENAME_ENTITY -> handleRenameEntity(session, request)
            InteractionIntentType.UPDATE_FOCUS -> handleUpdateFocus(session, request)
            InteractionIntentType.UPDATE_AUTHOR_INTENT -> handleUpdateAuthorIntent(session, request)
            InteractionIntentType.EXPORT_BOOK -> handleExportBook(session, request)
            InteractionIntentType.CHAT -> handleChat(session, request)
            else -> InteractionRuntimeResult(
                session = session,
                responseText = "Intent '${request.intent}' not implemented"
            )
        }
    }

    private suspend fun handleListBooks(session: InteractionSession): InteractionRuntimeResult {
        val books = tools.listBooks()
        val responseText = if (books.isEmpty()) {
            "No books found"
        } else {
            "Found ${books.size} books:\n${books.joinToString("\n") { "- $it" }}"
        }

        return InteractionRuntimeResult(
            session = session,
            responseText = responseText,
            details = mapOf("books" to books)
        )
    }

    private suspend fun handleCreateBook(session: InteractionSession, request: InteractionRequest): InteractionRuntimeResult {
        val input = CreateBookInput(
            title = request.title ?: "Untitled",
            genre = request.genre,
            platform = request.platform,
            language = request.language,
            chapterWordCount = request.chapterWordCount,
            targetChapters = request.targetChapters,
            blurb = request.blurb,
            worldPremise = request.worldPremise,
            settingNotes = request.settingNotes,
            protagonist = request.protagonist,
            supportingCast = request.supportingCast,
            conflictCore = request.conflictCore,
            volumeOutline = request.volumeOutline,
            constraints = request.constraints,
            authorIntent = request.authorIntent,
            currentFocus = request.currentFocus
        )

        val result = tools.createBook(input)
        val bookId = extractBookId(result)

        val updatedSession = if (bookId != null) {
            bindActiveBook(session, bookId)
        } else {
            session
        }

        return InteractionRuntimeResult(
            session = updatedSession,
            responseText = if (bookId != null) "Book created: $bookId" else "Failed to create book",
            details = mapOf("bookId" to bookId)
        )
    }

    private suspend fun handleSelectBook(session: InteractionSession, request: InteractionRequest): InteractionRuntimeResult {
        val bookId = request.bookId ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Book ID is required"
        )

        val updatedSession = bindActiveBook(session, bookId)

        return InteractionRuntimeResult(
            session = updatedSession,
            responseText = "Selected book: $bookId"
        )
    }

    private suspend fun handleWriteNext(session: InteractionSession, request: InteractionRequest): InteractionRuntimeResult {
        val bookId = request.bookId ?: session.activeBookId ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Book ID is required"
        )

        val result = tools.writeNextChapter(bookId)
        val chapterNumber = extractChapterNumber(result)

        val updatedSession = if (chapterNumber != null) {
            session.copy(activeChapterNumber = chapterNumber)
        } else {
            session
        }

        return InteractionRuntimeResult(
            session = updatedSession,
            responseText = if (chapterNumber != null) "Wrote chapter $chapterNumber" else "Failed to write chapter",
            details = mapOf("chapterNumber" to chapterNumber)
        )
    }

    private suspend fun handleReviseChapter(session: InteractionSession, request: InteractionRequest): InteractionRuntimeResult {
        val bookId = request.bookId ?: session.activeBookId ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Book ID is required"
        )

        val chapterNumber = request.chapterNumber ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Chapter number is required"
        )

        val result = tools.reviseDraft(bookId, chapterNumber, "local-fix")

        return InteractionRuntimeResult(
            session = session,
            responseText = "Revised chapter $chapterNumber",
            details = mapOf("result" to result)
        )
    }

    private suspend fun handleRewriteChapter(session: InteractionSession, request: InteractionRequest): InteractionRuntimeResult {
        val bookId = request.bookId ?: session.activeBookId ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Book ID is required"
        )

        val chapterNumber = request.chapterNumber ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Chapter number is required"
        )

        val result = tools.reviseDraft(bookId, chapterNumber, "rewrite")

        return InteractionRuntimeResult(
            session = session,
            responseText = "Rewrote chapter $chapterNumber",
            details = mapOf("result" to result)
        )
    }

    private suspend fun handlePatchChapterText(session: InteractionSession, request: InteractionRequest): InteractionRuntimeResult {
        val bookId = request.bookId ?: session.activeBookId ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Book ID is required"
        )

        val chapterNumber = request.chapterNumber ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Chapter number is required"
        )

        val targetText = request.targetText ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Target text is required"
        )

        val replacementText = request.replacementText ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Replacement text is required"
        )

        val result = tools.patchChapterText(bookId, chapterNumber, targetText, replacementText)

        return InteractionRuntimeResult(
            session = session,
            responseText = "Patched chapter $chapterNumber",
            details = mapOf("result" to result)
        )
    }

    private suspend fun handleReplaceChapterText(session: InteractionSession, request: InteractionRequest): InteractionRuntimeResult {
        val bookId = request.bookId ?: session.activeBookId ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Book ID is required"
        )

        val chapterNumber = request.chapterNumber ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Chapter number is required"
        )

        val fullText = request.fullText ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Full text is required"
        )

        val result = tools.replaceChapterText(bookId, chapterNumber, fullText)

        return InteractionRuntimeResult(
            session = session,
            responseText = "Replaced chapter $chapterNumber",
            details = mapOf("result" to result)
        )
    }

    private suspend fun handleRenameEntity(session: InteractionSession, request: InteractionRequest): InteractionRuntimeResult {
        val bookId = request.bookId ?: session.activeBookId ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Book ID is required"
        )

        val oldValue = request.oldValue ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Old value is required"
        )

        val newValue = request.newValue ?: return InteractionRuntimeResult(
            session = session,
            responseText = "New value is required"
        )

        val result = tools.renameEntity(bookId, oldValue, newValue)

        return InteractionRuntimeResult(
            session = session,
            responseText = "Renamed '$oldValue' to '$newValue'",
            details = mapOf("result" to result)
        )
    }

    private suspend fun handleUpdateFocus(session: InteractionSession, request: InteractionRequest): InteractionRuntimeResult {
        val bookId = request.bookId ?: session.activeBookId ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Book ID is required"
        )

        val instruction = request.instruction ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Instruction is required"
        )

        val result = tools.updateCurrentFocus(bookId, instruction)

        return InteractionRuntimeResult(
            session = session,
            responseText = "Updated focus",
            details = mapOf("result" to result)
        )
    }

    private suspend fun handleUpdateAuthorIntent(session: InteractionSession, request: InteractionRequest): InteractionRuntimeResult {
        val bookId = request.bookId ?: session.activeBookId ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Book ID is required"
        )

        val instruction = request.instruction ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Instruction is required"
        )

        val result = tools.updateAuthorIntent(bookId, instruction)

        return InteractionRuntimeResult(
            session = session,
            responseText = "Updated author intent",
            details = mapOf("result" to result)
        )
    }

    private suspend fun handleExportBook(session: InteractionSession, request: InteractionRequest): InteractionRuntimeResult {
        val bookId = request.bookId ?: session.activeBookId ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Book ID is required"
        )

        val options = ExportOptions(
            format = request.format,
            approvedOnly = request.approvedOnly,
            outputPath = request.outputPath
        )

        val result = tools.exportBook(bookId, options)

        return InteractionRuntimeResult(
            session = session,
            responseText = "Exported book $bookId",
            details = mapOf("result" to result)
        )
    }

    private suspend fun handleChat(session: InteractionSession, request: InteractionRequest): InteractionRuntimeResult {
        val instruction = request.instruction ?: return InteractionRuntimeResult(
            session = session,
            responseText = "Message is required"
        )

        val options = ChatOptions(
            bookId = request.bookId ?: session.activeBookId,
            automationMode = session.automationMode
        )

        val result = tools.chat(instruction, options)

        return InteractionRuntimeResult(
            session = session,
            responseText = result?.toString() ?: "No response",
            details = mapOf("result" to result)
        )
    }

    private fun extractBookId(result: Any?): String? {
        if (result == null) return null
        if (result is String) return result
        if (result is Map<*, *>) return result["bookId"] as? String
        return null
    }

    private fun extractChapterNumber(result: Any?): Int? {
        if (result == null) return null
        if (result is Int) return result
        if (result is Map<*, *>) return (result["chapterNumber"] as? Number)?.toInt()
        return null
    }
}
