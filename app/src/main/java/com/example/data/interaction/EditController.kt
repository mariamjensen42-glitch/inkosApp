package com.example.data.interaction

import android.content.Context
import com.example.data.models.ChapterMeta
import com.example.data.state.StateManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * EditController - Edit controller for managing chapter and truth file edits.
 *
 * This is the Kotlin Android equivalent of the TypeScript edit-controller.ts module.
 * It handles:
 * - Planning edit transactions
 * - Executing edit operations
 * - Managing entity renames, chapter rewrites, and truth file edits
 */

sealed class EditRequest {
    abstract val bookId: String

    data class EntityRename(
        override val bookId: String,
        val entityType: String, // "protagonist", "character", "location", "organization"
        val oldValue: String,
        val newValue: String
    ) : EditRequest()

    data class ChapterRewrite(
        override val bookId: String,
        val chapterNumber: Int,
        val instruction: String
    ) : EditRequest()

    data class ChapterReplace(
        override val bookId: String,
        val chapterNumber: Int,
        val fullText: String
    ) : EditRequest()

    data class ChapterLocalEdit(
        override val bookId: String,
        val chapterNumber: Int,
        val instruction: String,
        val targetText: String? = null,
        val replacementText: String? = null
    ) : EditRequest()

    data class TruthFileEdit(
        override val bookId: String,
        val fileName: String,
        val instruction: String
    ) : EditRequest()

    data class FocusEdit(
        override val bookId: String,
        val instruction: String
    ) : EditRequest()
}

data class PlannedEditTransaction(
    val transactionType: String,
    val bookId: String,
    val chapterNumber: Int? = null,
    val truthAuthority: String? = null,
    val normalizedFileName: String? = null,
    val affectedScope: String, // "chapter", "downstream", "future", "book"
    val requiresTruthRebuild: Boolean
)

data class ExecutedEditTransaction(
    val transactionType: String,
    val bookId: String,
    val chapterNumber: Int? = null,
    val touchedFiles: List<String>,
    val reviewRequired: Boolean,
    val summary: String
)

/**
 * EditController - Main class for managing edits.
 */
