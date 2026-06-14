package com.example.data.agents

import com.example.data.models.AgentContext
import com.example.data.models.ChapterMemo
import com.example.data.models.ContextPackage
import com.example.data.models.RuleStack
import com.example.data.utils.ContextFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * ContinuityAuditor - Audits chapter content for continuity issues.
 *
 * This is the Kotlin Android equivalent of the TypeScript ContinuityAuditor class.
 * It handles:
 * - Auditing chapter content for continuity issues
 * - Checking 37 dimensions of quality
 * - Identifying critical, warning, and info issues
 * - Providing suggestions for fixing issues
 */

data class AuditResult(
    val passed: Boolean,
    val issues: List<AuditIssue>,
    val summary: String,
    val parseFailed: Boolean = false,
    val overallScore: Int? = null, // 0-100
    val tokenUsage: TokenUsage? = null
)

data class AuditIssue(
    val severity: String, // "critical", "warning", "info"
    val category: String,
    val description: String,
    val suggestion: String,
    val repairScope: String? = null // "local", "structural", "unknown"
)

data class ContinuityAuditInput(
    val chapterContent: String,
    val chapterNumber: Int,
    val contextPackage: ContextPackage,
    val chapterMemo: ChapterMemo? = null,
    val ruleStack: RuleStack? = null,
    val language: String = "zh",
    val temperature: Double = 0.3
)

/**
 * ContinuityAuditor - Main class for auditing chapter content.
 */
class ContinuityAuditor(ctx: AgentContext) : BaseAgent(ctx) {

    override val name: String = "continuity-auditor"

    companion object {
        // 37 dimensions of quality
        private val DIMENSION_LABELS = mapOf(
            1 to Pair("OOC检查", "OOC Check"),
            2 to Pair("时间线检查", "Timeline Check"),
            3 to Pair("设定冲突", "Lore Conflict Check"),
            4 to Pair("战力崩坏", "Power Scaling Check"),
            5 to Pair("数值检查", "Numerical Consistency Check"),
            6 to Pair("伏笔检查", "Hook Check"),
            7 to Pair("节奏检查", "Pacing Check"),
            8 to Pair("文风检查", "Style Check"),
            9 to Pair("信息越界", "Information Boundary Check"),
            10 to Pair("词汇疲劳", "Lexical Fatigue Check"),
            11 to Pair("利益链断裂", "Incentive Chain Check"),
            12 to Pair("年代考据", "Era Accuracy Check"),
            13 to Pair("配角降智", "Side Character Competence Check"),
            14 to Pair("配角工具人化", "Side Character Instrumentalization Check"),
            15 to Pair("爽点虚化", "Payoff Dilution Check"),
            16 to Pair("台词失真", "Dialogue Authenticity Check"),
            17 to Pair("流水账", "Chronicle Drift Check"),
            18 to Pair("知识库污染", "Knowledge Base Pollution Check"),
            19 to Pair("视角一致性", "POV Consistency Check"),
            20 to Pair("段落等长", "Paragraph Uniformity Check"),
            21 to Pair("套话密度", "Cliche Density Check"),
            22 to Pair("公式化转折", "Formulaic Twist Check"),
            23 to Pair("列表式结构", "List-like Structure Check"),
            24 to Pair("支线停滞", "Subplot Stagnation Check"),
            25 to Pair("弧线平坦", "Arc Flatline Check"),
            26 to Pair("节奏单调", "Pacing Monotony Check"),
            27 to Pair("敏感词检查", "Sensitive Content Check"),
            28 to Pair("正传事件冲突", "Mainline Canon Event Conflict"),
            29 to Pair("未来信息泄露", "Future Knowledge Leak Check"),
            30 to Pair("世界规则跨书一致性", "Cross-Book World Rule Check"),
            31 to Pair("番外伏笔隔离", "Spinoff Hook Isolation Check"),
            32 to Pair("读者期待管理", "Reader Expectation Check"),
            33 to Pair("章节备忘偏离", "Chapter Memo Drift Check"),
            34 to Pair("角色还原度", "Character Fidelity Check"),
            35 to Pair("世界规则遵守", "World Rule Compliance Check"),
            36 to Pair("关系动态", "Relationship Dynamics Check"),
            37 to Pair("正典事件一致性", "Canon Event Consistency Check")
        )
    }

