package com.example.data.api

import android.util.Log

data class ArchitectRole(
    val tier: String, // "major" | "minor"
    val name: String,
    val content: String
)

data class ArchitectOutput(
    val storyBible: String,
    val bookRules: String,
    val storyFrame: String = "",
    val volumeMap: String = "",
    val rhythmPrinciples: String = "",
    val roles: List<ArchitectRole> = emptyList(),
    val pendingHooks: String = ""
)

class ArchitectIncompleteException(
    val missing: List<String>,
    val partialContent: String,
    message: String
) : Exception(message)

object ArchitectAgent {
    private const val TAG = "ArchitectAgent"

    private fun determineLanguage(title: String, brief: String): String {
        val content = title + brief
        for (char in content) {
            if (char.code in 0x4E00..0x9FFF) {
                return "zh"
            }
        }
        return "en"
    }

    private fun buildChineseFoundationPrompt(
        title: String,
        genre: String,
        targetChapters: Int,
        chapterWordCount: Int,
        brief: String,
        externalContext: String?
    ): String {
        val contextBlock = if (!externalContext.isNullOrBlank()) {
            "\n\n## 外部指令\n以下是来自外部系统的创作指令，请将其融入设定中：\n\n$externalContext\n"
        } else ""

        return """
        你是这本书的总架构师。你的唯一输出是**散文密度的基础设定**——不是表格、不是 schema、不是条目化 bullet。你的散文密度决定了后面 planner 能不能读出"稀疏 memo"，writer 能不能写出活人，reviewer 能不能校准硬伤。$contextBlock

        ## 书籍元信息
        - 题材：$genre
        - 目标章数：${targetChapters}章
        - 每章字数：${chapterWordCount}字
        - 标题：$title
        - 创作简报：$brief

        ## 产出约束（硬性）
        - 有明确的数值/资源体系可追踪
        - 战力等级分明、年代和历史事实需要符合逻辑与考据（在 story_frame 中织入时代锚，在 book_rules 中写清不可违背的规则）

        ## 输出结构（5 个 SECTION，严格按 === SECTION: === 分块，不要漏任何一块）

        ## 去重铁律（必读）
        禁止在多段里重复同一事实。主角弧线只写在 roles；世界铁律只写在 story_frame.世界观底色；节奏原则只写在 volume_map 最后一段；角色当前现状只写在 roles.当前现状；初始钩子只写在 pending_hooks。如果一个段落写了另一段的内容，删掉。

        === SECTION: story_frame ===

        这是散文骨架。**4 段**，每段约 400-600 字，不要写表格，不要写 bullet list，写成能被人读下去的段落。段落标题用 ## 开头。主角弧线详见 roles 部分。

        ### 段 1：主题与基调
        写这本书到底讲的是什么。具体的命题与精神。主题下面跟着基调——温情冷冽悲壮肃杀。

        ### 段 2：核心冲突、对手定性、前台/后台双层故事
        这本书的主要矛盾是什么。主要对手是谁。
        前台故事：读者看得见的表层冲突；
        后台故事：藏在所有表层事件背后的深层阴谋或规则。

        ### 段 3：世界观底色（铁律 + 质感 + 本书专属规则）
        这个世界的物理规则与世界质感是什么。
        
        ### 段 4：终局方向 + 全书 Objective（OKR 大纲的根）
        最后一个镜头大致长什么样。可量化可判定的全书终极目标状态一句话。

        === SECTION: volume_map ===

        这是分卷散文地图，**5 段主体 + 1 段节奏原则尾段**。只写到卷级 prose——写清楚每卷的主题、角色阶段目标、卷尾不可逆事件。禁止指定具体章号。

        ### 段 1：各卷主题与情绪曲线
        ### 段 2：卷间钩子与回收承诺
        ### 段 3：各卷 OKR（Objective + Key Results）
        ### 段 4：卷尾必须发生的改变
        ### 段 5：节奏原则（具体化 + 通用）
        包含喘息频率、高潮间距、钩子密度、爽点节奏等。

        === SECTION: roles ===

        一人一卡 prose。主要/次要角色列表落盘。主角卡是本书角色弧线的唯一权威来源。
        用以下格式分隔：

        ---ROLE---
        tier: major
        name: 角色名
        ---CONTENT---
        ## 核心标签
        ## 反差细节
        ## 人物小传（过往经历）
        ## 主角弧线（起点 → 终点 → 代价）
        ## 当前现状（第 0 章初始状态）
        ## 关系网络
        ## 内在驱动
        ## 成长弧光

        === SECTION: book_rules ===

        ## 主角
        - 名字：主角名
        - 性格锁：性格关键词
        - 行为约束：行为边界

        ## 题材锁
        - 主类型：$genre

        ## 禁止事项
        - 书本创作禁忌点

        === SECTION: pending_hooks ===

        初始伏笔池表格（由于没有直接终端，保持 Markdown 风格的伏笔表）：
        | hook_id | start_chapter | type | status | last_advanced_chapter | expected_payoff | payoff_timing | depends_on | pays_off_in_arc | core_hook | half_life | notes |
        | H001 | 0 | setting | deferred | 0 | 揭露其神秘身世 | slow-burn | none | 终端卷 | true | 80 | 主角母亲留下的神秘锁扣 |

        """.trimIndent()
    }

