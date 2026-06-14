package com.example.data.agents

import android.util.Log
import com.example.data.llm.LLMMessage
import com.example.data.llm.ChatCompletionOptions
import com.example.data.models.*

/**
 * Reviser mode - matches TypeScript ReviseMode
 */
enum class ReviseMode {
    AUTO,
    POLISH,
    REWRITE,
    REWORK,
    ANTI_DETECT,
    SPOT_FIX
}

/**
 * Revision output - matches TypeScript ReviseOutput
 */
data class ReviseOutput(
    val revisedContent: String,
    val wordCount: Int,
    val fixedIssues: List<String>,
    val updatedState: String,
    val updatedLedger: String,
    val updatedHooks: String,
    val tokenUsage: TokenUsage? = null
)

/**
 * Audit issue - matches TypeScript AuditIssue
 */
data class AuditIssue(
    val severity: String, // "critical", "warning", "info"
    val category: String,
    val description: String,
    val suggestion: String,
    val repairScope: String? = null // "local", "structural"
)

/**
 * Audit result - matches TypeScript AuditResult
 */
data class AuditResult(
    val passed: Boolean,
    val summary: String,
    val issues: List<AuditIssue>,
    val score: Int = 0
)

/**
 * Reviser Agent - matches TypeScript ReviserAgent
 * Revises chapters based on audit issues
 */
class ReviserAgent(ctx: AgentContext) : BaseAgent(ctx) {
    override val name: String = "reviser"
    
    companion object {
        private const val TAG = "ReviserAgent"
    }

    /**
     * Revise a chapter - matches TypeScript reviseChapter method
     */
    suspend fun reviseChapter(
        book: BookConfig,
        chapterNumber: Int,
        chapterContent: String,
        issues: List<AuditIssue>,
        mode: ReviseMode = ReviseMode.AUTO,
        storyBible: String = "",
        volumeOutline: String = "",
        currentState: String = "",
        ledger: String = "",
        hooks: String = "",
        chapterSummaries: String = "",
        characterMatrix: String = "",
        chapterIntent: String? = null,
        language: String = "zh"
    ): ReviseOutput {
        Log.i(TAG, "Revising chapter $chapterNumber, mode: $mode")
        
        val isEnglish = language == "en"
        val issueList = buildIssueList(issues, isEnglish)
        
        val systemPrompt = buildReviserSystemPrompt(mode, isEnglish)
        val userPrompt = buildReviserUserPrompt(
            chapterNumber = chapterNumber,
            chapterContent = chapterContent,
            issueList = issueList,
            storyBible = storyBible,
            volumeOutline = volumeOutline,
            currentState = currentState,
            ledger = ledger,
            hooks = hooks,
            chapterSummaries = chapterSummaries,
            characterMatrix = characterMatrix,
            chapterIntent = chapterIntent,
            isEnglish = isEnglish
        )
        
        val messages = listOf(
            LLMMessage(role = "system", content = systemPrompt),
            LLMMessage(role = "user", content = userPrompt)
        )
        
        val response = chat(messages, ChatCompletionOptions(temperature = 0.3))
        return parseRevisionOutput(response.content, chapterContent, isEnglish, response.usage)
    }

    /**
     * Build reviser system prompt
     */
    private fun buildReviserSystemPrompt(mode: ReviseMode, isEnglish: Boolean): String {
        val modeDescription = when (mode) {
            ReviseMode.AUTO -> if (isEnglish) "Auto: analyze issues and apply appropriate fixes" else "自动：分析问题并应用适当的修复"
            ReviseMode.POLISH -> if (isEnglish) "Polish: only improve expression, rhythm, and paragraph flow. Do not change facts or plot." else "润色：只改表达、节奏、段落呼吸，不改事实与剧情结论。"
            ReviseMode.REWRITE -> if (isEnglish) "Rewrite: reorganize problem paragraphs, adjust narrative force, but preserve core facts." else "改写：允许重组问题段落、调整叙述力度，但优先保留原文的绝大部分句段。"
            ReviseMode.REWORK -> if (isEnglish) "Rework: restructure scene progression and conflict organization without changing main settings." else "重写：可重构场景推进和冲突组织，但不改主设定和大事件结果。"
            ReviseMode.ANTI_DETECT -> if (isEnglish) "Anti-detect: rewrite to reduce AI detectability while maintaining plot." else "反检测改写：在保持剧情不变的前提下，降低AI生成可检测性。"
            ReviseMode.SPOT_FIX -> if (isEnglish) "Spot-fix: only modify specific sentences/paragraphs flagged in review." else "定点修复：只修改审稿意见指出的具体句子或段落。"
        }
        
        return if (isEnglish) {
            """
            You are a professional web-fiction revision editor. Fix the chapter according to the review notes.
            
            Revision mode: $modeDescription
            
            Output format:
            === FIXED_ISSUES ===
            (List each fix on its own line)
            
            === REVISED_CONTENT ===
            (Full revised chapter content)
            
            === UPDATED_STATE ===
            (Updated state card)
            
            === UPDATED_HOOKS ===
            (Updated hooks board)
            """.trimIndent()
        } else {
            """
            你是一位专业的网络小说修稿编辑。根据审稿意见对章节进行修正。
            
            修稿模式：$modeDescription
            
            输出格式：
            === FIXED_ISSUES ===
            （逐条说明修正了什么，一行一条）
            
            === REVISED_CONTENT ===
            （修正后的完整正文）
            
            === UPDATED_STATE ===
            （更新后的完整状态卡）
            
            === UPDATED_HOOKS ===
            （更新后的完整伏笔池）
            """.trimIndent()
        }
    }

