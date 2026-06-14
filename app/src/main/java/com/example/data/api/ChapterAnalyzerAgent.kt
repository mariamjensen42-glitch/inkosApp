package com.example.data.api

import android.util.Log
import com.example.data.local.BookEntity

data class AnalyzeChapterOutput(
    val title: String,
    val content: String,
    val wordCount: Int,
    val updatedState: String,
    val updatedLedger: String,
    val updatedHooks: String,
    val chapterSummary: String,
    val updatedSubplots: String,
    val updatedEmotionalArcs: String,
    val updatedCharacterMatrix: String
)

object ChapterAnalyzerAgent {
    private const val TAG = "ChapterAnalyzerAgent"

    private fun determineLanguage(bookTitle: String, brief: String): String {
        val content = bookTitle + brief
        for (char in content) {
            if (char.code in 0x4E00..0x9FFF) {
                return "zh"
            }
        }
        return "en"
    }

    private fun updateSectionInStoryBible(storyBible: String, sectionHeader: String, newContent: String): String {
        if (newContent.trim().isEmpty()) return storyBible
        
        val lines = storyBible.split("\n")
        val result = StringBuilder()
        var foundSection = false
        var skipMode = false
        
        for (i in lines.indices) {
            val line = lines[i]
            if (line.trim().startsWith("#")) {
                val normalized = line.lowercase().replace(Regex("[^a-z0-9\\u4e00-\\u9fa5]"), "")
                val headingKey = sectionHeader.lowercase().replace(Regex("[^a-z0-9\\u4e00-\\u9fa5]"), "")
                if (normalized.contains(headingKey)) {
                    foundSection = true
                    // Output the header
                    result.append(line).append("\n")
                    // Output the new content
                    result.append(newContent).append("\n\n")
                    skipMode = true
                    continue
                } else {
                    skipMode = false
                }
            }
            if (!skipMode) {
                result.append(line).append("\n")
            }
        }
        
        // If the section didn't exist yet, simply append it at the end
        if (!foundSection) {
            result.append("\n\n# ").append(sectionHeader).append("\n").append(newContent).append("\n")
        }
        
        return result.toString().trim()
    }

    fun updateStoryBibleWithAnalysis(storyBible: String, analysis: AnalyzeChapterOutput): String {
        var updated = storyBible
        
        updated = updateSectionInStoryBible(updated, "📜 Current States", analysis.updatedState)
        if (analysis.updatedLedger.isNotBlank()) {
            updated = updateSectionInStoryBible(updated, "📊 Resource Ledger", analysis.updatedLedger)
        }
        updated = updateSectionInStoryBible(updated, "🪝 Active Hooks", analysis.updatedHooks)
        
        // Accumulate/Append Chapter Summary instead of simple replacement
        val existingSummaries = extractSection(storyBible, listOf("chaptersummary", "chaptersummaries", "章节摘要", "已有章节摘要", "摘要"))
        val appendedSummary = if (existingSummaries.trim().isEmpty() || existingSummaries.contains("not created") || existingSummaries.contains("尚未创建")) {
            // First summary
            "| 章节 | 标题 | 出场人物 | 关键事件 | 状态变化 | 伏笔动态 | 情绪基调 | 章节类型 |\n| --- | --- | --- | --- | --- | --- | --- | --- |\n" + analysis.chapterSummary
        } else {
            existingSummaries + "\n" + analysis.chapterSummary
        }
        updated = updateSectionInStoryBible(updated, "📝 Chapter Summaries", appendedSummary)
        
        updated = updateSectionInStoryBible(updated, "📉 Subplot Board", analysis.updatedSubplots)
        updated = updateSectionInStoryBible(updated, "📈 Emotional Arcs", analysis.updatedEmotionalArcs)
        updated = updateSectionInStoryBible(updated, "🖨️ Character Matrix", analysis.updatedCharacterMatrix)
        
        return updated
    }

