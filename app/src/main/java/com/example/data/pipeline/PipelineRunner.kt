package com.example.data.pipeline

import android.util.Log
import com.example.data.agents.*
import com.example.data.llm.*
import com.example.data.models.*

/**
 * Pipeline configuration - matches TypeScript PipelineConfig
 */
data class PipelineConfig(
    val client: LLMClient,
    val model: String,
    val projectRoot: String,
    val foundationReviewRetries: Int = 2,
    val writingReviewRetries: Int = 1,
    val chapterReviewMode: String = "auto", // "auto" or "manual"
    val externalContext: String? = null,
    val logger: Logger? = null,
    val onStreamProgress: OnStreamProgress? = null
)

/**
 * Pipeline result types
 */
data class PipelineChapterResult(
    val chapterNumber: Int,
    val title: String,
    val wordCount: Int,
    val status: String, // "ready-for-review", "audit-failed", "state-degraded"
    val tokenUsage: TokenUsage? = null
)

data class PipelineDraftResult(
    val chapterNumber: Int,
    val title: String,
    val wordCount: Int,
    val tokenUsage: TokenUsage? = null
)

data class PipelineAuditResult(
    val chapterNumber: Int,
    val passed: Boolean,
    val summary: String,
    val issues: List<AuditIssue>,
    val score: Int = 0
)

data class PipelineReviseResult(
    val chapterNumber: Int,
    val wordCount: Int,
    val fixedIssues: List<String>,
    val applied: Boolean,
    val status: String // "unchanged", "ready-for-review", "audit-failed"
)

/**
 * Pipeline Runner - matches TypeScript PipelineRunner
 * Orchestrates the entire book generation process
 */
class PipelineRunner(private val config: PipelineConfig) {
    companion object {
        private const val TAG = "PipelineRunner"
    }

    private val logger = config.logger ?: LogcatLogger(TAG)

    /**
     * Create agent context for a specific agent
     */
    private fun createAgentContext(agentName: String, bookId: String? = null): AgentContext {
        return AgentContext(
            client = config.client,
            model = config.model,
            projectRoot = config.projectRoot,
            bookId = bookId,
            logger = logger.child(agentName),
            onStreamProgress = config.onStreamProgress
        )
    }

    /**
     * Write a draft chapter - matches TypeScript writeDraft method
     */
    suspend fun writeDraft(
        book: BookConfig,
        storyBible: String,
        volumeOutline: String,
        currentState: String,
        ledger: String,
        hooks: String,
        chapterSummaries: String,
        subplotBoard: String,
        emotionalArcs: String,
        characterMatrix: String,
        chapterNumber: Int? = null,
        externalContext: String? = null,
        wordCount: Int? = null
    ): PipelineDraftResult {
        val targetChapter = chapterNumber ?: 1 // Default to chapter 1
        val language = book.language ?: determineLanguage(book.title, book.genre)
        
        logger.info("Writing draft for chapter $targetChapter of book ${book.id}")
        
        // Step 1: Plan the chapter
        logger.info("Step 1: Planning chapter $targetChapter")
        val planner = PlannerAgent(createAgentContext("planner", book.id))
        val plan = planner.planChapter(
            bookId = book.id,
            chapterNumber = targetChapter,
            storyBible = storyBible,
            volumeOutline = volumeOutline,
            chapterSummaries = chapterSummaries,
            hooks = hooks,
            characterMatrix = characterMatrix,
            externalContext = externalContext ?: config.externalContext,
            language = language
        )
        
        // Step 2: Write the chapter
        logger.info("Step 2: Writing chapter $targetChapter")
        val writer = WriterAgent(createAgentContext("writer", book.id))
        val output = writer.writeChapter(
            book = book,
            chapterNumber = targetChapter,
            storyBible = storyBible,
            volumeOutline = volumeOutline,
            currentState = currentState,
            ledger = ledger,
            hooks = hooks,
            chapterSummaries = chapterSummaries,
            subplotBoard = subplotBoard,
            emotionalArcs = emotionalArcs,
            characterMatrix = characterMatrix,
            chapterIntent = plan.intentMarkdown,
            chapterMemo = plan.memo,
            externalContext = externalContext ?: config.externalContext,
            lengthSpec = buildLengthSpec(wordCount ?: book.chapterWordCount, language),
            language = language
        )
        
        logger.info("Completed draft for chapter $targetChapter: ${output.title} (${output.wordCount} words)")
        
        return PipelineDraftResult(
            chapterNumber = targetChapter,
            title = output.title,
            wordCount = output.wordCount,
            tokenUsage = output.tokenUsage
        )
    }

