package com.example.data.agents

import android.util.Log
import com.example.data.llm.LLMMessage
import com.example.data.models.ChapterIntent
import com.example.data.models.ChapterMemo
import com.example.data.models.LengthSpec
import com.example.data.models.TokenUsage

/**
 * Planner agent output
 */
data class PlannerOutput(
    val intent: ChapterIntent,
    val memo: ChapterMemo,
    val intentMarkdown: String,
    val plannerInputs: List<String>
)

/**
 * Planner Agent - matches TypeScript PlannerAgent
 * Generates chapter intent and memo for the writer
 */
class PlannerAgent(ctx: AgentContext) : BaseAgent(ctx) {
    override val name: String = "planner"
    
    companion object {
        private const val TAG = "PlannerAgent"
        private const val MEMO_RETRY_LIMIT = 3
    }

    /**
     * Plan a chapter - matches TypeScript planChapter method
     */
    suspend fun planChapter(
        bookId: String,
        chapterNumber: Int,
        storyBible: String,
        volumeOutline: String,
        chapterSummaries: String,
        hooks: String,
        characterMatrix: String,
        externalContext: String? = null,
        language: String = "zh"
    ): PlannerOutput {
        Log.i(TAG, "Planning chapter $chapterNumber for book $bookId")
        
        // Determine if this is a golden opening chapter
        val isGoldenOpening = if (language == "en") chapterNumber <= 5 else chapterNumber <= 3
        
        // Build goal from context
        val goal = deriveGoal(externalContext, chapterNumber, language)
        
        // Build must-keep and must-avoid lists
        val mustKeep = extractMustKeep(storyBible, chapterSummaries)
        val mustAvoid = extractMustAvoid(hooks)
        
        // Build chapter memo
        val memo = planChapterMemo(
            chapterNumber = chapterNumber,
            goal = goal,
            storyBible = storyBible,
            volumeOutline = volumeOutline,
            chapterSummaries = chapterSummaries,
            hooks = hooks,
            characterMatrix = characterMatrix,
            externalContext = externalContext,
            isGoldenOpening = isGoldenOpening,
            language = language
        )
        
        // Build chapter intent
        val intent = ChapterIntent(
            chapter = chapterNumber,
            goal = memo.goal,
            mustKeep = mustKeep,
            mustAvoid = mustAvoid
        )
        
        // Render intent markdown
        val intentMarkdown = renderIntentMarkdown(intent, memo, language, hooks, chapterSummaries)
        
        return PlannerOutput(
            intent = intent,
            memo = memo,
            intentMarkdown = intentMarkdown,
            plannerInputs = listOf("story_bible", "volume_outline", "chapter_summaries", "hooks", "character_matrix")
        )
    }

    /**
     * Plan chapter memo via LLM
     */
    private suspend fun planChapterMemo(
        chapterNumber: Int,
        goal: String,
        storyBible: String,
        volumeOutline: String,
        chapterSummaries: String,
        hooks: String,
        characterMatrix: String,
        externalContext: String?,
        isGoldenOpening: Boolean,
        language: String
    ): ChapterMemo {
        val systemPrompt = buildPlannerSystemPrompt(language)
        val userPrompt = buildPlannerUserPrompt(
            chapterNumber = chapterNumber,
            goal = goal,
            storyBible = storyBible,
            volumeOutline = volumeOutline,
            chapterSummaries = chapterSummaries,
            hooks = hooks,
            characterMatrix = characterMatrix,
            externalContext = externalContext,
            isGoldenOpening = isGoldenOpening,
            language = language
        )
        
        val messages = listOf(
            LLMMessage(role = "system", content = systemPrompt),
            LLMMessage(role = "user", content = userPrompt)
        )
        
        // Retry loop for memo parsing
        var lastError: Exception? = null
        for (attempt in 0 until MEMO_RETRY_LIMIT) {
            try {
                val response = chat(messages, com.example.data.llm.ChatCompletionOptions(temperature = 0.7))
                return parseMemo(response.content, chapterNumber, isGoldenOpening)
            } catch (e: Exception) {
                lastError = e
                Log.w(TAG, "Memo parse failed (attempt ${attempt + 1}/$MEMO_RETRY_LIMIT): ${e.message}")
            }
        }
        
        // Fallback memo
        Log.w(TAG, "Planner fell back after $MEMO_RETRY_LIMIT attempts: ${lastError?.message}")
        return buildFallbackMemo(chapterNumber, goal, isGoldenOpening, language)
    }