    private fun extractSection(text: String, headingKeywords: List<String>): String {
        val lines = text.split("\n")
        var foundStart = false
        var startIndex = -1
        
        for (i in lines.indices) {
            val line = lines[i].trim()
            if (line.startsWith("#")) {
                val normalized = line.lowercase().replace(Regex("[^a-z0-9\\u4e00-\\u9fa5]"), "")
                val matches = headingKeywords.any { keyword -> normalized.contains(keyword.lowercase()) }
                if (matches) {
                    foundStart = true
                    startIndex = i
                    break
                }
            }
        }
        
        if (!foundStart || startIndex == -1) return ""
        
        val result = StringBuilder()
        for (i in (startIndex + 1) until lines.size) {
            val line = lines[i]
            if (line.trim().startsWith("#")) {
                break
            }
            result.append(line).append("\n")
        }
        
        return result.toString().trim()
    }

    private fun getPlaceholder(isEnglish: Boolean): String {
        return if (isEnglish) "(file not created yet)" else "(文件尚未创建)"
    }

    suspend fun analyzeChapter(
        book: BookEntity,
        chapterIndex: Int,
        chapterContent: String,
        chapterTitle: String? = null,
        chapterIntent: String? = null
    ): AnalyzeChapterOutput {
        val lang = determineLanguage(book.title, book.brief)
        val isEnglish = lang == "en"
        val placeholder = getPlaceholder(isEnglish)

        // Extract pieces from dynamic book storyBible. Falls back gracefully to default templates.
        val storyBible = book.storyBible
        
        val parsedBookRules = book.bookRules
        
        val currentStateRaw = extractSection(storyBible, listOf("currentstate", "状态卡", "当前状态", "state"))
        val currentState = if (currentStateRaw.isNotEmpty()) currentStateRaw else {
            if (isEnglish) {
                """
                | Field | Value |
                | --- | --- |
                | Current Chapter | $chapterIndex |
                | Current Location | TBD |
                | Protagonist State | In Initial state |
                | Current Goal | TBD |
                | Current Constraint | None |
                | Current Alliances | None |
                | Current Conflict | None |
                """.trimIndent()
            } else {
                """
                | 字段 | 值 |
                |------|-----|
                | 当前章节 | $chapterIndex |
                | 当前位置 | 尚未确定 |
                | 主角状态 | 处于初始阶段 |
                | 当前目标 | 暂无 |
                | 当前限制 | 暂无 |
                | 当前敌我 | 暂无 |
                | 当前冲突 | 暂无 |
                """.trimIndent()
            }
        }

        val ledgerRaw = extractSection(storyBible, listOf("ledger", "账本", "资源账本", "updatedledger"))
        val ledger = if (ledgerRaw.isNotEmpty()) ledgerRaw else {
            if (isEnglish) {
                "| Asset | Quantity | Limit | Description |\n| --- | --- | --- | --- |"
            } else {
                "| 资源类型 | 当前数量 | 硬上限 | 备注/单位 |"
            }
        }

        val hooksRaw = extractSection(storyBible, listOf("hooks", "伏笔", "待回收伏笔", "pendinghooks", "updatedhooks"))
        val hooks = if (hooksRaw.isNotEmpty()) hooksRaw else {
            if (isEnglish) {
                "| hook_id | start_chapter | type | status | last_advanced_chapter | expected_payoff | payoff_timing | notes |"
            } else {
                "| hook_id | 起始章节 | 类型 | 状态 | 最近推进 | 预期回收 | 回收节奏 | 备注 |"
            }
        }

        val chapterSummariesRaw = extractSection(storyBible, listOf("chaptersummary", "chaptersummaries", "章节摘要", "已有章节摘要", "摘要"))
        val chapterSummaries = if (chapterSummariesRaw.isNotEmpty()) chapterSummariesRaw else placeholder

        val subplotBoardRaw = extractSection(storyBible, listOf("subplot", "支线", "支线进度"))
        val subplotBoard = if (subplotBoardRaw.isNotEmpty()) subplotBoardRaw else {
            if (isEnglish) {
                "| Subplot ID | Connection | Current Progress | Key Characters | Next Steps | Status |"
            } else {
                "| 支线ID | 主线关联 | 当前进度 | 关键涉事角色 | 下一步动向 | 状态 |"
            }
        }

        val emotionalArcsRaw = extractSection(storyBible, listOf("emotional", "情感弧线", "情绪状态"))
        val emotionalArcs = if (emotionalArcsRaw.isNotEmpty()) emotionalArcsRaw else {
            if (isEnglish) {
                "| Character | Chapter | Emotional State | Trigger Event | Intensity | Arc Direction |"
            } else {
                "| 角色 | 章节 | 情绪状态 | 触发事件 | 强度(1-10) | 弧线方向 |"
            }
        }

        val characterMatrixRaw = extractSection(storyBible, listOf("charactermatrix", "角色交互矩阵", "角色矩阵", "roles"))
        val characterMatrix = if (characterMatrixRaw.isNotEmpty()) characterMatrixRaw else placeholder

        val storyFrameRaw = extractSection(storyBible, listOf("storybible", "故事圣经", "设定", "storyframe", "故事框架"))
        val storyFrame = if (storyFrameRaw.isNotEmpty()) storyFrameRaw else storyBible

        val volumeOutlineRaw = extractSection(storyBible, listOf("volumeoutline", "卷纲", "分卷大纲", "volumemap", "分卷地图"))
        val volumeOutline = if (volumeOutlineRaw.isNotEmpty()) volumeOutlineRaw else placeholder

        val systemPrompt = buildSystemPrompt(book, parsedBookRules, lang)
        val userPrompt = buildUserPrompt(
            lang = lang,
            chapterNumber = chapterIndex,
            chapterContent = chapterContent,
            chapterTitle = chapterTitle,
            currentState = currentState,
            ledger = ledger,
            hooks = hooks,
            chapterSummaries = chapterSummaries,
            subplotBoard = subplotBoard,
            emotionalArcs = emotionalArcs,
            characterMatrix = characterMatrix,
            storyFrame = storyFrame,
            volumeOutline = volumeOutline,
            chapterIntent = chapterIntent
        )

        Log.i(TAG, "Requesting chapter analysis from AI API for Chapter $chapterIndex")
        val response = LlmRouter.generateContent(systemPrompt, userPrompt, requireJson = false)
        
        return parseResponse(chapterIndex, response, chapterContent, chapterTitle, isEnglish)
    }