    private fun buildEnglishFoundationPrompt(
        title: String,
        genre: String,
        targetChapters: Int,
        chapterWordCount: Int,
        brief: String,
        externalContext: String?
    ): String {
        val contextBlock = if (!externalContext.isNullOrBlank()) {
            "\n\n## External Instructions\n$externalContext\n"
        } else ""

        return """
        You are the architect of this book. Your only job is to produce **prose-density foundation design** — not tables, not schema, not bullet lists. $contextBlock

        ## Book metadata
        - Title: $title
        - Genre: $genre
        - Target chapters: $targetChapters
        - Chapter length: $chapterWordCount
        - Creative brief: $brief

        ## Output contract (5 === SECTION: === blocks)

        === SECTION: story_frame ===

        Four prose sections, ~400-600 characters each. No tables. No bullet lists. Real paragraphs.

        ## 01_Theme_and_Tonal_Ground
        ## 02_Core_Conflict_and_Foreground_Background_Story_Layers
        ## 03_World_Tonal_Ground
        ## 04_Endgame_Direction_and_Book_Objective

        === SECTION: volume_map ===

        Prose volume map, **5 sections + 1 closing rhythm paragraph**. Stay at volume-level prose only.
        
        ## 01_Volume_Themes_and_Emotional_Curves
        ## 02_Cross_Volume_Hooks_and_Payoff_Promises
        ## 03_Per_Volume_OKRs
        ## 04_Volume_End_Mandatory_Changes
        ## 05_Rhythm_Principles

        === SECTION: roles ===

        One-file-per-character prose. Inside roles block, separate each card using:

        ---ROLE---
        tier: major
        name: Character Name
        ---CONTENT---
        ## Core_Tags
        ## Contrast_Detail
        ## Back_Story
        ## Protagonist_Arc
        ## Current_State
        ## Relationship_Network
        ## Inner_Driver
        ## Growth_Arc

        === SECTION: book_rules ===

        ## Protagonist
        - Name: character name
        - Personality lock: key tags
        - Behavioral constraints: boundaries

        ## Genre Lock
        - Primary: $genre

        ## Prohibitions
        - Clichés and style fences

        === SECTION: pending_hooks ===

        Initial hook pool table:
        | hook_id | start_chapter | type | status | last_advanced_chapter | expected_payoff | payoff_timing | depends_on | pays_off_in_arc | core_hook | half_life | notes |
        | H001 | 0 | setting | deferred | 0 | Revealed ancestry | slow-burn | none | finale | true | 80 | The strange item |

        """.trimIndent()
    }

    suspend fun generateFoundation(
        title: String,
        genre: String,
        targetChapters: Int,
        chapterWordCount: Int,
        brief: String,
        externalContext: String? = null
    ): ArchitectOutput {
        val lang = determineLanguage(title, brief)
        val systemInstruction = AgentSystemPrompt.buildAgentSystemPrompt(null, lang, "book-create") +
                "\n\nYou are the Book Architect. You generate beautiful 5-section novel foundations."

        val prompt = if (lang == "zh") {
            buildChineseFoundationPrompt(title, genre, targetChapters, chapterWordCount, brief, externalContext)
        } else {
            buildEnglishFoundationPrompt(title, genre, targetChapters, chapterWordCount, brief, externalContext)
        }

        Log.i(TAG, "Requesting AI generation for Novel Architect Foundation blueprint")
        val content = LlmRouter.generateContent(systemInstruction, prompt, requireJson = false)

        return parseSectionsWithRepair(content, lang)
    }

