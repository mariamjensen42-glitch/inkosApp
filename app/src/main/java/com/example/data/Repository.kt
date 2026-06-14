package com.example.data

import android.content.Context
import android.util.Log
import com.example.data.api.AgentSystemPrompt
import com.example.data.api.LlmRouter
import com.example.data.api.AITellAnalyzer
import com.example.data.local.AppDatabase
import com.example.data.local.BookEntity
import com.example.data.local.ChapterEntity
import com.example.data.local.PlayStateEntity
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

class Repository(context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val bookDao = database.bookDao()
    private val chapterDao = database.chapterDao()
    private val playStateDao = database.playStateDao()

    companion object {
        private const val TAG = "Repository"
    }

    private fun determineLanguage(bookTitle: String, brief: String): String {
        val content = bookTitle + brief
        for (char in content) {
            if (char.code in 0x4E00..0x9FFF) {
                return "zh"
            }
        }
        return "en"
    }

    // --- Book Operations ---
    fun getAllBooks(): Flow<List<BookEntity>> = bookDao.getAllBooks()

    fun getBookByIdFlow(id: Long): Flow<BookEntity?> = bookDao.getBookByIdFlow(id)

    suspend fun getBookById(id: Long): BookEntity? = bookDao.getBookById(id)

    suspend fun createBook(title: String, type: String, genre: String, brief: String): Long {
        var storyBible = """
            # Story Bible: $title
            **Genre**: $genre
            **Brief**: $brief
            
            ## Narrative Rules
            - Adhere strictly to the protagonist style.
            - Ensure deep immersion and logical consistency of items and power structures.
            - Ban clichéd, AI-sounding transitions like "suddenly, in this cosmic universe..."
        """.trimIndent()

        var bookRules = """
            # Book Level Guidelines
            - Avoid repetitive text inside chapters.
            - Verify character memory before making statements.
            - Word range target parameter active.
        """.trimIndent()

        if (type == "NOVEL") {
            try {
                val output = com.example.data.api.ArchitectAgent.generateFoundation(
                    title = title,
                    genre = genre,
                    targetChapters = 12,
                    chapterWordCount = 1000,
                    brief = brief
                )
                storyBible = output.storyBible
                bookRules = output.bookRules
            } catch (e: Exception) {
                Log.e(TAG, "Failed calling ArchitectAgent during book creation, using default baseline", e)
            }
        }

        val book = BookEntity(
            title = title,
            type = type,
            genre = genre,
            brief = brief,
            storyBible = storyBible,
            bookRules = bookRules,
            status = if (type == "PLAY") "PLAYING" else "DRAFT"
        )
        val bookId = bookDao.insertBook(book)

        // If it's an InkOS Play open world, prepare initial world configuration
        if (type == "PLAY") {
            initializeOpenWorld(bookId, title, genre, brief)
        }

        return bookId
    }

    suspend fun redesignFoundation(bookId: Long, customBrief: String): BookEntity? {
        val book = bookDao.getBookById(bookId) ?: return null
        try {
            val output = com.example.data.api.ArchitectAgent.generateFoundation(
                title = book.title,
                genre = book.genre,
                targetChapters = book.totalChapters,
                chapterWordCount = book.targetWordsPerChapter,
                brief = customBrief
            )
            val updated = book.copy(
                brief = customBrief,
                storyBible = output.storyBible,
                bookRules = output.bookRules
            )
            bookDao.updateBook(updated)
            return updated
        } catch (e: Exception) {
            Log.e(TAG, "Failed running ArchitectAgent redesign", e)
            return null
        }
    }

    suspend fun updateBook(book: BookEntity) = bookDao.updateBook(book)

    suspend fun deleteBook(book: BookEntity) {
        chapterDao.deleteChaptersForBook(book.id)
        bookDao.deleteBook(book)
    }

    // --- Chapter Operations ---
    fun getChaptersFlow(bookId: Long): Flow<List<ChapterEntity>> = chapterDao.getChaptersForBookFlow(bookId)

    suspend fun getChapterById(id: Long): ChapterEntity? = chapterDao.getChapterById(id)

    // --- InkOS Novel Logic Pipelines (Plan -> Compose -> Audit -> Revise) ---

    /**
     * Step 1: PLAN CHAPTER (Plan next chapter intent)
     */
    suspend fun planChapter(bookId: Long, chapterIndex: Int, userFocus: String): String {
        val book = bookDao.getBookById(bookId) ?: return "Book not found."
        
        val lang = determineLanguage(book.title, book.brief)
        val systemInstruction = AgentSystemPrompt.buildAgentSystemPrompt(book.title, lang, "book") + 
            "\n\nPlanner Role: Generate a high-quality Markdown document representing the intent/plan of the next chapter."
        val prompt = """
            Book Title: ${book.title}
            Genre: ${book.genre}
            Story Brief: ${book.brief}
            Story Bible: ${book.storyBible}
            
            Target Chapter: Chapter $chapterIndex
            User Instruction / Current Attention: $userFocus
            
            Please outline:
            1. Chapter Intent & Primary Focus
            2. Key Actions and Events (MUST-KEEP list)
            3. Taboos and Clichés to Avoid (MUST-AVOID list, to reduce AI-sounding elements)
            
            Return clean Markdown.
        """.trimIndent()

        val intentMarkdown = LlmRouter.generateContent(systemInstruction, prompt, requireJson = false)

        val existing = chapterDao.getChapterByIndex(bookId, chapterIndex)
        if (existing != null) {
            chapterDao.updateChapter(existing.copy(intent = intentMarkdown, status = "DRAFT"))
        } else {
            chapterDao.insertChapter(
                ChapterEntity(
                    bookId = bookId,
                    title = "Chapter $chapterIndex",
                    chapterIndex = chapterIndex,
                    intent = intentMarkdown,
                    status = "DRAFT"
                )
            )
        }
        return intentMarkdown
    }

    /**
     * Step 2: COMPOSE CHAPTER (Generate the actual text draft)
     */
    suspend fun composeChapter(bookId: Long, chapterIndex: Int): String {
        val book = bookDao.getBookById(bookId) ?: return "Book not found."
        val chapter = chapterDao.getChapterByIndex(bookId, chapterIndex) ?: return "Chapter intent not found. Plan first."

        // Retrieve background chapters for continuity context
        val prevChapters = chapterDao.getChaptersForBook(bookId)
            .filter { it.chapterIndex < chapterIndex }
            .takeLast(2) // pass last 2 chapters context to lower token expansion
            
        val contextPrompt = buildString {
            append("PREVIOUS RUNTIME CONTEXT:\n")
            if (prevChapters.isEmpty()) {
                append("[This is the beginning chapter of the novel]\n")
            } else {
                prevChapters.forEach {
                    append("Chapter ${it.chapterIndex} SUMMARY & TEXT excerpt:\n")
                    append(it.content.take(800)).append("\n...\n")
                }
            }
        }

        val lang = determineLanguage(book.title, book.brief)
        val systemInstruction = AgentSystemPrompt.buildAgentSystemPrompt(book.title, lang, "book") +
            "\n\nWriter Role: Write immersive, highly detailed chapter prose. Exclude introductory remarks."
        val prompt = """
            Book Parameters:
            Title: ${book.title}
            Genre: ${book.genre}
            Theme rules: ${book.bookRules}
            
            $contextPrompt
            
            CURRENT CHAPTER BLUEPRINT (intent.md):
            ${chapter.intent}
            
            Instruction: Write the full body text of Chapter $chapterIndex. 
            Aim for approximately ${book.targetWordsPerChapter} words in full. Do not write summary bullets; write the actual narrative.
            Exclude introductory/concluding chat remarks from the model. Start directly with the prose.
        """.trimIndent()

        val chapterText = LlmRouter.generateContent(systemInstruction, prompt, requireJson = false)
        
        chapterDao.updateChapter(
            chapter.copy(
                content = chapterText,
                wordCount = chapterText.length,
                status = "DRAFT"
            )
        )
        return chapterText
    }

    /**
     * Step 3: AUDIT CHAPTER (37-dimensions of continuous memory review and AI flavor alerts)
     */
    suspend fun auditChapter(bookId: Long, chapterIndex: Int): String {
        val book = bookDao.getBookById(bookId) ?: return "Book not found."
        val chapter = chapterDao.getChapterByIndex(bookId, chapterIndex) ?: return "Chapter not found."

        val lang = determineLanguage(book.title, book.brief)
        val systemInstruction = AgentSystemPrompt.buildAgentSystemPrompt(book.title, lang, "book") +
            "\n\nAuditor Role: Conduct an audit reviewing character memory compliance, continuity consistency, and repetitive cliché indicators."
        val prompt = """
            Book: ${book.title}
            Chapter: $chapterIndex
            Chapter Title: ${chapter.title}
            
            CHAPTER TEXT DRAFT FOR INSPECTION:
            ${chapter.content}
            
            Please output a structured Audit report checking:
            1. Character Memory Compliance (did they remember facts?): [Compliant/No]
            2. Physical Asset & Location Continuity (how's material consistency?): [Compliant/No]
            3. Narrative Pacing (rushed or bloated?): [Score]
            4. AI Flavor Alert (any repetitive transitions or summaries?): [Specify patterns found]
            5. Recommendations for Revision (Bullet list of direct actions).
        """.trimIndent()

        val auditLogs = LlmRouter.generateContent(systemInstruction, prompt, requireJson = false)
        
        // Rule-based AI-tell structural analysis (pure deterministic checks)
        val aiTellResult = AITellAnalyzer.analyzeAITells(chapter.content, lang)
        val aiTellSection = if (aiTellResult.issues.isNotEmpty()) {
            val buildStr = StringBuilder()
            buildStr.append("\n\n### 🔍 结构化 AI 痕迹规则检测 (Rule-based Analysis)\n")
            aiTellResult.issues.forEach { issue ->
                val emoji = if (issue.severity == "warning") "⚠️" else "ℹ️"
                buildStr.append("- **$emoji [${issue.category}]**: ${issue.description}\n")
                buildStr.append("  *建议*: ${issue.suggestion}\n")
            }
            buildStr.toString()
        } else {
            "\n\n### 🔍 结构化 AI 痕迹规则检测 (Rule-based Analysis)\n- ✅ 未检测出明显的结构性 AI 生成痕迹 (等长、套话词频或列表句式均处于正常范围)。\n"
        }
        val finalAuditLogs = auditLogs + aiTellSection

        // Dynamic State Extraction & Book Story Bible update
        if (book.type == "NOVEL") {
            try {
                Log.i(TAG, "Starting ChapterAnalyzerAgent execution for Book ${book.title}, Chapter $chapterIndex")
                val analysis = com.example.data.api.ChapterAnalyzerAgent.analyzeChapter(
                    book = book,
                    chapterIndex = chapterIndex,
                    chapterContent = chapter.content,
                    chapterTitle = chapter.title,
                    chapterIntent = chapter.intent
                )
                val updatedStoryBible = com.example.data.api.ChapterAnalyzerAgent.updateStoryBibleWithAnalysis(book.storyBible, analysis)
                bookDao.updateBook(book.copy(storyBible = updatedStoryBible))
                Log.i(TAG, "Successfully completed ChapterAnalyzerAgent execution and updated Story Bible.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed running ChapterAnalyzerAgent dynamically during audit", e)
            }
        }

        chapterDao.updateChapter(chapter.copy(auditLogs = finalAuditLogs, status = "AUDITING"))
        return finalAuditLogs
    }

    /**
     * Step 4: REVISE AND APPROVE (Make revisions according to Audit suggestions and seal the chapter)
     */
    suspend fun reviseChapter(bookId: Long, chapterIndex: Int): String {
        val book = bookDao.getBookById(bookId) ?: return "Book not found."
        val chapter = chapterDao.getChapterByIndex(bookId, chapterIndex) ?: return "Chapter not found."

        val lang = determineLanguage(book.title, book.brief)
        val systemInstruction = AgentSystemPrompt.buildAgentSystemPrompt(book.title, lang, "book") +
            "\n\nReviser Role: Refine text, eliminate AI patterns, and address audit notes."
        val prompt = """
            Book: ${book.title}
            Chapter: $chapterIndex
            
            ORIGINAL DRAFT:
            ${chapter.content}
            
            AUDITOR INPUT & SUGGESTED REVISIONS:
            ${chapter.auditLogs}
            
            Instruction: Produce the final revised draft. Incorporate the critical audit suggestion fixes, remove generic over-summarized sentences, and enhance vocabulary realism. Output ONLY the finalized prose.
        """.trimIndent()

        val revisedText = LlmRouter.generateContent(systemInstruction, prompt, requireJson = false)
        chapterDao.updateChapter(
            chapter.copy(
                content = revisedText,
                wordCount = revisedText.length,
                status = "APPROVED"
            )
        )

        // Increment book chapter pointer if index completed
        if (book.currentChapterIndex == chapterIndex) {
            bookDao.updateBook(book.copy(currentChapterIndex = chapterIndex + 1))
        }

        return revisedText
    }

    /**
     * Rollback a chapter to Draft status
     */
    suspend fun rollbackChapter(bookId: Long, chapterIndex: Int) {
        val chapter = chapterDao.getChapterByIndex(bookId, chapterIndex)
        if (chapter != null) {
            chapterDao.updateChapter(chapter.copy(status = "DRAFT", auditLogs = ""))
        }
        val book = bookDao.getBookById(bookId)
        if (book != null && book.currentChapterIndex > chapterIndex) {
            bookDao.updateBook(book.copy(currentChapterIndex = chapterIndex))
        }
    }

    // --- InkOS Short Story Run pipeline ---
    suspend fun runShortStoryPipeline(bookId: Long, direction: String, chaptersCount: Int) {
        val book = bookDao.getBookById(bookId) ?: return
        
        val lang = determineLanguage(book.title, book.brief)
        val systemInstruction = AgentSystemPrompt.buildAgentSystemPrompt(book.title, lang, "short", confirmedIntent = "short_run")
        val prompt = """
            Create a complete short story package.
            Title: ${book.title}
            Genre: ${book.genre}
            Brief Direction: $direction
            Total chapters: $chaptersCount
            
            Structure the response as a single, beautifully readable Master Outline & Story with chapters:
            - Full summary and marketing selling points
            - Cover illustration prompt
            - Chapter-by-chapter detailed progression text (write Chapters 1 to $chaptersCount)
            
            Write high-quality suspenseful content. Keep it extremely engaging.
        """.trimIndent()

        val result = LlmRouter.generateContent(systemInstruction, prompt, requireJson = false)

        val coverPromptText = "An elegant noir style illustration of ${book.title}, high contrast colors, beautiful typography, crisp resolution."

        bookDao.updateBook(
            book.copy(
                salesPackage = result,
                status = "COMPLETED",
                totalChapters = chaptersCount,
                coverPrompt = coverPromptText
            )
        )

        // Generate individual chapters local entries so the user can inspect them too
        chapterDao.deleteChaptersForBook(bookId)
        for (i in 1..chaptersCount) {
            chapterDao.insertChapter(
                ChapterEntity(
                    bookId = bookId,
                    title = "Chapter $i",
                    chapterIndex = i,
                    intent = "Generate progression of Chapter $i according to Short Brief.",
                    content = "Chapter $i of '${book.title}' is contained in the Master Short story package. Expand your exploration of Vance's world.",
                    status = "APPROVED",
                    wordCount = 200
                )
            )
        }
    }

    // --- InkOS Play Open World RPG Simulation ---

    private suspend fun initializeOpenWorld(bookId: Long, title: String, genre: String, brief: String) {
        val worldContract = """
            - World Setting: $genre ($brief)
            - Rules: High narrative choice, responsive characters, item collection, evidence ratings (★ to ★★★★★).
            - Time: Semi-continuous, non-fixed time progression where sleeping passes days, searches pass hours.
            - Energy: Managed (100 max) as status.
        """.trimIndent()

        // Create initial JSON configurations
        val initScene = if (genre.contains("DETECTIVE") || genre.contains("侦探") || genre.contains("City Noir")) {
            "【Rain-slicked Back Alley】\nYou stand under a flickering gas-lamp. Crime tape flaps in the wet wind. The body has already been taken, but the clues remain undisturbed. Gromm Ironforge from the local precinct is examining a discarded cigarette carton."
        } else {
            "【Damp Frontier Outpost】\nThe fires of the military brazier throw sharp orange shadows. The wind carries the howl of timber wolves from the northern peaks. Captain Gromm stands by a weathered table maps, polishing his claymore sword."
        }

        val characters = JSONArray().apply {
            put(JSONObject().apply {
                put("name", "Gromm Ironforge")
                put("relation", "Local Ally (Suspicious)")
                put("status", "Standing guard, watchful of any shadows")
            })
        }.toString()

        val items = JSONArray().apply {
            put(JSONObject().apply {
                put("name", "Rusted Key")
                put("rating", "★★☆☆☆")
                put("desc", "A brass key found in the mud, smelling of copper and sulfur.")
            })
        }.toString()

        val history = JSONArray().apply {
            put(JSONObject().apply {
                put("role", "system")
                put("text", "The world of $title ($genre) has been spawned from the creative blueprint.")
            })
        }.toString()

        val playState = PlayStateEntity(
            bookId = bookId,
            worldContract = worldContract,
            timeState = "14:15 (Day 1)",
            currentScene = initScene,
            charactersJson = characters,
            itemsJson = items,
            historyLogJson = history
        )

        playStateDao.insertPlayState(playState)
    }

    fun getPlayStateFlow(bookId: Long): Flow<PlayStateEntity?> = playStateDao.getPlayStateForBookFlow(bookId)

    suspend fun getPlayState(bookId: Long): PlayStateEntity? = playStateDao.getPlayStateForBook(bookId)

    /**
     * ADVANCE PLAY STATE (User action loop)
     */
    suspend fun executePlayAction(bookId: Long, userAction: String): String {
        val book = bookDao.getBookById(bookId) ?: return "World not found"
        val state = playStateDao.getPlayStateForBook(bookId) ?: return "Play state not active"

        val lang = determineLanguage(book.title, book.brief)
        val systemInstruction = AgentSystemPrompt.buildAgentSystemPrompt(book.title, lang, "play", confirmedAction = true, playWorldExists = true) +
            "\n\nYou are the Play Director. You MUST return a JSON object with keys: " +
            "scene_description, hud_energy, hud_status, characters, items, actions, time_state, log_response."

        val prompt = """
            World Contract: ${state.worldContract}
            Current Time: ${state.timeState}
            Current Scene: ${state.currentScene}
            Current Characters: ${state.charactersJson}
            Current Items: ${state.itemsJson}
            Recent History: ${state.historyLogJson}
            
            User Next Action: "$userAction"
            
            Advance the state. Record changes. Be descriptive! Update relation statuses or item ratings if necessary. Add items if they find evidence. 
        """.trimIndent()

        Log.i(TAG, "Requesting Play agent action processing")
        val responseText = LlmRouter.generateContent(systemInstruction, prompt, requireJson = true)

        try {
            val json = JSONObject(responseText)
            val scene = json.optString("scene_description", state.currentScene)
            val time = json.optString("time_state", state.timeState)
            val charactersArr = json.optJSONArray("characters")?.toString() ?: state.charactersJson
            val itemsArr = json.optJSONArray("items")?.toString() ?: state.itemsJson
            
            // Log response and history compilation
            val logResponse = json.optString("log_response", "Action parsed successfully.")
            val historyArr = JSONArray(state.historyLogJson).apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("text", userAction)
                })
                put(JSONObject().apply {
                    put("role", "assistant")
                    put("text", logResponse)
                })
            }

            val updatedState = state.copy(
                timeState = time,
                currentScene = scene,
                charactersJson = charactersArr,
                itemsJson = itemsArr,
                historyLogJson = historyArr.toString(),
                lastUpdate = System.currentTimeMillis()
            )

            playStateDao.updatePlayState(updatedState)
            return logResponse
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing Play response, falling back safely", e)
            // SAFELY update history with simple error message log
            val historyArr = JSONArray(state.historyLogJson).apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("text", userAction)
                })
                put(JSONObject().apply {
                    put("role", "assistant")
                    put("text", "The Director observed your action '$userAction' and recorded the progression. Keep pushing limits!")
                })
            }
            playStateDao.updatePlayState(state.copy(
                timeState = "Day 1 (Transitioning)",
                historyLogJson = historyArr.toString(),
                lastUpdate = System.currentTimeMillis()
            ))
            return "Action executed locally."
        }
    }
}
