package com.example.data.agents

import android.util.Log
import com.example.data.llm.LLMMessage
import com.example.data.llm.ChatCompletionOptions
import com.example.data.models.*

/**
 * Writer agent output - matches TypeScript WriteChapterOutput
 */
data class WriterOutput(
    val chapterNumber: Int,
    val title: String,
    val content: String,
    val wordCount: Int,
    val preWriteCheck: String,
    val postSettlement: String,
    val updatedState: String,
    val updatedLedger: String,
    val updatedHooks: String,
    val chapterSummary: String,
    val updatedSubplots: String,
    val updatedEmotionalArcs: String,
    val updatedCharacterMatrix: String,
    val tokenUsage: TokenUsage? = null
)

/**
 * Writer Agent - matches TypeScript WriterAgent
 * Generates chapter content with state settlement
 */
class WriterAgent(ctx: AgentContext) : BaseAgent(ctx) {
    override val name: String = "writer"
    
    companion object {
        private const val TAG = "WriterAgent"
        
        // Context budget limits
        private const val STORY_BIBLE_BUDGET = 14000
        private const val CURRENT_STATE_BUDGET = 7000
        private const val LEDGER_BUDGET = 6000
        private const val HOOKS_BUDGET = 9000
        private const val CHAPTER_SUMMARIES_BUDGET = 9000
        private const val SUBPLOT_BUDGET = 7000
        private const val EMOTIONAL_ARCS_BUDGET = 7000
        private const val CHARACTER_MATRIX_BUDGET = 12000
    }

    /**
     * Write a chapter - matches TypeScript writeChapter method
     */
    suspend fun writeChapter(
        book: BookConfig,
        chapterNumber: Int,
        storyBible: String,
        volumeOutline: String,
        currentState: String,
        ledger: String,
        hooks: String,
        chapterSummaries: String,
        subplotBoard: String,
        emotionalArcs: String,
        characterMatrix: String,
        chapterIntent: String? = null,
        chapterMemo: ChapterMemo? = null,
        externalContext: String? = null,
        lengthSpec: LengthSpec? = null,
        language: String = "zh"
    ): WriterOutput {
        Log.i(TAG, "Writing chapter $chapterNumber for book ${book.id}")
        
        val targetWords = lengthSpec?.target ?: book.chapterWordCount
        val resolvedLengthSpec = lengthSpec ?: buildLengthSpec(targetWords, language)
        
        // Phase 1: Creative writing
        Log.i(TAG, "Phase 1: creative writing for chapter $chapterNumber")
        val creativeSystemPrompt = buildWriterSystemPrompt(book, language, resolvedLengthSpec)
        val creativeUserPrompt = buildWriterUserPrompt(
            chapterNumber = chapterNumber,
            storyBible = storyBible,
            volumeOutline = volumeOutline,
            currentState = currentState,
            ledger = ledger,
            hooks = hooks,
            recentChapters = "", // TODO: Load recent chapters
            chapterSummaries = chapterSummaries,
            subplotBoard = subplotBoard,
            emotionalArcs = emotionalArcs,
            characterMatrix = characterMatrix,
            chapterIntent = chapterIntent,
            chapterMemo = chapterMemo,
            externalContext = externalContext,
            lengthSpec = resolvedLengthSpec,
            language = language
        )
        
        val creativeMessages = listOf(
            LLMMessage(role = "system", content = creativeSystemPrompt),
            LLMMessage(role = "user", content = creativeUserPrompt)
        )
        
        val creativeResponse = chat(creativeMessages, ChatCompletionOptions(temperature = 0.7))
        val creative = parseCreativeOutput(chapterNumber, creativeResponse.content, language)
        
        // Phase 2: State settlement
        Log.i(TAG, "Phase 2: state settlement for chapter $chapterNumber")
        val settlement = settleChapterState(
            book = book,
            chapterNumber = chapterNumber,
            title = creative.title,
            content = creative.content,
            currentState = currentState,
            ledger = ledger,
            hooks = hooks,
            chapterSummaries = chapterSummaries,
            subplotBoard = subplotBoard,
            emotionalArcs = emotionalArcs,
            characterMatrix = characterMatrix,
            volumeOutline = volumeOutline,
            chapterIntent = chapterIntent,
            language = language
        )
        
        val totalUsage = TokenUsage(
            promptTokens = (creativeResponse.usage.promptTokens) + (settlement.tokenUsage?.promptTokens ?: 0),
            completionTokens = (creativeResponse.usage.completionTokens) + (settlement.tokenUsage?.completionTokens ?: 0),
            totalTokens = (creativeResponse.usage.totalTokens) + (settlement.tokenUsage?.totalTokens ?: 0)
        )
        
        return WriterOutput(
            chapterNumber = chapterNumber,
            title = creative.title,
            content = creative.content,
            wordCount = countChapterLength(creative.content, resolvedLengthSpec.countingMode),
            preWriteCheck = creative.preWriteCheck,
            postSettlement = settlement.postSettlement,
            updatedState = settlement.updatedState,
            updatedLedger = settlement.updatedLedger,
            updatedHooks = settlement.updatedHooks,
            chapterSummary = settlement.chapterSummary,
            updatedSubplots = settlement.updatedSubplots,
            updatedEmotionalArcs = settlement.updatedEmotionalArcs,
            updatedCharacterMatrix = settlement.updatedCharacterMatrix,
            tokenUsage = totalUsage
        )
    }

