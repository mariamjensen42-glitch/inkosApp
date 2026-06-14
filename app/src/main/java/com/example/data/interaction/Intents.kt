package com.example.data.interaction

import org.json.JSONObject

/**
 * Intents - Intent definitions for the interaction system.
 *
 * This is the Kotlin Android equivalent of the TypeScript intents.ts module.
 * It contains:
 * - InteractionIntentType - Intent type enum
 * - InteractionRequest - Interaction request
 */

enum class InteractionIntentType {
    DEVELOP_BOOK,
    SHOW_BOOK_DRAFT,
    CREATE_BOOK,
    DISCARD_BOOK_DRAFT,
    LIST_BOOKS,
    SELECT_BOOK,
    CONTINUE_BOOK,
    WRITE_NEXT,
    PAUSE_BOOK,
    RESUME_BOOK,
    REVISE_CHAPTER,
    REWRITE_CHAPTER,
    PATCH_CHAPTER_TEXT,
    REPLACE_CHAPTER_TEXT,
    EDIT_TRUTH,
    RENAME_ENTITY,
    UPDATE_FOCUS,
    UPDATE_AUTHOR_INTENT,
    CHAT,
    EXPLAIN_STATUS,
    EXPLAIN_FAILURE,
    EXPORT_BOOK;

    companion object {
        fun fromString(value: String): InteractionIntentType {
            return when (value.lowercase()) {
                "develop_book" -> DEVELOP_BOOK
                "show_book_draft" -> SHOW_BOOK_DRAFT
                "create_book" -> CREATE_BOOK
                "discard_book_draft" -> DISCARD_BOOK_DRAFT
                "list_books" -> LIST_BOOKS
                "select_book" -> SELECT_BOOK
                "continue_book" -> CONTINUE_BOOK
                "write_next" -> WRITE_NEXT
                "pause_book" -> PAUSE_BOOK
                "resume_book" -> RESUME_BOOK
                "revise_chapter" -> REVISE_CHAPTER
                "rewrite_chapter" -> REWRITE_CHAPTER
                "patch_chapter_text" -> PATCH_CHAPTER_TEXT
                "replace_chapter_text" -> REPLACE_CHAPTER_TEXT
                "edit_truth" -> EDIT_TRUTH
                "rename_entity" -> RENAME_ENTITY
                "update_focus" -> UPDATE_FOCUS
                "update_author_intent" -> UPDATE_AUTHOR_INTENT
                "chat" -> CHAT
                "explain_status" -> EXPLAIN_STATUS
                "explain_failure" -> EXPLAIN_FAILURE
                "export_book" -> EXPORT_BOOK
                else -> CHAT
            }
        }
    }
}

data class InteractionRequest(
    val intent: InteractionIntentType,
    val bookId: String? = null,
    val chapterNumber: Int? = null,
    val title: String? = null,
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
    val currentFocus: String? = null,
    val fileName: String? = null,
    val format: String? = null, // "txt", "md", "epub"
    val approvedOnly: Boolean? = null,
    val outputPath: String? = null,
    val oldValue: String? = null,
    val newValue: String? = null,
    val targetText: String? = null,
    val replacementText: String? = null,
    val fullText: String? = null,
    val instruction: String? = null,
    val mode: AutomationMode? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("intent", intent.name.lowercase())
            bookId?.let { put("bookId", it) }
            chapterNumber?.let { put("chapterNumber", it) }
            title?.let { put("title", it) }
            genre?.let { put("genre", it) }
            platform?.let { put("platform", it) }
            language?.let { put("language", it) }
            chapterWordCount?.let { put("chapterWordCount", it) }
            targetChapters?.let { put("targetChapters", it) }
            blurb?.let { put("blurb", it) }
            worldPremise?.let { put("worldPremise", it) }
            settingNotes?.let { put("settingNotes", it) }
            protagonist?.let { put("protagonist", it) }
            supportingCast?.let { put("supportingCast", it) }
            conflictCore?.let { put("conflictCore", it) }
            volumeOutline?.let { put("volumeOutline", it) }
            constraints?.let { put("constraints", it) }
            authorIntent?.let { put("authorIntent", it) }
            currentFocus?.let { put("currentFocus", it) }
            fileName?.let { put("fileName", it) }
            format?.let { put("format", it) }
            approvedOnly?.let { put("approvedOnly", it) }
            outputPath?.let { put("outputPath", it) }
            oldValue?.let { put("oldValue", it) }
            newValue?.let { put("newValue", it) }
            targetText?.let { put("targetText", it) }
            replacementText?.let { put("replacementText", it) }
            fullText?.let { put("fullText", it) }
            instruction?.let { put("instruction", it) }
            mode?.let { put("mode", it.name.lowercase()) }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): InteractionRequest {
            return InteractionRequest(
                intent = InteractionIntentType.fromString(json.getString("intent")),
                bookId = json.optString("bookId", null),
                chapterNumber = if (json.has("chapterNumber") && !json.isNull("chapterNumber")) json.getInt("chapterNumber") else null,
                title = json.optString("title", null),
                genre = json.optString("genre", null),
                platform = json.optString("platform", null),
                language = json.optString("language", null),
                chapterWordCount = if (json.has("chapterWordCount") && !json.isNull("chapterWordCount")) json.getInt("chapterWordCount") else null,
                targetChapters = if (json.has("targetChapters") && !json.isNull("targetChapters")) json.getInt("targetChapters") else null,
                blurb = json.optString("blurb", null),
                worldPremise = json.optString("worldPremise", null),
                settingNotes = json.optString("settingNotes", null),
                protagonist = json.optString("protagonist", null),
                supportingCast = json.optString("supportingCast", null),
                conflictCore = json.optString("conflictCore", null),
                volumeOutline = json.optString("volumeOutline", null),
                constraints = json.optString("constraints", null),
                authorIntent = json.optString("authorIntent", null),
                currentFocus = json.optString("currentFocus", null),
                fileName = json.optString("fileName", null),
                format = json.optString("format", null),
                approvedOnly = if (json.has("approvedOnly") && !json.isNull("approvedOnly")) json.getBoolean("approvedOnly") else null,
                outputPath = json.optString("outputPath", null),
                oldValue = json.optString("oldValue", null),
                newValue = json.optString("newValue", null),
                targetText = json.optString("targetText", null),
                replacementText = json.optString("replacementText", null),
                fullText = json.optString("fullText", null),
                instruction = json.optString("instruction", null),
                mode = json.optString("mode", null)?.let { AutomationMode.fromString(it) }
            )
        }
    }
}