class EditController(
    private val context: Context,
    private val stateManager: StateManager
) {

    companion object {
        /**
         * Plan an edit transaction.
         */
        fun planEditTransaction(request: EditRequest): PlannedEditTransaction {
            return when (request) {
                is EditRequest.EntityRename -> PlannedEditTransaction(
                    transactionType = "entity-rename",
                    bookId = request.bookId,
                    affectedScope = "book",
                    requiresTruthRebuild = true
                )
                is EditRequest.ChapterRewrite -> PlannedEditTransaction(
                    transactionType = "chapter-rewrite",
                    bookId = request.bookId,
                    chapterNumber = request.chapterNumber,
                    affectedScope = "downstream",
                    requiresTruthRebuild = true
                )
                is EditRequest.ChapterReplace -> PlannedEditTransaction(
                    transactionType = "chapter-replace",
                    bookId = request.bookId,
                    chapterNumber = request.chapterNumber,
                    affectedScope = "chapter",
                    requiresTruthRebuild = false
                )
                is EditRequest.ChapterLocalEdit -> PlannedEditTransaction(
                    transactionType = "chapter-local-edit",
                    bookId = request.bookId,
                    chapterNumber = request.chapterNumber,
                    affectedScope = "chapter",
                    requiresTruthRebuild = false
                )
                is EditRequest.TruthFileEdit -> PlannedEditTransaction(
                    transactionType = "truth-file-edit",
                    bookId = request.bookId,
                    normalizedFileName = request.fileName,
                    affectedScope = "book",
                    requiresTruthRebuild = true
                )
                is EditRequest.FocusEdit -> PlannedEditTransaction(
                    transactionType = "focus-edit",
                    bookId = request.bookId,
                    affectedScope = "book",
                    requiresTruthRebuild = true
                )
            }
        }
    }

    /**
     * Execute an edit request.
     */
    suspend fun executeEdit(request: EditRequest): ExecutedEditTransaction = withContext(Dispatchers.IO) {
        val plan = planEditTransaction(request)

        when (request) {
            is EditRequest.EntityRename -> executeEntityRename(request)
            is EditRequest.ChapterRewrite -> executeChapterRewrite(request)
            is EditRequest.ChapterReplace -> executeChapterReplace(request)
            is EditRequest.ChapterLocalEdit -> executeChapterLocalEdit(request)
            is EditRequest.TruthFileEdit -> executeTruthFileEdit(request)
            is EditRequest.FocusEdit -> executeFocusEdit(request)
        }
    }

    private suspend fun executeEntityRename(request: EditRequest.EntityRename): ExecutedEditTransaction {
        val bookDir = stateManager.bookDir(request.bookId)
        val touchedFiles = mutableListOf<String>()

        // Find and update all files that reference the entity
        val storyDir = File(bookDir, "story")
        if (storyDir.exists()) {
            storyDir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "md") {
                    val content = file.readText()
                    if (content.contains(request.oldValue)) {
                        val updatedContent = content.replace(request.oldValue, request.newValue)
                        file.writeText(updatedContent)
                        touchedFiles.add(file.absolutePath)
                    }
                }
            }
        }

        // Also update chapter files
        val chaptersDir = File(bookDir, "chapters")
        if (chaptersDir.exists()) {
            chaptersDir.walkTopDown().forEach { file ->
                if (file.isFile && file.extension == "md") {
                    val content = file.readText()
                    if (content.contains(request.oldValue)) {
                        val updatedContent = content.replace(request.oldValue, request.newValue)
                        file.writeText(updatedContent)
                        touchedFiles.add(file.absolutePath)
                    }
                }
            }
        }

        return ExecutedEditTransaction(
            transactionType = "entity-rename",
            bookId = request.bookId,
            touchedFiles = touchedFiles,
            reviewRequired = true,
            summary = "Renamed ${request.entityType} from '${request.oldValue}' to '${request.newValue}' in ${touchedFiles.size} files"
        )
    }

    private suspend fun executeChapterRewrite(request: EditRequest.ChapterRewrite): ExecutedEditTransaction {
        // In a full implementation, this would use the WriterAgent to rewrite the chapter
        val bookDir = stateManager.bookDir(request.bookId)
        val chaptersDir = File(bookDir, "chapters")

        // Find the chapter file
        val chapterFile = chaptersDir.listFiles()?.find { file ->
            val match = Regex("^(\\d+)_.*\\.md$").find(file.name)
            match != null && match.groupValues[1].toInt() == request.chapterNumber
        }

        if (chapterFile == null) {
            return ExecutedEditTransaction(
                transactionType = "chapter-rewrite",
                bookId = request.bookId,
                chapterNumber = request.chapterNumber,
                touchedFiles = emptyList(),
                reviewRequired = false,
                summary = "Chapter ${request.chapterNumber} not found"
            )
        }

        // In a real implementation, this would use the WriterAgent
        // For now, just mark the chapter as needing rewrite
        val summary = "Chapter ${request.chapterNumber} marked for rewrite with instruction: ${request.instruction}"

        return ExecutedEditTransaction(
            transactionType = "chapter-rewrite",
            bookId = request.bookId,
            chapterNumber = request.chapterNumber,
            touchedFiles = listOf(chapterFile.absolutePath),
            reviewRequired = true,
            summary = summary
        )
    }

    private suspend fun executeChapterReplace(request: EditRequest.ChapterReplace): ExecutedEditTransaction {
        val bookDir = stateManager.bookDir(request.bookId)
        val chaptersDir = File(bookDir, "chapters")

        // Find the chapter file
        val chapterFile = chaptersDir.listFiles()?.find { file ->
            val match = Regex("^(\\d+)_.*\\.md$").find(file.name)
            match != null && match.groupValues[1].toInt() == request.chapterNumber
        }

        if (chapterFile == null) {
            // Create new chapter file
            val newFile = File(chaptersDir, "${request.chapterNumber}_chapter.md")
            newFile.writeText(request.fullText)
            return ExecutedEditTransaction(
                transactionType = "chapter-replace",
                bookId = request.bookId,
                chapterNumber = request.chapterNumber,
                touchedFiles = listOf(newFile.absolutePath),
                reviewRequired = false,
                summary = "Created new chapter ${request.chapterNumber}"
            )
        }

        // Replace chapter content
        chapterFile.writeText(request.fullText)

        return ExecutedEditTransaction(
            transactionType = "chapter-replace",
            bookId = request.bookId,
            chapterNumber = request.chapterNumber,
            touchedFiles = listOf(chapterFile.absolutePath),
            reviewRequired = false,
            summary = "Replaced chapter ${request.chapterNumber}"
        )
    }

    private suspend fun executeChapterLocalEdit(request: EditRequest.ChapterLocalEdit): ExecutedEditTransaction {
        val bookDir = stateManager.bookDir(request.bookId)
        val chaptersDir = File(bookDir, "chapters")

        // Find the chapter file
        val chapterFile = chaptersDir.listFiles()?.find { file ->
            val match = Regex("^(\\d+)_.*\\.md$").find(file.name)
            match != null && match.groupValues[1].toInt() == request.chapterNumber
        }

        if (chapterFile == null) {
            return ExecutedEditTransaction(
                transactionType = "chapter-local-edit",
                bookId = request.bookId,
                chapterNumber = request.chapterNumber,
                touchedFiles = emptyList(),
                reviewRequired = false,
                summary = "Chapter ${request.chapterNumber} not found"
            )
        }

        val content = chapterFile.readText()
        val updatedContent = if (request.targetText != null && request.replacementText != null) {
            content.replace(request.targetText, request.replacementText)
        } else {
            // In a real implementation, this would use an LLM to apply the edit
            content
        }

        chapterFile.writeText(updatedContent)

        return ExecutedEditTransaction(
            transactionType = "chapter-local-edit",
            bookId = request.bookId,
            chapterNumber = request.chapterNumber,
            touchedFiles = listOf(chapterFile.absolutePath),
            reviewRequired = false,
            summary = "Applied local edit to chapter ${request.chapterNumber}"
        )
    }

    private suspend fun executeTruthFileEdit(request: EditRequest.TruthFileEdit): ExecutedEditTransaction {
        val bookDir = stateManager.bookDir(request.bookId)
        val storyDir = File(bookDir, "story")

        // Find the truth file
        val truthFile = storyDir.listFiles()?.find { file ->
            file.name == request.fileName || file.nameWithoutExtension == request.fileName
        }

        if (truthFile == null) {
            return ExecutedEditTransaction(
                transactionType = "truth-file-edit",
                bookId = request.bookId,
                touchedFiles = emptyList(),
                reviewRequired = false,
                summary = "Truth file '${request.fileName}' not found"
            )
        }

        // In a real implementation, this would use an LLM to apply the edit
        val summary = "Truth file '${request.fileName}' marked for edit with instruction: ${request.instruction}"

        return ExecutedEditTransaction(
            transactionType = "truth-file-edit",
            bookId = request.bookId,
            touchedFiles = listOf(truthFile.absolutePath),
            reviewRequired = true,
            summary = summary
        )
    }

    private suspend fun executeFocusEdit(request: EditRequest.FocusEdit): ExecutedEditTransaction {
        val bookDir = stateManager.bookDir(request.bookId)
        val focusFile = File(bookDir, "story/current_focus.md")

        if (!focusFile.exists()) {
            return ExecutedEditTransaction(
                transactionType = "focus-edit",
                bookId = request.bookId,
                touchedFiles = emptyList(),
                reviewRequired = false,
                summary = "Focus file not found"
            )
        }

        // In a real implementation, this would use an LLM to apply the edit
        val summary = "Focus file marked for edit with instruction: ${request.instruction}"

        return ExecutedEditTransaction(
            transactionType = "focus-edit",
            bookId = request.bookId,
            touchedFiles = listOf(focusFile.absolutePath),
            reviewRequired = true,
            summary = summary
        )
    }
}