    /**
     * Settle chapter state via LLM
     */
    private suspend fun settleChapterState(
        book: BookConfig,
        chapterNumber: Int,
        title: String,
        content: String,
        currentState: String,
        ledger: String,
        hooks: String,
        chapterSummaries: String,
        subplotBoard: String,
        emotionalArcs: String,
        characterMatrix: String,
        volumeOutline: String,
        chapterIntent: String?,
        language: String
    ): SettlementResult {
        val systemPrompt = buildSettlerSystemPrompt(book, language)
        val userPrompt = buildSettlerUserPrompt(
            chapterNumber = chapterNumber,
            title = title,
            content = content,
            currentState = currentState,
            ledger = ledger,
            hooks = hooks,
            chapterSummaries = chapterSummaries,
            subplotBoard = subplotBoard,
            emotionalArcs = emotionalArcs,
            characterMatrix = characterMatrix,
            volumeOutline = volumeOutline,
            chapterIntent = chapterIntent,
            language = language
        )
        
        val messages = listOf(
            LLMMessage(role = "system", content = systemPrompt),
            LLMMessage(role = "user", content = userPrompt)
        )
        
        val response = chat(messages, ChatCompletionOptions(temperature = 0.3))
        return parseSettlementOutput(response.content, language, response.usage)
    }

    /**
     * Build writer system prompt
     */
    private fun buildWriterSystemPrompt(book: BookConfig, language: String, lengthSpec: LengthSpec): String {
        return if (language == "en") {
            """
            You are a professional ${book.genre} web-fiction writer. Write immersive, highly detailed chapter prose.
            
            Requirements:
            - Target length: ${lengthSpec.target} words
            - Acceptable range: ${lengthSpec.softMin}-${lengthSpec.softMax} words
            - Write in vivid, engaging prose
            - Show, don't tell
            - Use distinct character voices
            - Maintain continuity with established facts
            
            Output format:
            === PRE_WRITE_CHECK ===
            (Self-check table)
            
            === CHAPTER_TITLE ===
            (Chapter title)
            
            === CHAPTER_CONTENT ===
            (Full chapter content)
            """.trimIndent()
        } else {
            """
            你是一位专业的${book.genre}网络小说写手。撰写沉浸式、高度详细的章节散文。
            
            要求：
            - 目标字数：${lengthSpec.target}字
            - 允许区间：${lengthSpec.softMin}-${lengthSpec.softMax}字
            - 用生动、引人入胜的笔触写作
            - 展示而非叙述
            - 使用独特的角色声音
            - 与已建立的事实保持连续性
            
            输出格式：
            === PRE_WRITE_CHECK ===
            （自检表）
            
            === CHAPTER_TITLE ===
            （章节标题）
            
            === CHAPTER_CONTENT ===
            （完整章节内容）
            """.trimIndent()
        }
    }

