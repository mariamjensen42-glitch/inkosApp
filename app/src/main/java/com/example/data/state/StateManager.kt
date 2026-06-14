package com.example.data.state

import android.content.Context
import com.example.data.models.BookConfig
import com.example.data.models.ChapterMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * StateManager - Manages book state, control documents, and chapter indices.
 *
 * This is the Kotlin Android equivalent of the TypeScript StateManager class.
 * It handles:
 * - Control documents (author_intent.md, current_focus.md)
 * - Book configuration (book.json)
 * - Chapter index management
 * - State snapshots
 * - File locking for concurrent access
 */
class StateManager(private val context: Context, private val projectRoot: File) {

    private val activeWrites = ConcurrentHashMap.newKeySet<String>()
    private val lockMutex = Mutex()

    companion object {
        private const val BOOKS_DIR = "books"
        private const val STORY_DIR = "story"
        private const val RUNTIME_DIR = "runtime"
        private const val OUTLINE_DIR = "outline"
        private const val SNAPSHOTS_DIR = "snapshots"
        private const val CHAPTERS_DIR = "chapters"
        private const val STATE_DIR = "state"

        private fun defaultAuthorIntent(language: String): String {
            return if (language == "zh") {
                "# 作者意图\n\n（在这里描述这本书的长期创作方向。）\n"
            } else {
                "# Author Intent\n\n(Describe the long-horizon vision for this book here.)\n"
            }
        }

        private fun defaultCurrentFocus(language: String): String {
            return if (language == "zh") {
                "# 当前聚焦\n\n## 当前重点\n\n（描述接下来 1-3 章最需要优先推进的内容。）\n"
            } else {
                "# Current Focus\n\n## Active Focus\n\n(Describe what the next 1-3 chapters should prioritize.)\n"
            }
        }
    }

    val booksDir: File
        get() = File(projectRoot, BOOKS_DIR)

    fun bookDir(bookId: String): File {
        return File(booksDir, bookId)
    }

    fun stateDir(bookId: String): File {
        return File(bookDir(bookId), "$STORY_DIR/$STATE_DIR")
    }

    suspend fun ensureControlDocuments(bookId: String, authorIntent: String? = null) {
        val language = resolveControlDocumentLanguage(bookId)
        ensureControlDocumentsAt(bookDir(bookId), language, authorIntent)
    }

    suspend fun ensureControlDocumentsAt(
        bookDir: File,
        language: String,
        authorIntent: String? = null
    ) = withContext(Dispatchers.IO) {
        val storyDir = File(bookDir, STORY_DIR)
        val runtimeDir = File(storyDir, RUNTIME_DIR)
        val outlineDir = File(storyDir, OUTLINE_DIR)
        val rolesMajorDir = File(storyDir, "roles/主要角色")
        val rolesMinorDir = File(storyDir, "roles/次要角色")

        storyDir.mkdirs()
        runtimeDir.mkdirs()
        outlineDir.mkdirs()
        rolesMajorDir.mkdirs()
        rolesMinorDir.mkdirs()

        writeIfMissing(
            File(storyDir, "author_intent.md"),
            if (authorIntent?.trim()?.isNotEmpty() == true) {
                authorIntent.trimEnd() + "\n"
            } else {
                defaultAuthorIntent(language)
            }
        )

        writeIfMissing(
            File(storyDir, "current_focus.md"),
            defaultCurrentFocus(language)
        )

        // Ensure style_guide includes writing methodology even without reference text
        val styleGuidePath = File(storyDir, "style_guide.md")
        try {
            val existing = styleGuidePath.readText()
            if (!existing.contains("写作方法论") && !existing.contains("Writing Methodology")) {
                // Note: WritingMethodology utility would need to be implemented separately
                styleGuidePath.writeText("$existing\n\n<!-- Writing methodology section placeholder -->\n")
            }
        } catch (e: Exception) {
            // File doesn't exist, create it
            styleGuidePath.writeText("<!-- Writing methodology section placeholder -->\n")
        }
    }

    suspend fun loadControlDocuments(bookId: String): Triple<String, String, File> {
        ensureControlDocuments(bookId)

        val storyDir = File(bookDir(bookId), STORY_DIR)
        val runtimeDir = File(storyDir, RUNTIME_DIR)
        val authorIntent = File(storyDir, "author_intent.md").readText()
        val currentFocus = File(storyDir, "current_focus.md").readText()

        return Triple(authorIntent, currentFocus, runtimeDir)
    }