    /**
     * Audit chapter content for continuity issues.
     */
    suspend fun audit(input: ContinuityAuditInput): AuditResult = withContext(Dispatchers.IO) {
        val language = input.language
        val isEnglish = language == "en"

        // Build context from package
        val context = buildContext(input.contextPackage, input.chapterNumber, isEnglish)

        // Build rule stack
        val ruleStackText = if (input.ruleStack != null) {
            buildRuleStackText(input.ruleStack, isEnglish)
        } else {
            ""
        }

        // Build chapter memo
        val memoText = if (input.chapterMemo != null) {
            if (isEnglish) {
                """
                ## Chapter Memo
                Goal: ${input.chapterMemo.goal}
                
                ${input.chapterMemo.body}
                """.trimIndent()
            } else {
                """
                ## 章节备忘
                goal：${input.chapterMemo.goal}
                
                ${input.chapterMemo.body}
                """.trimIndent()
            }
        } else {
            ""
        }

        val systemPrompt = buildSystemPrompt(isEnglish)
        val userPrompt = buildUserPrompt(
            input.chapterContent,
            input.chapterNumber,
            context,
            ruleStackText,
            memoText,
            isEnglish
        )

        val response = chat(systemPrompt, userPrompt, input.temperature)
        parseAuditResult(response, isEnglish)
    }

    private fun buildContext(contextPackage: ContextPackage, chapterNumber: Int, isEnglish: Boolean): String {
        val sb = StringBuilder()

        // Current state
        if (contextPackage.currentState.isNotEmpty()) {
            sb.appendLine(if (isEnglish) "## Current State" else "## 当前状态")
            sb.appendLine(contextPackage.currentState)
            sb.appendLine()
        }

        // Pending hooks
        if (contextPackage.pendingHooks.isNotEmpty()) {
            sb.appendLine(if (isEnglish) "## Pending Hooks" else "## 待处理伏笔")
            sb.appendLine(ContextFilter.filterHooks(contextPackage.pendingHooks))
            sb.appendLine()
        }

        // Chapter summaries
        if (contextPackage.chapterSummaries.isNotEmpty()) {
            sb.appendLine(if (isEnglish) "## Chapter Summaries" else "## 章节摘要")
            sb.appendLine(ContextFilter.filterSummaries(contextPackage.chapterSummaries, chapterNumber))
            sb.appendLine()
        }

        // Character matrix
        if (contextPackage.characterMatrix.isNotEmpty()) {
            sb.appendLine(if (isEnglish) "## Character Matrix" else "## 角色矩阵")
            sb.appendLine(contextPackage.characterMatrix)
            sb.appendLine()
        }

        return sb.toString()
    }

    private fun buildRuleStackText(ruleStack: RuleStack, isEnglish: Boolean): String {
        val sb = StringBuilder()

        if (ruleStack.bookRules.isNotEmpty()) {
            sb.appendLine(if (isEnglish) "## Book Rules" else "## 书级规则")
            sb.appendLine(ruleStack.bookRules)
            sb.appendLine()
        }

        if (ruleStack.genreRules.isNotEmpty()) {
            sb.appendLine(if (isEnglish) "## Genre Rules" else "## 题材规则")
            sb.appendLine(ruleStack.genreRules)
            sb.appendLine()
        }

        if (ruleStack.chapterRules.isNotEmpty()) {
            sb.appendLine(if (isEnglish) "## Chapter Rules" else "## 章节规则")
            sb.appendLine(ruleStack.chapterRules)
            sb.appendLine()
        }

        return sb.toString()
    }