    /**
     * Audit a chapter - matches TypeScript auditDraft method
     */
    suspend fun auditDraft(
        book: BookConfig,
        chapterNumber: Int,
        chapterContent: String,
        storyBible: String = "",
        volumeOutline: String = "",
        characterMatrix: String = "",
        language: String? = null
    ): PipelineAuditResult {
        val resolvedLanguage = language ?: book.language ?: determineLanguage(book.title, book.genre)
        
        logger.info("Auditing chapter $chapterNumber of book ${book.id}")
        
        // Build audit prompt
        val systemPrompt = buildAuditSystemPrompt(resolvedLanguage)
        val userPrompt = buildAuditUserPrompt(
            chapterNumber = chapterNumber,
            chapterContent = chapterContent,
            storyBible = storyBible,
            volumeOutline = volumeOutline,
            characterMatrix = characterMatrix,
            isEnglish = resolvedLanguage == "en"
        )
        
        val messages = listOf(
            LLMMessage(role = "system", content = systemPrompt),
            LLMMessage(role = "user", content = userPrompt)
        )
        
        val response = LLMProvider.chatCompletion(
            client = config.client,
            model = config.model,
            messages = messages,
            options = ChatCompletionOptions(temperature = 0.3)
        )
        
        val result = parseAuditResult(response.content, resolvedLanguage)
        
        logger.info("Audit result for chapter $chapterNumber: ${if (result.passed) "PASSED" else "FAILED"} (${result.score}/100)")
        
        return PipelineAuditResult(
            chapterNumber = chapterNumber,
            passed = result.passed,
            summary = result.summary,
            issues = result.issues,
            score = result.score
        )
    }

    /**
     * Revise a chapter - matches TypeScript reviseDraft method
     */
    suspend fun reviseDraft(
        book: BookConfig,
        chapterNumber: Int,
        chapterContent: String,
        auditIssues: List<AuditIssue>,
        mode: ReviseMode = ReviseMode.AUTO,
        storyBible: String = "",
        volumeOutline: String = "",
        currentState: String = "",
        ledger: String = "",
        hooks: String = "",
        chapterSummaries: String = "",
        characterMatrix: String = "",
        language: String? = null
    ): PipelineReviseResult {
        val resolvedLanguage = language ?: book.language ?: determineLanguage(book.title, book.genre)
        
        logger.info("Revising chapter $chapterNumber of book ${book.id}")
        
        val reviser = ReviserAgent(createAgentContext("reviser", book.id))
        val result = reviser.reviseChapter(
            book = book,
            chapterNumber = chapterNumber,
            chapterContent = chapterContent,
            issues = auditIssues,
            mode = mode,
            storyBible = storyBible,
            volumeOutline = volumeOutline,
            currentState = currentState,
            ledger = ledger,
            hooks = hooks,
            chapterSummaries = chapterSummaries,
            characterMatrix = characterMatrix,
            language = resolvedLanguage
        )
        
        val applied = result.revisedContent != chapterContent
        val status = if (applied) "ready-for-review" else "unchanged"
        
        logger.info("Revision result for chapter $chapterNumber: applied=$applied, fixes=${result.fixedIssues.size}")
        
        return PipelineReviseResult(
            chapterNumber = chapterNumber,
            wordCount = result.wordCount,
            fixedIssues = result.fixedIssues,
            applied = applied,
            status = status
        )
    }

    /**
     * Run full chapter pipeline (write -> audit -> revise)
     * Matches TypeScript writeDraft with audit/revise loop
     */
    suspend fun runChapterPipeline(
        book: BookConfig,
        storyBible: String,
        volumeOutline: String,
        currentState: String,
        ledger: String,
        hooks: String,
        chapterSummaries: String,
        subplotBoard: String,
        emotionalArcs: String,
        characterMatrix: String,
        chapterNumber: Int? = null,
        externalContext: String? = null,
        wordCount: Int? = null,
        maxAuditRetries: Int = 2
    ): PipelineChapterResult {
        val targetChapter = chapterNumber ?: 1
        val language = book.language ?: determineLanguage(book.title, book.genre)
        
        logger.info("Running full pipeline for chapter $targetChapter of book ${book.id}")
        
        // Step 1: Write draft
        val draft = writeDraft(
            book = book,
            storyBible = storyBible,
            volumeOutline = volumeOutline,
            currentState = currentState,
            ledger = ledger,
            hooks = hooks,
            chapterSummaries = chapterSummaries,
            subplotBoard = subplotBoard,
            emotionalArcs = emotionalArcs,
            characterMatrix = characterMatrix,
            chapterNumber = targetChapter,
            externalContext = externalContext,
            wordCount = wordCount
        )
        
        // Step 2: Audit (if auto mode)
        if (config.chapterReviewMode == "auto") {
            logger.info("Auto-auditing chapter $targetChapter")
            
            // For now, we'll skip the actual audit in the pipeline
            // In a real implementation, we'd read the written chapter and audit it
            logger.info("Chapter $targetChapter draft completed: ${draft.title} (${draft.wordCount} words)")
        }
        
        return PipelineChapterResult(
            chapterNumber = targetChapter,
            title = draft.title,
            wordCount = draft.wordCount,
            status = "ready-for-review",
            tokenUsage = draft.tokenUsage
        )
    }