    /**
     * Build planner system prompt
     */
    private fun buildPlannerSystemPrompt(language: String): String {
        return if (language == "en") {
            """
            You are a chapter planner for web fiction. Your job is to create a detailed chapter memo that guides the writer.
            
            Output a structured memo with these sections:
            1. Chapter Goal (1-2 sentences, specific and actionable)
            2. Thread References (list of hook IDs this chapter should advance)
            3. Current Task (what the writer needs to accomplish)
            4. Reader Expectations (what the reader is waiting for)
            5. Payoffs to Deliver / Secrets to Keep
            6. Transitional Beats Purpose
            7. Key Choice Check (3 questions about the protagonist's main choice)
            8. Required End-of-Chapter Change
            9. Hook Ledger (which hooks to advance/resolve/defer)
            10. Do Not (list of things to avoid)
            
            Be specific and actionable. Avoid generic advice.
            """.trimIndent()
        } else {
            """
            你是网络小说章节规划师。你的任务是创建详细的章节备忘录来指导写手。
            
            输出结构化的备忘录，包含以下部分：
            1. 本章目标（1-2句话，具体可执行）
            2. 关联线索（列出本章应推进的伏笔ID）
            3. 当前任务（写手需要完成什么）
            4. 读者期待（读者在等什么）
            5. 该兑现的 / 暂不掀的
            6. 过渡节拍的作用
            7. 关键抉择三连问（关于主角主要选择的三个问题）
            8. 章尾必须发生的改变
            9. 本章伏笔账（哪些伏笔推进/回收/推迟）
            10. 不要做（需要避免的事情列表）
            
            要具体可执行，避免泛泛建议。
            """.trimIndent()
        }
    }

    /**
     * Build planner user prompt
     */
    private fun buildPlannerUserPrompt(
        chapterNumber: Int,
        goal: String,
        storyBible: String,
        volumeOutline: String,
        chapterSummaries: String,
        hooks: String,
        characterMatrix: String,
        externalContext: String?,
        isGoldenOpening: Boolean,
        language: String
    ): String {
        val contextBlock = if (!externalContext.isNullOrBlank()) {
            if (language == "en") "\n## External Context\n$externalContext\n" else "\n## 外部指令\n$externalContext\n"
        } else ""
        
        val goldenOpeningBlock = if (isGoldenOpening) {
            if (language == "en") {
                "\n## Golden Opening\nThis is one of the opening chapters. Make it hook the reader immediately with vivid scenes and compelling conflict.\n"
            } else {
                "\n## 黄金开头\n这是开篇章节之一。用生动的场景和引人入胜的冲突立即抓住读者。\n"
            }
        } else ""
        
        return if (language == "en") {
            """
            Plan chapter $chapterNumber.
            $contextBlock$goldenOpeningBlock
            ## Story Bible
            ${storyBible.take(8000)}
            
            ## Volume Outline
            ${volumeOutline.take(4000)}
            
            ## Chapter Summaries
            ${chapterSummaries.take(4000)}
            
            ## Pending Hooks
            ${hooks.take(4000)}
            
            ## Character Matrix
            ${characterMatrix.take(4000)}
            
            ## Chapter Goal
            $goal
            
            Output the chapter memo in the structured format.
            """.trimIndent()
        } else {
            """
            规划第${chapterNumber}章。
            $contextBlock$goldenOpeningBlock
            ## 故事圣经
            ${storyBible.take(8000)}
            
            ## 卷纲
            ${volumeOutline.take(4000)}
            
            ## 章节摘要
            ${chapterSummaries.take(4000)}
            
            ## 待回收伏笔
            ${hooks.take(4000)}
            
            ## 角色矩阵
            ${characterMatrix.take(4000)}
            
            ## 章节目标
            $goal
            
            按结构化格式输出章节备忘录。
            """.trimIndent()
        }
    }

    /**
     * Parse memo from LLM response
     */
    private fun parseMemo(content: String, chapterNumber: Int, isGoldenOpening: Boolean): ChapterMemo {
        val goal = extractSection(content, listOf("chapter goal", "本章目标")) ?: "推进第${chapterNumber}章"
        val body = content
        val threadRefs = extractListItems(
            extractSection(content, listOf("thread refs", "关联线索")) ?: "",
            10
        )
        
        return ChapterMemo(
            chapter = chapterNumber,
            goal = goal.take(50),
            isGoldenOpening = isGoldenOpening,
            body = body,
            threadRefs = threadRefs
        )
    }