    private fun buildSystemPrompt(isEnglish: Boolean): String {
        return if (isEnglish) {
            """
You are a professional web fiction continuity auditor. Your job is to audit chapter content for continuity issues across 37 dimensions.

## Audit Dimensions

${DIMENSION_LABELS.entries.joinToString("\n") { "- ${it.key}. ${it.value.second}" }}

## Output Format

Return a JSON object with the following structure:
{
  "passed": true/false,
  "overallScore": 0-100,
  "issues": [
    {
      "severity": "critical/warning/info",
      "category": "dimension name",
      "description": "issue description",
      "suggestion": "fix suggestion",
      "repairScope": "local/structural/unknown"
    }
  ],
  "summary": "overall summary"
}

## Severity Levels

- **critical**: Must fix before publishing. Breaks immersion, violates canon, or causes reader confusion.
- **warning**: Should fix. Reduces quality but doesn't break the story.
- **info**: Nice to fix. Minor improvements.

## Repair Scope

- **local**: Can be fixed in this chapter only.
- **structural**: Requires changes to multiple chapters or the outline.
- **unknown**: Unclear scope.
            """.trimIndent()
        } else {
            """
你是一位专业网文连续性审计编辑。你的工作是审计章节内容在37个维度上的连续性问题。

## 审计维度

${DIMENSION_LABELS.entries.joinToString("\n") { "- ${it.key}. ${it.value.first}" }}

## 输出格式

返回一个JSON对象，结构如下：
{
  "passed": true/false,
  "overallScore": 0-100,
  "issues": [
    {
      "severity": "critical/warning/info",
      "category": "维度名称",
      "description": "问题描述",
      "suggestion": "修复建议",
      "repairScope": "local/structural/unknown"
    }
  ],
  "summary": "总体摘要"
}

## 严重程度

- **critical**: 发布前必须修复。破坏沉浸感、违反设定或导致读者困惑。
- **warning**: 应该修复。降低质量但不破坏故事。
- **info**: 可以修复。小幅改进。

## 修复范围

- **local**: 仅需在本章修复。
- **structural**: 需要修改多章或大纲。
- **unknown**: 范围不明确。
            """.trimIndent()
        }
    }

    private fun buildUserPrompt(
        chapterContent: String,
        chapterNumber: Int,
        context: String,
        ruleStackText: String,
        memoText: String,
        isEnglish: Boolean
    ): String {
        return if (isEnglish) {
            """
Audit chapter $chapterNumber for continuity issues.

$context

$ruleStackText

$memoText

## Chapter Content

$chapterContent

Please audit this chapter and return the result in JSON format.
            """.trimIndent()
        } else {
            """
审计第${chapterNumber}章的连续性问题。

$context

$ruleStackText

$memoText

## 章节内容

$chapterContent

请审计此章节并以JSON格式返回结果。
            """.trimIndent()
        }
    }

    private fun parseAuditResult(response: String, isEnglish: Boolean): AuditResult {
        return try {
            val json = JSONObject(response)
            val issues = json.optJSONArray("issues")?.let { array ->
                (0 until array.length()).map { i ->
                    val issue = array.getJSONObject(i)
                    AuditIssue(
                        severity = issue.optString("severity", "info"),
                        category = issue.optString("category", ""),
                        description = issue.optString("description", ""),
                        suggestion = issue.optString("suggestion", ""),
                        repairScope = issue.optString("repairScope", null)
                    )
                }
            } ?: emptyList()

            AuditResult(
                passed = json.optBoolean("passed", true),
                issues = issues,
                summary = json.optString("summary", ""),
                overallScore = json.optInt("overallScore", null)
            )
        } catch (e: Exception) {
            AuditResult(
                passed = false,
                issues = emptyList(),
                summary = if (isEnglish) "Failed to parse audit result" else "解析审计结果失败",
                parseFailed = true
            )
        }
    }
}