    /**
     * Build writer user prompt
     */
    private fun buildWriterUserPrompt(
        chapterNumber: Int,
        storyBible: String,
        volumeOutline: String,
        currentState: String,
        ledger: String,
        hooks: String,
        recentChapters: String,
        chapterSummaries: String,
        subplotBoard: String,
        emotionalArcs: String,
        characterMatrix: String,
        chapterIntent: String?,
        chapterMemo: ChapterMemo?,
        externalContext: String?,
        lengthSpec: LengthSpec,
        language: String
    ): String {
        val contextBlock = if (!externalContext.isNullOrBlank()) {
            if (language == "en") "\n## External Context\n$externalContext\n" else "\n## 外部指令\n$externalContext\n"
        } else ""
        
        val intentBlock = if (!chapterIntent.isNullOrBlank()) {
            if (language == "en") "\n## Chapter Intent\n$chapterIntent\n" else "\n## 章节意图\n$chapterIntent\n"
        } else ""
        
        val memoBlock = if (chapterMemo != null) {
            if (language == "en") "\n## Chapter Memo\n${chapterMemo.body.take(2000)}\n" else "\n## 章节备忘录\n${chapterMemo.body.take(2000)}\n"
        } else ""
        
        return if (language == "en") {
            """
            Write chapter $chapterNumber.
            $contextBlock$intentBlock$memoBlock
            ## Current State
            ${currentState.take(CURRENT_STATE_BUDGET)}
            
            ## Resource Ledger
            ${ledger.take(LEDGER_BUDGET)}
            
            ## Plot Threads
            ${hooks.take(HOOKS_BUDGET)}
            
            ## Chapter Summaries
            ${chapterSummaries.take(CHAPTER_SUMMARIES_BUDGET)}
            
            ## Subplot Board
            ${subplotBoard.take(SUBPLOT_BUDGET)}
            
            ## Emotional Arcs
            ${emotionalArcs.take(EMOTIONAL_ARCS_BUDGET)}
            
            ## Character Matrix
            ${characterMatrix.take(CHARACTER_MATRIX_BUDGET)}
            
            ## Recent Chapters
            ${recentChapters.ifEmpty { "(This is the first chapter)" }}
            
            ## Worldbuilding
            ${storyBible.take(STORY_BIBLE_BUDGET)}
            
            ## Volume Outline
            ${volumeOutline.take(8000)}
            
            Requirements:
            - Target length: ${lengthSpec.target} words
            - Acceptable range: ${lengthSpec.softMin}-${lengthSpec.softMax} words
            
            Output PRE_WRITE_CHECK first, then the chapter.
            """.trimIndent()
        } else {
            """
            请续写第${chapterNumber}章。
            $contextBlock$intentBlock$memoBlock
            ## 当前状态卡
            ${currentState.take(CURRENT_STATE_BUDGET)}
            
            ## 资源账本
            ${ledger.take(LEDGER_BUDGET)}
            
            ## 伏笔池
            ${hooks.take(HOOKS_BUDGET)}
            
            ## 章节摘要
            ${chapterSummaries.take(CHAPTER_SUMMARIES_BUDGET)}
            
            ## 支线进度板
            ${subplotBoard.take(SUBPLOT_BUDGET)}
            
            ## 情感弧线
            ${emotionalArcs.take(EMOTIONAL_ARCS_BUDGET)}
            
            ## 角色交互矩阵
            ${characterMatrix.take(CHARACTER_MATRIX_BUDGET)}
            
            ## 最近章节
            ${recentChapters.ifEmpty { "(这是第一章，无前文)" }}
            
            ## 世界观设定
            ${storyBible.take(STORY_BIBLE_BUDGET)}
            
            ## 卷纲
            ${volumeOutline.take(8000)}
            
            要求：
            - 目标字数：${lengthSpec.target}字
            - 允许区间：${lengthSpec.softMin}-${lengthSpec.softMax}字
            
            先输出写作自检表，再写正文。
            """.trimIndent()
        }
    }