    /**
     * Build fallback memo when LLM fails
     */
    private fun buildFallbackMemo(
        chapterNumber: Int,
        goal: String,
        isGoldenOpening: Boolean,
        language: String
    ): ChapterMemo {
        val body = if (language == "en") {
            """
            # Chapter $chapterNumber memo
            
            ## Chapter goal
            $goal
            
            ## Thread refs
            none
            
            ## Current task
            Continue chapter $chapterNumber with clear narrative focus.
            
            ## Reader expectations
            Keep the reader engaged with the current story arc.
            
            ## Payoffs to deliver
            Only deliver what is supported by context.
            
            ## Transitional beats
            Use transitions to build tension or provide necessary information.
            
            ## Key choice check
            The protagonist's choice must have reason, match current interest, and stay consistent.
            
            ## Required end-of-chapter change
            End with a concrete change in information, pressure, or relationship.
            
            ## Hook ledger
            Advance active hooks; resolve only what has evidence; defer larger threads.
            
            ## Do not
            Do not contradict established facts or ignore user instructions.
            
            ## Planner warning
            Generated with fallback due to LLM parse failure.
            """.trimIndent()
        } else {
            """
            # 第${chapterNumber}章 memo
            
            ## 本章目标
            $goal
            
            ## 关联线索
            无
            
            ## 当前任务
            沿用当前章节目标推进第${chapterNumber}章。
            
            ## 读者期待
            延续当前故事弧线，保持读者投入。
            
            ## 该兑现的
            只兑现已有上下文支撑的承诺。
            
            ## 过渡节拍
            用过渡建立张力或提供必要信息。
            
            ## 关键抉择三连问
            主角的选择必须有原因、符合当前利益、不背离人设。
            
            ## 章尾必须发生的改变
            章尾至少在信息、压力或关系上发生一个明确变化。
            
            ## 本章伏笔账
            推进活跃伏笔；只结清有证据的线索；大线继续保留。
            
            ## 不要做
            不要违背既成事实，不要无视用户指令。
            
            ## Planner warning
            因LLM解析失败使用fallback生成。
            """.trimIndent()
        }
        
        return ChapterMemo(
            chapter = chapterNumber,
            goal = goal.take(50),
            isGoldenOpening = isGoldenOpening,
            body = body,
            threadRefs = emptyList()
        )
    }

    /**
     * Derive goal from context
     */
    private fun deriveGoal(externalContext: String?, chapterNumber: Int, language: String): String {
        if (!externalContext.isNullOrBlank()) {
            val firstLine = externalContext.lines().firstOrNull { it.isNotBlank() && !it.startsWith("#") }
            if (!firstLine.isNullOrBlank()) return firstLine.take(100)
        }
        return if (language == "en") "Advance chapter $chapterNumber with clear narrative focus."
        else "推进第${chapterNumber}章，保持清晰的叙事焦点。"
    }

    /**
     * Extract must-keep items
     */
    private fun extractMustKeep(storyBible: String, chapterSummaries: String): List<String> {
        val items = mutableListOf<String>()
        items.addAll(extractListItems(storyBible, 2))
        items.addAll(extractListItems(chapterSummaries, 2))
        return items.distinct().take(4)
    }

    /**
     * Extract must-avoid items
     */
    private fun extractMustAvoid(hooks: String): List<String> {
        return extractListItems(hooks, 3)
    }

    /**
     * Render intent markdown
     */
    private fun renderIntentMarkdown(
        intent: ChapterIntent,
        memo: ChapterMemo,
        language: String,
        hooks: String,
        chapterSummaries: String
    ): String {
        val mustKeep = intent.mustKeep.joinToString("\n") { "- $it" }.ifEmpty { "- none" }
        val mustAvoid = intent.mustAvoid.joinToString("\n") { "- $it" }.ifEmpty { "- none" }
        val threadRefs = memo.threadRefs.joinToString("\n") { "- $it" }.ifEmpty { "- (none)" }
        
        return if (language == "en") {
            """
            # Chapter Intent
            
            ## Goal
            ${intent.goal}
            
            ## Must Keep
            $mustKeep
            
            ## Must Avoid
            $mustAvoid
            
            ## Chapter Memo
            
            ### Thread Refs
            $threadRefs
            
            ### Body
            ${memo.body}
            
            ## Pending Hooks Snapshot
            ${hooks.take(2000)}
            
            ## Chapter Summaries Snapshot
            ${chapterSummaries.take(2000)}
            """.trimIndent()
        } else {
            """
            # 章节意图
            
            ## 目标
            ${intent.goal}
            
            ## 必须保留
            $mustKeep
            
            ## 必须避免
            $mustAvoid
            
            ## 章节备忘录
            
            ### 关联线索
            $threadRefs
            
            ### 正文
            ${memo.body}
            
            ## 伏笔快照
            ${hooks.take(2000)}
            
            ## 章节摘要快照
            ${chapterSummaries.take(2000)}
            """.trimIndent()
        }
    }
}
