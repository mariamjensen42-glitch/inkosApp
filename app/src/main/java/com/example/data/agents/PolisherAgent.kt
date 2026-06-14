package com.example.data.agents

import com.example.data.models.AgentContext
import com.example.data.models.ChapterMemo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PolisherAgent - Polishes chapter content at the prose level.
 *
 * This is the Kotlin Android equivalent of the TypeScript PolisherAgent class.
 * It handles:
 * - Polishing chapter content
 * - Improving sentence craft, paragraph shape, wording
 * - Enhancing five-sense immersion and dialogue naturalness
 * - Preserving plot, character, and mainline structure
 */

data class PolishChapterInput(
    val chapterContent: String,
    val chapterNumber: Int,
    val chapterMemo: ChapterMemo? = null,
    val language: String = "zh",
    val temperature: Double = 0.4
)

data class PolishChapterOutput(
    val polishedContent: String,
    val changed: Boolean,
    val tokenUsage: TokenUsage? = null
)

data class TokenUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/**
 * PolisherAgent - Main class for polishing chapter content.
 */
class PolisherAgent(ctx: AgentContext) : BaseAgent(ctx) {

    override val name: String = "polisher"

    /**
     * Polish chapter content.
     * - Runs AFTER the reviewer+reviser cycle accepts the chapter's structure
     * - ONLY touches prose surface: sentence craft, paragraph shape, wording, punctuation
     * - Forbidden from changing plot, character, or mainline
     * - If structural/plot issue found, marks it in a comment line for next reviewer iteration
     */
    suspend fun polishChapter(input: PolishChapterInput): PolishChapterOutput = withContext(Dispatchers.IO) {
        val language = input.language
        val isEnglish = language == "en"

        val memoBlock = if (input.chapterMemo != null) {
            if (isEnglish) {
                """
                
                ## Chapter Memo (do NOT let polish drift from this goal)
                Goal: ${input.chapterMemo.goal}
                
                ${input.chapterMemo.body}
                """.trimIndent()
            } else {
                """
                
                ## 章节备忘（润色不得偏离此目标）
                goal：${input.chapterMemo.goal}
                
                ${input.chapterMemo.body}
                """.trimIndent()
            }
        } else {
            ""
        }

        val systemPrompt = if (isEnglish) {
            buildEnglishSystemPrompt()
        } else {
            buildChineseSystemPrompt()
        }

        val userPrompt = if (isEnglish) {
            "Polish chapter ${input.chapterNumber}. Return the polished chapter in full, nothing else — no JSON, no headers, no commentary.$memoBlock\n\n## Chapter Under Polish\n${input.chapterContent}"
        } else {
            "请润色第${input.chapterNumber}章。只返回完整的润色后正文，不要 JSON、不要标题、不要解释。$memoBlock\n\n## 待润色章节\n${input.chapterContent}"
        }

        val response = chat(systemPrompt, userPrompt, input.temperature)

        val raw = response.trim()
        // Strip any leading fenced code block wrapper if the model wraps the
        // chapter body defensively.
        val stripped = stripWrappingFence(raw)
        val polishedContent = if (stripped.isNotEmpty()) stripped else input.chapterContent

        PolishChapterOutput(
            polishedContent = polishedContent,
            changed = polishedContent != input.chapterContent,
            tokenUsage = null // Token usage tracking would need to be implemented
        )
    }

    private fun stripWrappingFence(text: String): String {
        val fenceMatch = Regex("""^```[a-zA-Z]*\n([\s\S]*?)\n```\s*$""").find(text)
        return fenceMatch?.groupValues?.get(1)?.trim() ?: text
    }

    private fun buildChineseSystemPrompt(): String {
        return """
你是一位专业中文网文文字层润色编辑。

## 润色边界（硬约束）

你只改文字层——句式 / 段落 / 排版 / 用词 / 五感 / 对话自然度。你禁止增删情节、改变人设、调整主线。发现情节/结构问题只能以 [polisher-note] 形式附在章末供下一轮 reviewer 参考，不能动正文。

结构的事归 Reviewer，不归你。如果读到人设崩、主线偏、冲突缺、memo 未兑现之类的问题，保留原意，不要替作者补情节。

## 6 条文笔类雷点（你要消灭的）

- 描写无效：冗长的环境描写、与主线无关的对话塞满页面。把无效描写删到"一笔带过"。
- 文笔华丽过度：为辞藻堆辞藻，情感失真，形容词地毯轰炸。让文字服从情绪，不要炫技。
- 文笔欠佳：句意含混、指代不清、逻辑跳跃、语言干瘪。重写成通顺、有画面感的句子。
- 排版不规范：段落过长、格式不统一、对话无换行。统一为手机阅读友好格式。
- （延伸）AI 味痕迹：转折词泛滥、"了"字堆砌、"仿佛/宛如/竟然"等情绪中介词、编剧旁白、分析报告式语言。替换成口语化表达或具体动作。
- （延伸）群像脸谱化：不写"众人齐声惊呼"，而是挑 1-2 个角色写具体反应。

## 文字层硬规约

1. 段落不超过 4 行（手机屏）
2. 对话必须独立成段
3. 五感描写（视/听/触/嗅/味）至少出现 2 种
4. 避免"了"字超过 3 次/段
5. 避免"仿佛/宛如/竟然"等情绪中介词
6. 用具体动作代替抽象描述
        """.trimIndent()
    }

    private fun buildEnglishSystemPrompt(): String {
        return """
You are a professional English web fiction prose editor.

## Polish Boundary (Hard Constraints)

You only touch the prose layer — sentence craft, paragraph shape, layout, word choice, five-sense immersion, dialogue naturalness. You are FORBIDDEN from adding/removing plot, changing character, or adjusting the mainline. If you find plot/structure issues, mark them as [polisher-note] at the end of the chapter for the next reviewer iteration — do NOT touch the prose.

Structure is the Reviewer's job, not yours. If you see character inconsistency, mainline drift, conflict gaps, or unfulfilled memos, preserve the original intent — don't try to fix the plot yourself.

## 6 Prose Issues to Eliminate

- Ineffective description: Long environment descriptions, irrelevant dialogue filling the page. Cut to "brief mention."
- Overly ornate prose: Word pile-up, emotional distortion, adjective carpet bombing. Let words serve emotion, not show off.
- Poor writing: Ambiguous meaning, unclear references, logical jumps, dry language. Rewrite into clear, vivid sentences.
- Formatting issues: Overly long paragraphs, inconsistent format, dialogue without line breaks. Standardize for mobile reading.
- AI traces: Excessive transition words, "了" pile-up, emotional mediator words like "仿佛/宛如/竟然", narrator voice, report-style language. Replace with colloquial expression or concrete action.
- Faceless group scenes: Don't write "everyone gasped" — pick 1-2 characters for specific reactions.

## Prose Rules

1. Paragraphs no longer than 4 lines (mobile screen)
2. Dialogue must be separate paragraphs
3. Five-sense descriptions (sight/sound/touch/smell/taste) at least 2 per chapter
4. Avoid "了" more than 3 times per paragraph
5. Avoid emotional mediator words like "仿佛/宛如/竟然"
6. Use concrete actions instead of abstract descriptions
        """.trimIndent()
    }
}