    /**
     * Build settler system prompt
     */
    private fun buildSettlerSystemPrompt(book: BookConfig, language: String): String {
        return if (language == "en") {
            """
            You are a fiction continuity analyst. Analyze a finished chapter, extract every state change, and update the tracking files.
            
            Output format:
            === POST_SETTLEMENT ===
            (Settlement notes)
            
            === UPDATED_STATE ===
            (Updated state card as Markdown table)
            
            === UPDATED_LEDGER ===
            (Updated resource ledger)
            
            === UPDATED_HOOKS ===
            (Updated hooks board)
            
            === CHAPTER_SUMMARY ===
            (Chapter summary row)
            
            === UPDATED_SUBPLOTS ===
            (Updated subplot board)
            
            === UPDATED_EMOTIONAL_ARCS ===
            (Updated emotional arcs)
            
            === UPDATED_CHARACTER_MATRIX ===
            (Updated character matrix)
            """.trimIndent()
        } else {
            """
            你是小说连续性分析师。分析一章已完成的小说正文，提取所有状态变化并更新追踪文件。
            
            输出格式：
            === POST_SETTLEMENT ===
            （结算说明）
            
            === UPDATED_STATE ===
            （更新后的状态卡，Markdown表格）
            
            === UPDATED_LEDGER ===
            （更新后的资源账本）
            
            === UPDATED_HOOKS ===
            （更新后的伏笔池）
            
            === CHAPTER_SUMMARY ===
            （章节摘要行）
            
            === UPDATED_SUBPLOTS ===
            （更新后的支线进度板）
            
            === UPDATED_EMOTIONAL_ARCS ===
            （更新后的情感弧线）
            
            === UPDATED_CHARACTER_MATRIX ===
            （更新后的角色矩阵）
            """.trimIndent()
        }
    }

    /**
     * Build settler user prompt
     */
    private fun buildSettlerUserPrompt(
        chapterNumber: Int,
        title: String,
        content: String,
        currentState: String,
        ledger: String,
        hooks: String,
        chapterSummaries: String,
        subplotBoard: String,
        emotionalArcs: String,
        characterMatrix: String,
        volumeOutline: String,
        chapterIntent: String?,
        language: String
    ): String {
        val intentBlock = if (!chapterIntent.isNullOrBlank()) {
            if (language == "en") "\n## Chapter Intent\n$chapterIntent\n" else "\n## 章节意图\n$chapterIntent\n"
        } else ""
        
        return if (language == "en") {
            """
            Analyze chapter $chapterNumber: $title
            $intentBlock
            ## Chapter Content
            ${content.take(20000)}
            
            ## Current State
            ${currentState.take(CURRENT_STATE_BUDGET)}
            
            ## Resource Ledger
            ${ledger.take(LEDGER_BUDGET)}
            
            ## Pending Hooks
            ${hooks.take(HOOKS_BUDGET)}
            
            ## Chapter Summaries
            ${chapterSummaries.take(CHAPTER_SUMMARIES_BUDGET)}
            
            ## Subplot Board
            ${subplotBoard.take(SUBPLOT_BUDGET)}
            
            ## Emotional Arcs
            ${emotionalArcs.take(EMOTIONAL_ARCS_BUDGET)}
            
            ## Character Matrix
            ${characterMatrix.take(CHARACTER_MATRIX_BUDGET)}
            
            ## Volume Outline
            ${volumeOutline.take(8000)}
            
            Update all tracking files based on this chapter.
            """.trimIndent()
        } else {
            """
            请分析第${chapterNumber}章：$title
            $intentBlock
            ## 正文内容
            ${content.take(20000)}
            
            ## 当前状态卡
            ${currentState.take(CURRENT_STATE_BUDGET)}
            
            ## 资源账本
            ${ledger.take(LEDGER_BUDGET)}
            
            ## 待回收伏笔
            ${hooks.take(HOOKS_BUDGET)}
            
            ## 章节摘要
            ${chapterSummaries.take(CHAPTER_SUMMARIES_BUDGET)}
            
            ## 支线进度板
            ${subplotBoard.take(SUBPLOT_BUDGET)}
            
            ## 情感弧线
            ${emotionalArcs.take(EMOTIONAL_ARCS_BUDGET)}
            
            ## 角色矩阵
            ${characterMatrix.take(CHARACTER_MATRIX_BUDGET)}
            
            ## 卷纲
            ${volumeOutline.take(8000)}
            
            基于本章更新所有追踪文件。
            """.trimIndent()
        }
    }