    private fun buildSystemPrompt(
        book: BookEntity,
        bookRules: String,
        lang: String
    ): String {
        val isEnglish = lang == "en"
        val numericalBlock = if (book.genre.lowercase().contains("xuanhuan") || book.genre.lowercase().contains("system") || book.genre.lowercase().contains("sci-fi")) {
            if (isEnglish) {
                "\n- This genre tracks numerical/resources systems; UPDATED_LEDGER must capture every resource change shown in the chapter."
            } else {
                "\n- 本题材有数值/资源体系，你必须在 UPDATED_LEDGER 中追踪正文中出现的所有资源变动。"
            }
        } else {
            if (isEnglish) {
                "\n- This genre has no numerical system; leave UPDATED_LEDGER empty."
            } else {
                "\n- 本题材无数值系统，UPDATED_LEDGER 留空。"
            }
        }

        if (isEnglish) {
            return """
            You are a fiction continuity analyst. Analyze a finished chapter, extract every state change, and update the tracking files.
            
            ## Working Mode
            You are not writing new prose. You are reading completed chapter text and updating the book's truth files.
            1. Read the chapter carefully and extract all important facts.
            2. Update the existing tracking files incrementally rather than rebuilding them from scratch.
            3. Keep the output contract identical to the writer pipeline.
            
            ## What To Extract
            - Character entrances, exits, injuries, breakthroughs, deaths, and other status changes
            - Location movement and scene transitions
            - Item or resource gains and losses
            - Hook setup, advancement, and payoff
            - Emotional arc movement
            - Subplot progress
            - Relationship changes and information-boundary changes
            
            ## Book Information
            - Title: ${book.title}
            - Genre: ${book.genre}
            - Platform: ${book.status}
            $numericalBlock
            
            ${if (bookRules.isNotEmpty()) "## Book Rules\n\n$bookRules" else ""}
            
            ## Output Format
            Use === TAG === delimiters exactly as shown:
            
            === CHAPTER_TITLE ===
            (Extract or infer the chapter title. Output title text only.)
            
            === CHAPTER_CONTENT ===
            (Repeat original chapter content exactly. Do not rewrite.)
            
            === PRE_WRITE_CHECK ===
            (Leave empty in analysis mode.)
            
            === POST_SETTLEMENT ===
            (Leave empty in analysis mode.)
            
            === UPDATED_STATE ===
            Updated state card as a Markdown table reflecting the end-of-chapter state:
            | Field | Value |
            | --- | --- |
            | Current Chapter | {chapter_index} |
            | Current Location | ... |
            | Protagonist State | ... |
            | Current Goal | ... |
            | Current Constraint | ... |
            | Current Alliances | ... |
            | Current Conflict | ... |
            
            === UPDATED_LEDGER ===
            (If the genre has a numerical system: output the fully updated resource ledger table. Otherwise leave empty.)
            
            === UPDATED_HOOKS ===
            Updated hooks pool as a Markdown table with the latest status of every known hook:
            | hook_id | start_chapter | type | status | last_advanced_chapter | expected_payoff | payoff_timing | notes |
            
            === CHAPTER_SUMMARY ===
            Single Markdown table row:
            | Chapter | Title | Characters | Key Events | State Changes | Hook Activity | Mood | Chapter Type |
            
            === UPDATED_SUBPLOTS ===
            Updated subplot board (Markdown table)
            
            === UPDATED_EMOTIONAL_ARCS ===
            Updated emotional arcs (Markdown table)
            
            === UPDATED_CHARACTER_MATRIX ===
            Updated character matrix (one ## section per character, bullet-list fields):
            
            ## Character Name
            - **Role**: protagonist / antagonist / ally / minor / mentioned
            - **Tags**: core identity tags
            - **Contrast**: distinctive details that defy expectations
            - **Speech**: speaking style summary
            - **Personality**: core personality traits
            - **Motivation**: fundamental driving force
            - **Current**: immediate goal this chapter
            - **Relationships**: OtherChar(type/Ch#) | ...
            - **Known**: what this character knows (only witnessed or told)
            - **Unknown**: what this character does not know
            
            ## Rules
            1. UPDATED_STATE and UPDATED_HOOKS must be incremental updates based on the current tracking files.
            2. Every factual change in the chapter must appear in the corresponding tracking file.
            3. Do not miss resource changes, movement, relationship changes, or information changes.
            4. Information boundaries in the character matrix must stay exact.
            """.trimIndent()
        }

        return """
        你是小说连续性分析师。你的任务是分析一章已完成的小说正文，从中提取所有状态变化并更新追踪文件。

        ## 工作模式
        你不是在写作，而是在分析已有正文。你需要：
        1. 仔细阅读正文，提取所有关键信息
        2. 基于"当前追踪文件"做增量更新
        3. 输出格式与写作模块完全一致

        ## 分析维度
        从正文中提取以下信息：
        - 角色出场、退场、状态变化（受伤/突破/死亡等）
        - 位置移动、场景转换
        - 物品/资源的获得与消耗
        - 伏笔的埋设、推进、回收
        - 情感弧线变化
        - 支线进展
        - 角色间关系变化、新的信息边界

        ## 书籍信息
        - 标题：${book.title}
        - 题材：${book.genre}
        $numericalBlock

        ${if (bookRules.isNotEmpty()) "## 本书规则\n\n$bookRules" else ""}

        ## 输出格式（必须严格遵循）
        使用 === TAG === 分隔各部分，与写作模块完全一致：

        === CHAPTER_TITLE ===
        （从正文标题行提取或推断章节标题，只输出标题文字）

        === CHAPTER_CONTENT ===
        （原样输出正文内容，不做任何修改）

        === PRE_WRITE_CHECK ===
        （留空，分析模式不需要写作自检）

        === POST_SETTLEMENT ===
        （留空，分析模式不需要写后结算）

        === UPDATED_STATE ===
        更新后的状态卡（Markdown表格），反映本章结束时的最新状态：
        | 字段 | 值 |
        |------|-----|
        | 当前章节 | {章节号} |
        | 当前位置 | ... |
        | 主角状态 | ... |
        | 当前目标 | ... |
        | 当前限制 | ... |
        | 当前敌我 | ... |
        | 当前冲突 | ... |

        === UPDATED_LEDGER ===
        （如有数值系统：更新后的完整资源账本表格；无则留空）

        === UPDATED_HOOKS ===
        更新后的伏笔池（Markdown表格），包含所有已知伏笔的最新状态：
        | hook_id | 起始章节 | 类型 | 状态 | 最近推进 | 预期回收 | 回收节奏 | 备注 |

        === CHAPTER_SUMMARY ===
        本章摘要（Markdown表格行）：
        | 章节 | 标题 | 出场人物 | 关键事件 | 状态变化 | 伏笔动态 | 情绪基调 | 章节类型 |

        === UPDATED_SUBPLOTS ===
        更新后的支线进度板（Markdown表格）

        === UPDATED_EMOTIONAL_ARCS ===
        更新后的情感弧线（Markdown表格）

        === UPDATED_CHARACTER_MATRIX ===
        更新后的角色矩阵（每个角色一个 ## 块，字段用 bullet list）：

        ## 角色名
        - **定位**: 主角 / 反派 / 盟友 / 配角 / 提及
        - **标签**: 核心身份标签
        - **反差**: 打破刻板印象的独特细节
        - **说话**: 说话风格概述
        - **性格**: 性格底色
        - **动机**: 根本驱动力
        - **当前**: 本章即时目标
        - **关系**: 某角色(关系性质/Ch#) | ...
        - **已知**: 该角色已知的信息（仅限亲历或被告知）
        - **未知**: 该角色不知道的信息

        ## 关键规则
        1. 状态卡和伏笔池必须基于"当前追踪文件"做增量更新，不是从零开始
        2. 正文中的每一个事实性变化都必须反映在对应的追踪文件中
        3. 不要遗漏细节：数值变化、位置变化、关系变化、信息变化都要记录
        4. 角色矩阵中的"已知/未知"要准确——角色只知道他在场时发生的事
        """.trimIndent()
    }