    private fun parseSectionsWithRepair(content: String, language: String): ArchitectOutput {
        try {
            return parseSections(content, language)
        } catch (e: ArchitectIncompleteException) {
            Log.w(TAG, "Parsing failed due to incomplete sections: ${e.missing}. Retrying/Repairing flow omitted in fallback, returning partial.")
            // Build absolute recovery of missing fields
            return ArchitectOutput(
                storyBible = e.partialContent,
                bookRules = "# Compact Rules\n- Adhere to the core story style.",
                storyFrame = "Parsed Partial Frame",
                volumeMap = "Parsed Partial Map",
                roles = emptyList(),
                pendingHooks = "| H001 | 0 | setting | deferred | 0 | audit placeholder | slow-burn | none | general | false | 80 | hook |"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed parsing story bible sections completely, executing safe extraction", e)
            return ArchitectOutput(
                storyBible = content,
                bookRules = "# Generated Rules\n$content"
            )
        }
    }

    private fun parseSections(content: String, language: String): ArchitectOutput {
        val parsedSections = parseArchitectSectionMap(content)

        val storyFrame = parsedSections["story_frame"] ?: ""
        val volumeMap = parsedSections["volume_map"] ?: ""
        val rhythmPrinciples = parsedSections["rhythm_principles"] ?: ""
        val rolesRaw = parsedSections["roles"] ?: ""

        val legacyStoryBible = parsedSections["story_bible"] ?: ""
        val legacyVolumeOutline = parsedSections["volume_outline"] ?: ""
        val bookRules = parsedSections["book_rules"] ?: ""
        val pendingHooksRaw = parsedSections["pending_hooks"] ?: ""

        val effectiveStoryFrame = if (storyFrame.isNotEmpty()) storyFrame else legacyStoryBible
        val effectiveVolumeMap = if (volumeMap.isNotEmpty()) volumeMap else legacyVolumeOutline

        val missing = mutableListOf<String>()
        if (effectiveStoryFrame.isEmpty()) missing.add("story_frame")
        if (effectiveVolumeMap.isEmpty()) missing.add("volume_map")
        if (bookRules.isEmpty()) missing.add("book_rules")
        if (pendingHooksRaw.isEmpty()) missing.add("pending_hooks")

        if (missing.isNotEmpty()) {
            throw ArchitectIncompleteException(
                missing = missing,
                partialContent = content,
                message = "Story foundation came back incomplete. Missing: $missing"
            )
        }

        val roles = parseRoles(rolesRaw)
        val pendingHooks = stripTrailingAssistantCoda(pendingHooksRaw)

        // Synthesize single cohesive bible representation
        val storyBible = """
            # 📖 Story Frame
            $effectiveStoryFrame
            
            # 🗺️ Volume Map
            $effectiveVolumeMap
            
            # 👥 Character Directory / Roles Included
            $rolesRaw
            
            # 🪝 Pending Hooks Table
            $pendingHooks
        """.trimIndent()

        return ArchitectOutput(
            storyBible = storyBible,
            bookRules = bookRules,
            storyFrame = effectiveStoryFrame,
            volumeMap = effectiveVolumeMap,
            rhythmPrinciples = rhythmPrinciples,
            roles = roles,
            pendingHooks = pendingHooks
        )
    }

    private fun parseArchitectSectionMap(content: String): Map<String, String> {
        val sectionRegex = Regex("(?m)^\\s{0,3}(?:#{1,6}\\s*)?===\\s*SECTION\\s*[：:]\\s*([^\\n=]+?)\\s*===\\s*(?:#+\\s*)?$")
        val matches = sectionRegex.findAll(content).toList()
        val parsedSections = mutableMapOf<String, String>()

        if (matches.isNotEmpty()) {
            for (i in matches.indices) {
                val match = matches[i]
                val name = normalizeSectionName(match.groupValues[1])
                val start = match.range.last + 1
                val end = if (i + 1 < matches.size) matches[i + 1].range.first else content.length
                val sectionContent = content.substring(start, end).trim()
                parsedSections[name] = sectionContent
            }
            return parsedSections
        }

        // Fallback to headers
        val headingRegex = Regex("(?m)^\\s{0,3}#{1,3}\\s+(.+?)\\s*$")
        val headingMatches = headingRegex.findAll(content)
            .map { match ->
                val name = canonicalSectionNameFromHeading(match.groupValues[1])
                match to name
            }
            .filter { it.second != null }
            .toList()

        for (i in headingMatches.indices) {
            val (match, name) = headingMatches[i]
            val start = match.range.last + 1
            val end = if (i + 1 < headingMatches.size) headingMatches[i + 1].first.range.first else content.length
            val sectionContent = content.substring(start, end).trim()
            parsedSections[name!!] = sectionContent
        }

        return parsedSections
    }

    private fun normalizeSectionName(name: String): String {
        return name.trim()
            .lowercase()
            .replace(Regex("[`\"'*_]"), " ")
            .replace(Regex("[^a-z0-9]+"), "_")
            .replace(Regex("^_+|_+$"), "")
    }

    private fun canonicalSectionNameFromHeading(heading: String): String? {
        val normalized = normalizeSectionName(heading)
        if (listOf("story_frame", "story_bible", "story_foundation", "foundation").any { normalized.contains(it) } ||
            Regex("(故事框架|故事圣经|基础设定|世界框架|故事底座)").containsMatchIn(heading)) {
            return "story_frame"
        }
        if (listOf("volume_map", "volume_outline", "outline", "plot_map").any { normalized.contains(it) } ||
            Regex("(分卷地图|卷纲|分卷大纲|章节地图|故事大纲)").containsMatchIn(heading)) {
            return "volume_map"
        }
        if (listOf("roles", "characters", "character_cards").any { normalized.contains(it) } ||
            Regex("(角色设定|人物设定|角色卡|主要角色|角色|人物)").containsMatchIn(heading)) {
            return "roles"
        }
        if (listOf("book_rules", "rules", "writing_rules").any { normalized.contains(it) } ||
            Regex("(本书规则|写作规则|运行规则|创作规则|规则卡)").containsMatchIn(heading)) {
            return "book_rules"
        }
        if (listOf("pending_hooks", "hooks", "hook_ledger").any { normalized.contains(it) } ||
            Regex("(待回收钩子|待回收伏笔|伏笔表|钩子表|钩子|伏笔)").containsMatchIn(heading)) {
            return "pending_hooks"
        }
        if (listOf("rhythm_principles", "rhythm").any { normalized.contains(it) } ||
            Regex("(节奏原则|节奏)").containsMatchIn(heading)) {
            return "rhythm_principles"
        }
        if (listOf("current_state", "initial_state").any { normalized.contains(it) } ||
            Regex("(当前状态|初始状态)").containsMatchIn(heading)) {
            return "current_state"
        }
        return null
    }

    private fun parseRoles(raw: String): List<ArchitectRole> {
        if (raw.trim().isEmpty()) return emptyList()
        val blocks = raw.split(Regex("(?m)^---ROLE---\$"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        val roles = mutableListOf<ArchitectRole>()

        for (block in blocks) {
            val contentSplit = block.split(Regex("(?m)^---CONTENT---\$"))
            if (contentSplit.size < 2) continue

            val headerRaw = contentSplit[0].trim()
            val content = contentSplit.subList(1, contentSplit.size).joinToString("\n---CONTENT---\n").trim()

            val tierRegex = Regex("tier\\s*[:：]\\s*(major|minor|主要|次要)", RegexOption.IGNORE_CASE)
            val nameRegex = Regex("name\\s*[:：]\\s*(.+)", RegexOption.IGNORE_CASE)

            val tierMatch = tierRegex.find(headerRaw)
            val nameMatch = nameRegex.find(headerRaw)

            if (tierMatch == null || nameMatch == null) continue

            val tierRaw = tierMatch.groupValues[1].lowercase()
            val tier = if (tierRaw == "major" || tierRaw == "主要") "major" else "minor"
            val name = nameMatch.groupValues[1].trim()

            if (name.isNotEmpty() && content.isNotEmpty()) {
                roles.add(ArchitectRole(tier, name, content))
            }
        }
        return roles
    }

    private fun stripTrailingAssistantCoda(section: String): String {
        val lines = section.split("\n")
        val cutoffPattern = Regex("^(如果(?:你愿意|需要|想要|希望)|If (?:you(?:'d)? like|you want|needed)|I can (?:continue|next))", RegexOption.IGNORE_CASE)
        val cutoff = lines.indexOfFirst { line ->
            val trimmed = line.trim()
            trimmed.isNotEmpty() && cutoffPattern.containsMatchIn(trimmed)
        }
        if (cutoff < 0) {
            return section
        }
        return lines.subList(0, cutoff).joinToString("\n").trimEnd()
    }
}