    /**
     * Parse creative output from LLM response
     */
    private fun parseCreativeOutput(chapterNumber: Int, content: String, language: String): CreativeResult {
        val title = extractTag(content, "CHAPTER_TITLE") ?: 
            if (language == "en") "Chapter $chapterNumber" else "第${chapterNumber}章"
        val chapterContent = extractTag(content, "CHAPTER_CONTENT") ?: content
        val preWriteCheck = extractTag(content, "PRE_WRITE_CHECK") ?: ""
        
        return CreativeResult(
            title = title.trim(),
            content = chapterContent.trim(),
            preWriteCheck = preWriteCheck.trim()
        )
    }

    /**
     * Parse settlement output from LLM response
     */
    private fun parseSettlementOutput(content: String, language: String, usage: com.example.data.llm.LLMTokenUsage): SettlementResult {
        return SettlementResult(
            postSettlement = extractTag(content, "POST_SETTLEMENT") ?: "",
            updatedState = extractTag(content, "UPDATED_STATE") ?: 
                if (language == "en") "(State not updated)" else "(状态卡未更新)",
            updatedLedger = extractTag(content, "UPDATED_LEDGER") ?: "",
            updatedHooks = extractTag(content, "UPDATED_HOOKS") ?: 
                if (language == "en") "(Hooks not updated)" else "(伏笔池未更新)",
            chapterSummary = extractTag(content, "CHAPTER_SUMMARY") ?: "",
            updatedSubplots = extractTag(content, "UPDATED_SUBPLOTS") ?: "",
            updatedEmotionalArcs = extractTag(content, "UPDATED_EMOTIONAL_ARCS") ?: "",
            updatedCharacterMatrix = extractTag(content, "UPDATED_CHARACTER_MATRIX") ?: "",
            tokenUsage = TokenUsage(
                promptTokens = usage.promptTokens,
                completionTokens = usage.completionTokens,
                totalTokens = usage.totalTokens
            )
        )
    }

    /**
     * Extract content between === TAG === markers
     */
    private fun extractTag(content: String, tag: String): String? {
        val regex = Regex("===\\s*$tag\\s*===\\s*([\\s\\S]*?)(?=\\s*===\\s*[A-Z_]+\\s*===|$)")
        val match = regex.find(content)
        return match?.groupValues?.get(1)?.trim()
    }
}

/**
 * Creative result from writer
 */
private data class CreativeResult(
    val title: String,
    val content: String,
    val preWriteCheck: String
)

/**
 * Settlement result from state settlement
 */
private data class SettlementResult(
    val postSettlement: String,
    val updatedState: String,
    val updatedLedger: String,
    val updatedHooks: String,
    val chapterSummary: String,
    val updatedSubplots: String,
    val updatedEmotionalArcs: String,
    val updatedCharacterMatrix: String,
    val tokenUsage: TokenUsage? = null
)