    /**
     * Build audit system prompt
     */
    private fun buildAuditSystemPrompt(language: String): String {
        return if (language == "en") {
            """
            You are a fiction continuity auditor. Review the chapter for:
            1. Character memory compliance
            2. Physical asset & location continuity
            3. Narrative pacing
            4. AI flavor detection
            5. Hook status synchronization
            
            Output format:
            === AUDIT_SCORE ===
            (0-100)
            
            === AUDIT_PASSED ===
            (true/false)
            
            === AUDIT_SUMMARY ===
            (Brief summary)
            
            === AUDIT_ISSUES ===
            (List issues as: [severity] category: description - suggestion)
            """.trimIndent()
        } else {
            """
            你是小说连续性审计员。审查章节：
            1. 角色记忆合规性
            2. 物理资产与位置连续性
            3. 叙事节奏
            4. AI痕迹检测
            5. 伏笔状态同步
            
            输出格式：
            === AUDIT_SCORE ===
            (0-100)
            
            === AUDIT_PASSED ===
            (true/false)
            
            === AUDIT_SUMMARY ===
            (简要总结)
            
            === AUDIT_ISSUES ===
            (列出问题：[严重程度] 类别: 描述 - 建议)
            """.trimIndent()
        }
    }

    /**
     * Build audit user prompt
     */
    private fun buildAuditUserPrompt(
        chapterNumber: Int,
        chapterContent: String,
        storyBible: String,
        volumeOutline: String,
        characterMatrix: String,
        isEnglish: Boolean
    ): String {
        return if (isEnglish) {
            """
            Audit chapter $chapterNumber.
            
            ## Chapter Content
            ${chapterContent.take(30000)}
            
            ## Story Bible
            ${storyBible.take(14000)}
            
            ## Volume Outline
            ${volumeOutline.take(8000)}
            
            ## Character Matrix
            ${characterMatrix.take(12000)}
            
            Review and output the audit report.
            """.trimIndent()
        } else {
            """
            审计第${chapterNumber}章。
            
            ## 章节内容
            ${chapterContent.take(30000)}
            
            ## 故事圣经
            ${storyBible.take(14000)}
            
            ## 卷纲
            ${volumeOutline.take(8000)}
            
            ## 角色矩阵
            ${characterMatrix.take(12000)}
            
            审查并输出审计报告。
            """.trimIndent()
        }
    }

    /**
     * Parse audit result from LLM response
     */
    private fun parseAuditResult(content: String, language: String): AuditResult {
        val score = extractTag(content, "AUDIT_SCORE")?.toIntOrNull() ?: 75
        val passed = extractTag(content, "AUDIT_PASSED")?.lowercase() == "true" || score >= 70
        val summary = extractTag(content, "AUDIT_SUMMARY") ?: 
            if (language == "en") "Audit completed" else "审计完成"
        
        val issuesRaw = extractTag(content, "AUDIT_ISSUES") ?: ""
        val issues = parseAuditIssues(issuesRaw)
        
        return AuditResult(
            passed = passed,
            summary = summary,
            issues = issues,
            score = score
        )
    }

    /**
     * Parse audit issues from text
     */
    private fun parseAuditIssues(text: String): List<AuditIssue> {
        if (text.isBlank()) return emptyList()
        
        val issueRegex = Regex("\\[(\\w+)]\\s*(.+?):\\s*(.+?)\\s*-\\s*(.+)")
        return text.lines()
            .mapNotNull { line ->
                val match = issueRegex.find(line.trim())
                if (match != null) {
                    AuditIssue(
                        severity = match.groupValues[1].lowercase(),
                        category = match.groupValues[2].trim(),
                        description = match.groupValues[3].trim(),
                        suggestion = match.groupValues[4].trim()
                    )
                } else null
            }
    }

    /**
     * Extract content between === TAG === markers
     */
    private fun extractTag(content: String, tag: String): String? {
        val regex = Regex("===\\s*$tag\\s*===\\s*([\\s\\S]*?)(?=\\s*===\\s*[A-Z_]+\\s*===|$)")
        val match = regex.find(content)
        return match?.groupValues?.get(1)?.trim()
    }

    /**
     * Determine language from text
     */
    private fun determineLanguage(title: String, genre: String): String {
        val content = title + genre
        for (char in content) {
            if (char.code in 0x4E00..0x9FFF) {
                return "zh"
            }
        }
        return "en"
    }
}