    /**
     * Build reviser user prompt
     */
    private fun buildReviserUserPrompt(
        chapterNumber: Int,
        chapterContent: String,
        issueList: String,
        storyBible: String,
        volumeOutline: String,
        currentState: String,
        ledger: String,
        hooks: String,
        chapterSummaries: String,
        characterMatrix: String,
        chapterIntent: String?,
        isEnglish: Boolean
    ): String {
        val intentBlock = if (!chapterIntent.isNullOrBlank()) {
            if (isEnglish) "\n## Chapter Intent\n$chapterIntent\n" else "\n## 章节意图\n$chapterIntent\n"
        } else ""
        
        return if (isEnglish) {
            """
            Revise chapter $chapterNumber.
            $intentBlock
            ## Audit Issues
            $issueList
            
            ## Current State
            ${currentState.take(7000)}
            
            ## Resource Ledger
            ${ledger.take(6000)}
            
            ## Pending Hooks
            ${hooks.take(9000)}
            
            ## Chapter Summaries
            ${chapterSummaries.take(9000)}
            
            ## Character Matrix
            ${characterMatrix.take(12000)}
            
            ## Volume Outline
            ${volumeOutline.take(8000)}
            
            ## Chapter to Revise
            ${chapterContent.take(30000)}
            
            Fix the issues and output the revised chapter.
            """.trimIndent()
        } else {
            """
            请修正第${chapterNumber}章。
            $intentBlock
            ## 审稿问题
            $issueList
            
            ## 当前状态卡
            ${currentState.take(7000)}
            
            ## 资源账本
            ${ledger.take(6000)}
            
            ## 待回收伏笔
            ${hooks.take(9000)}
            
            ## 章节摘要
            ${chapterSummaries.take(9000)}
            
            ## 角色矩阵
            ${characterMatrix.take(12000)}
            
            ## 卷纲
            ${volumeOutline.take(8000)}
            
            ## 待修正章节
            ${chapterContent.take(30000)}
            
            修正问题并输出修正后的章节。
            """.trimIndent()
        }
    }

    /**
     * Build issue list for prompt
     */
    private fun buildIssueList(issues: List<AuditIssue>, isEnglish: Boolean): String {
        if (issues.isEmpty()) {
            return if (isEnglish) "(No issues found)" else "(未发现问题)"
        }
        
        return issues.joinToString("\n") { issue ->
            "- [${issue.severity}] ${issue.category}: ${issue.description}\n  ${if (isEnglish) "Suggestion" else "建议"}: ${issue.suggestion}"
        }
    }

    /**
     * Parse revision output from LLM response
     */
    private fun parseRevisionOutput(
        content: String,
        originalContent: String,
        isEnglish: Boolean,
        usage: com.example.data.llm.LLMTokenUsage
    ): ReviseOutput {
        val fixedIssues = extractTag(content, "FIXED_ISSUES")
            ?.split("\n")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()
        
        val revisedContent = extractTag(content, "REVISED_CONTENT") ?: originalContent
        val updatedState = extractTag(content, "UPDATED_STATE") ?: 
            if (isEnglish) "(State not updated)" else "(状态卡未更新)"
        val updatedLedger = extractTag(content, "UPDATED_LEDGER") ?: ""
        val updatedHooks = extractTag(content, "UPDATED_HOOKS") ?: 
            if (isEnglish) "(Hooks not updated)" else "(伏笔池未更新)"
        
        return ReviseOutput(
            revisedContent = revisedContent,
            wordCount = revisedContent.length,
            fixedIssues = fixedIssues,
            updatedState = updatedState,
            updatedLedger = updatedLedger,
            updatedHooks = updatedHooks,
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