    private fun buildUserPrompt(
        lang: String,
        chapterNumber: Int,
        chapterContent: String,
        chapterTitle: String?,
        currentState: String,
        ledger: String,
        hooks: String,
        chapterSummaries: String,
        subplotBoard: String,
        emotionalArcs: String,
        characterMatrix: String,
        storyFrame: String,
        volumeOutline: String,
        chapterIntent: String?
    ): String {
        val isEnglish = lang == "en"
        val titleLine = if (!chapterTitle.isNullOrBlank()) {
            if (isEnglish) "Chapter Title: $chapterTitle\n" else "章节标题：$chapterTitle\n"
        } else ""

        val intentBlock = if (!chapterIntent.isNullOrBlank()) {
            if (isEnglish) "\n## Planned Chapter Intent\n$chapterIntent\n" else "\n## 本章规划意图\n$chapterIntent\n"
        } else ""

        val ledgerBlock = if (ledger.isNotEmpty()) {
            if (isEnglish) "\n## Current Resource Ledger\n$ledger\n" else "\n## 当前资源账本\n$ledger\n"
        } else ""

        if (isEnglish) {
            return """
            Analyze chapter $chapterNumber and update all tracking files.
            $titleLine
            $intentBlock
            
            ## Chapter Content
            $chapterContent
            
            ## Current State
            $currentState
            $ledgerBlock
            
            ## Current Hooks
            $hooks
            
            ## Recent Chapter Summaries
            $chapterSummaries
            
            ## Current Subplot Board
            $subplotBoard
            
            ## Current Emotional Arcs
            $emotionalArcs
            
            ## Current Character Matrix
            $characterMatrix
            
            ## Story Frame
            $storyFrame
            
            ## Volume Map / Outline
            $volumeOutline
            
            Please return the result strictly in the === TAG === format.
            """.trimIndent()
        }

        return """
        请分析第${chapterNumber}章正文，更新所有追踪文件。
        $titleLine
        $intentBlock

        ## 正文内容
        $chapterContent

        ## 当前状态卡
        $currentState
        $ledgerBlock

        ## 当前伏笔池
        $hooks

        ## 历史章节摘要
        $chapterSummaries

        ## 当前支线进度板
        $subplotBoard

        ## 当前情感弧线
        $emotionalArcs

        ## 当前角色交互矩阵
        $characterMatrix

        ## 故事世界框架
        $storyFrame

        ## 分卷大纲地图
        $volumeOutline

        请严格按照 === TAG === 格式输出分析结果。
        """.trimIndent()
    }