    private suspend fun resolveControlDocumentLanguage(bookId: String): String = withContext(Dispatchers.IO) {
        try {
            val raw = File(bookDir(bookId), "book.json").readText()
            val parsed = JSONObject(raw)
            if (parsed.optString("language") == "zh") "zh" else "en"
        } catch (e: Exception) {
            "en"
        }
    }

    suspend fun acquireBookLock(bookId: String): suspend () -> Unit {
        withContext(Dispatchers.IO) {
            bookDir(bookId).mkdirs()
        }

        val lockPath = File(bookDir(bookId), ".write.lock")

        return lockMutex.withLock {
            withContext(Dispatchers.IO) {
                if (lockPath.exists()) {
                    val lockData = try {
                        lockPath.readText()
                    } catch (e: Exception) {
                        "pid:unknown ts:unknown"
                    }

                    val isStale = try {
                        // On Android, we can't easily check if a process is alive
                        // We'll consider locks older than 5 minutes as stale
                        val lockTimestamp = lockData.substringAfter("ts:").toLongOrNull() ?: 0
                        System.currentTimeMillis() - lockTimestamp > 5 * 60 * 1000
                    } catch (e: Exception) {
                        true
                    }

                    if (isStale) {
                        lockPath.delete()
                    } else {
                        throw IllegalStateException(
                            "Book \"$bookId\" is locked by another process ($lockData). " +
                                "If this is stale, delete ${lockPath.absolutePath}"
                        )
                    }
                }

                lockPath.writeText("pid:${android.os.Process.myPid()} ts:${System.currentTimeMillis()}")
                activeWrites.add(bookId)
            }

            // Return unlock function
            {
                activeWrites.remove(bookId)
                withContext(Dispatchers.IO) {
                    try {
                        lockPath.delete()
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }
        }
    }

    suspend fun loadProjectConfig(): JSONObject = withContext(Dispatchers.IO) {
        val configPath = File(projectRoot, "inkos.json")
        val raw = configPath.readText()
        JSONObject(raw)
    }

    suspend fun saveProjectConfig(config: JSONObject) = withContext(Dispatchers.IO) {
        val configPath = File(projectRoot, "inkos.json")
        configPath.writeText(config.toString(2))
    }

    suspend fun loadBookConfig(bookId: String): BookConfig = withContext(Dispatchers.IO) {
        val configPath = File(bookDir(bookId), "book.json")
        val raw = configPath.readText()
        if (raw.isBlank()) {
            throw IllegalStateException("book.json is empty for book \"$bookId\"")
        }
        BookConfig.fromJson(JSONObject(raw))
    }

    suspend fun saveBookConfig(bookId: String, config: BookConfig) = withContext(Dispatchers.IO) {
        saveBookConfigAt(bookDir(bookId), config)
    }

    suspend fun saveBookConfigAt(bookDir: File, config: BookConfig) = withContext(Dispatchers.IO) {
        bookDir.mkdirs()
        File(bookDir, "book.json").writeText(config.toJson().toString(2))
    }

    suspend fun ensureRuntimeState(bookId: String, fallbackChapter: Int = 0) = withContext(Dispatchers.IO) {
        // Note: StateBootstrap would need to be implemented separately
        // bootstrapStructuredStateFromMarkdown(bookDir(bookId), fallbackChapter)
    }

    suspend fun listBooks(): List<String> = withContext(Dispatchers.IO) {
        try {
            val entries = booksDir.listFiles()?.filter { it.isDirectory } ?: emptyList()
            entries.filter { File(it, "book.json").exists() }
                .map { it.name }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getNextChapterNumber(bookId: String): Int = withContext(Dispatchers.IO) {
        // Note: resolveDurableStoryProgress would need to be implemented separately
        // For now, we'll use a simple implementation
        val chaptersDir = File(bookDir(bookId), CHAPTERS_DIR)
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

    suspend fun getPersistedChapterCount(bookId: String): Int = withContext(Dispatchers.IO) {
        val chaptersDir = File(bookDir(bookId), CHAPTERS_DIR)
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

        chapterNumbers.size
    }

    suspend fun loadChapterIndex(bookId: String): List<ChapterMeta> = withContext(Dispatchers.IO) {
        val indexPath = File(bookDir(bookId), "$CHAPTERS_DIR/index.json")
        try {
            val raw = indexPath.readText()
            val jsonArray = JSONArray(raw)
            (0 until jsonArray.length()).map { i ->
                ChapterMeta.fromJson(jsonArray.getJSONObject(i))
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun saveChapterIndex(bookId: String, index: List<ChapterMeta>) = withContext(Dispatchers.IO) {
        saveChapterIndexAt(bookDir(bookId), index)
    }

    suspend fun saveChapterIndexAt(bookDir: File, index: List<ChapterMeta>) = withContext(Dispatchers.IO) {
        val chaptersDir = File(bookDir, CHAPTERS_DIR)
        chaptersDir.mkdirs()
        val jsonArray = JSONArray()
        index.forEach { chapter ->
            jsonArray.put(chapter.toJson())
        }
        File(chaptersDir, "index.json").writeText(jsonArray.toString(2))
    }

    suspend fun snapshotState(bookId: String, chapterNumber: Int) = withContext(Dispatchers.IO) {
        snapshotStateAt(bookDir(bookId), chapterNumber)
    }

    suspend fun snapshotStateAt(bookDir: File, chapterNumber: Int) = withContext(Dispatchers.IO) {
        val storyDir = File(bookDir, STORY_DIR)
        val snapshotDir = File(storyDir, "$SNAPSHOTS_DIR/$chapterNumber")
        snapshotDir.mkdirs()

        val files = listOf(
            "current_state.md", "particle_ledger.md", "pending_hooks.md",
            "chapter_summaries.md", "subplot_board.md", "emotional_arcs.md", "character_matrix.md"
        )

        files.forEach { fileName ->
            try {
                val sourceFile = File(storyDir, fileName)
                if (sourceFile.exists()) {
                    val content = sourceFile.readText()
                    File(snapshotDir, fileName).writeText(content)
                }
            } catch (e: Exception) {
                // File doesn't exist yet
            }
        }

        val stateDir = File(bookDir, "$STORY_DIR/$STATE_DIR")
        val snapshotStateDir = File(snapshotDir, STATE_DIR)

        try {
            val stateFiles = stateDir.listFiles()
            if (stateFiles != null && stateFiles.isNotEmpty()) {
                snapshotStateDir.mkdirs()
                stateFiles.forEach { file ->
                    val content = file.readText()
                    File(snapshotStateDir, file.name).writeText(content)
                }
            }
        } catch (e: Exception) {
            // state directory missing — skip
        }
    }

    suspend fun isCompleteBookDirectory(bookDir: File): Boolean = withContext(Dispatchers.IO) {
        val requiredSingle = listOf(
            File(bookDir, "book.json"),
            File(bookDir, "$STORY_DIR/book_rules.md"),
            File(bookDir, "$STORY_DIR/current_state.md"),
            File(bookDir, "$STORY_DIR/pending_hooks.md"),
            File(bookDir, "$CHAPTERS_DIR/index.json")
        )

        val eitherOr = listOf(
            // story_frame (new) OR story_bible (legacy)
            listOf(
                File(bookDir, "$STORY_DIR/outline/story_frame.md"),
                File(bookDir, "$STORY_DIR/story_bible.md")
            ),
            // volume_map (new) OR volume_outline (legacy)
            listOf(
                File(bookDir, "$STORY_DIR/outline/volume_map.md"),
                File(bookDir, "$STORY_DIR/volume_outline.md")
            )
        )

        for (requiredPath in requiredSingle) {
            if (!requiredPath.exists()) {
                return@withContext false
            }
        }

        for (alternatives in eitherOr) {
            var found = false
            for (candidate in alternatives) {
                if (candidate.exists()) {
                    found = true
                    break
                }
            }
            if (!found) return@withContext false
        }

        true
    }

    suspend fun restoreState(bookId: String, chapterNumber: Int): Boolean = withContext(Dispatchers.IO) {
        val storyDir = File(bookDir(bookId), STORY_DIR)
        val snapshotDir = File(storyDir, "$SNAPSHOTS_DIR/$chapterNumber")

        val files = listOf(
            "current_state.md", "particle_ledger.md", "pending_hooks.md",
            "chapter_summaries.md", "subplot_board.md", "emotional_arcs.md", "character_matrix.md"
        )

        try {
            val requiredFiles = listOf("current_state.md", "pending_hooks.md")
            val optionalFiles = files.filter { it !in requiredFiles }

            requiredFiles.forEach { fileName ->
                val content = File(snapshotDir, fileName).readText()
                File(storyDir, fileName).writeText(content)
            }

            optionalFiles.forEach { fileName ->
                val targetPath = File(storyDir, fileName)
                try {
                    val content = File(snapshotDir, fileName).readText()
                    targetPath.writeText(content)
                } catch (e: Exception) {
                    targetPath.delete()
                }
            }

            val stateDir = stateDir(bookId)
            var restoredStructuredState = false
            try {
                val snapshotStateDir = File(snapshotDir, STATE_DIR)
                val stateFiles = snapshotStateDir.listFiles()
                if (stateFiles != null && stateFiles.isNotEmpty()) {
                    restoredStructuredState = true
                    stateDir.mkdirs()
                    stateFiles.forEach { file ->
                        val content = file.readText()
                        File(stateDir, file.name).writeText(content)
                    }
                }
            } catch (e: Exception) {
                // snapshot structured state missing — skip
            }

            if (!restoredStructuredState) {
                stateDir.deleteRecursively()
            }

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun rollbackToChapter(bookId: String, targetChapter: Int): List<Int> = withContext(Dispatchers.IO) {
        val restored = restoreState(bookId, targetChapter)
        if (!restored) {
            throw IllegalStateException("Cannot restore snapshot for chapter $targetChapter in \"$bookId\"")
        }

        val bookDir = bookDir(bookId)
        val chaptersDir = File(bookDir, CHAPTERS_DIR)
        val index = loadChapterIndex(bookId)

        val kept = mutableListOf<ChapterMeta>()
        val discarded = mutableListOf<Int>()

        index.forEach { entry ->
            if (entry.number <= targetChapter) {
                kept.add(entry)
            } else {
                discarded.add(entry.number)
            }
        }

        // Delete chapter markdown files for discarded chapters
        try {
            chaptersDir.listFiles()?.forEach { file ->
                val match = Regex("^(\\d+)_.*\\.md$").find(file.name)
                if (match != null) {
                    val num = match.groupValues[1].toInt()
                    if (num > targetChapter) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // chapters directory missing
        }

        // Delete snapshots for discarded chapters
        val snapshotsDir = File(bookDir, "$STORY_DIR/$SNAPSHOTS_DIR")
        try {
            snapshotsDir.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    val num = file.name.toIntOrNull()
                    if (num != null && num > targetChapter) {
                        file.deleteRecursively()
                    }
                }
            }
        } catch (e: Exception) {
            // snapshots directory missing
        }

        // Delete runtime artifacts for discarded chapters
        val runtimeDir = File(bookDir, "$STORY_DIR/$RUNTIME_DIR")
        try {
            runtimeDir.listFiles()?.forEach { file ->
                val match = Regex("^chapter-(\\d+)\\.").find(file.name)
                if (match != null) {
                    val num = match.groupValues[1].toInt()
                    if (num > targetChapter) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // runtime directory missing
        }

        // Also check story/drafts/ for discarded chapter files
        val draftsDir = File(bookDir, "$STORY_DIR/drafts")
        try {
            draftsDir.listFiles()?.forEach { file ->
                val match = Regex("^(\\d+)_.*\\.md$").find(file.name)
                if (match != null) {
                    val num = match.groupValues[1].toInt()
                    if (num > targetChapter) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            // drafts directory missing
        }

        // Drop any persisted sqlite acceleration index
        val memoryDb = File(bookDir, "$STORY_DIR/memory.db")
        val memoryDbShm = File(bookDir, "$STORY_DIR/memory.db-shm")
        val memoryDbWal = File(bookDir, "$STORY_DIR/memory.db-wal")
        memoryDb.delete()
        memoryDbShm.delete()
        memoryDbWal.delete()

        saveChapterIndex(bookId, kept)
        discarded
    }

    private fun writeIfMissing(path: File, content: String) {
        if (!path.exists()) {
            path.parentFile?.mkdirs()
            path.writeText(content)
        }
    }
}