    private fun parseResponse(
        chapterNumber: Int,
        responseContent: String,
        originalContent: String,
        originalTitle: String?,
        isEnglish: Boolean
    ): AnalyzeChapterOutput {
        val tags = listOf(
            "CHAPTER_TITLE",
            "CHAPTER_CONTENT",
            "PRE_WRITE_CHECK",
            "POST_SETTLEMENT",
            "UPDATED_STATE",
            "UPDATED_LEDGER",
            "UPDATED_HOOKS",
            "CHAPTER_SUMMARY",
            "UPDATED_SUBPLOTS",
            "UPDATED_EMOTIONAL_ARCS",
            "UPDATED_CHARACTER_MATRIX"
        )

        val values = mutableMapOf<String, String>()
        
        // Dynamic extraction based on tags
        for (i in tags.indices) {
            val tag = tags[i]
            val regex = Regex("(?s)===\\s*$tag\\s*===\\s*(.*?)(?=\\s*===\\s*|$)")
            val match = regex.find(responseContent)
            if (match != null) {
                values[tag] = match.groupValues[1].trim()
            } else {
                values[tag] = ""
            }
        }

        val extractedTitle = values["CHAPTER_TITLE"] ?: ""
        val title = if (extractedTitle.isNotBlank()) extractedTitle else {
            originalTitle ?: (if (isEnglish) "Chapter $chapterNumber" else "第${chapterNumber}章")
        }

        val parsedContent = values["CHAPTER_CONTENT"] ?: ""
        val content = if (parsedContent.isNotBlank()) parsedContent else originalContent

        return AnalyzeChapterOutput(
            title = title,
            content = content,
            wordCount = content.length,
            updatedState = values["UPDATED_STATE"] ?: "",
            updatedLedger = values["UPDATED_LEDGER"] ?: "",
            updatedHooks = values["UPDATED_HOOKS"] ?: "",
            chapterSummary = values["CHAPTER_SUMMARY"] ?: "",
            updatedSubplots = values["UPDATED_SUBPLOTS"] ?: "",
            updatedEmotionalArcs = values["UPDATED_EMOTIONAL_ARCS"] ?: "",
            updatedCharacterMatrix = values["UPDATED_CHARACTER_MATRIX"] ?: ""
        )
    }
}
