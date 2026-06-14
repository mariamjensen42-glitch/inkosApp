package com.example.data.api

import kotlin.math.sqrt

data class AITellIssue(
    val severity: String, // "warning" | "info"
    val category: String,
    val description: String,
    val suggestion: String
)

data class AITellResult(
    val issues: List<AITellIssue>
)

object AITellAnalyzer {
    private val HEDGE_WORDS = mapOf(
        "zh" to listOf("似乎", "可能", "或许", "大概", "某种程度上", "一定程度上", "在某种意义上"),
        "en" to listOf("seems", "seemed", "perhaps", "maybe", "apparently", "in some ways", "to some extent")
    )

    private val TRANSITION_WORDS = mapOf(
        "zh" to listOf("然而", "不过", "与此同时", "另一方面", "尽管如此", "话虽如此", "但值得注意的是"),
        "en" to listOf("however", "meanwhile", "on the other hand", "nevertheless", "even so", "still")
    )

    fun analyzeAITells(content: String, language: String = "zh"): AITellResult {
        val issues = mutableListOf<AITellIssue>()
        val isEnglish = language == "en"
        val langKey = if (isEnglish) "en" else "zh"
        val joiner = if (isEnglish) ", " else "、"

        val paragraphs = content
            .split(Regex("\\n\\s*\\n"))
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // dim 20: Paragraph length uniformity (needs >= 3 paragraphs)
        if (paragraphs.size >= 3) {
            val paragraphLengths = paragraphs.map { it.length }
            val mean = paragraphLengths.sum().toDouble() / paragraphLengths.size
            if (mean > 0) {
                val variance = paragraphLengths.fold(0.0) { sum, l -> sum + Math.pow(l - mean, 2.0) } / paragraphLengths.size
                val stdDev = sqrt(variance)
                val cv = stdDev / mean
                if (cv < 0.15) {
                    issues.add(
                        AITellIssue(
                            severity = "warning",
                            category = if (isEnglish) "Paragraph uniformity" else "段落等长",
                            description = if (isEnglish) {
                                "Paragraph-length coefficient of variation is only ${String.format("%.3f", cv)} (threshold <0.15), which suggests unnaturally uniform paragraph sizing"
                            } else {
                                "段落长度变异系数仅${String.format("%.3f", cv)}（阈值<0.15），段落长度过于均匀，呈现AI生成特征"
                            },
                            suggestion = if (isEnglish) {
                                "Increase paragraph-length contrast: use shorter beats for impact and longer blocks for immersive detail"
                            } else {
                                "增加段落长度差异：短段落用于节奏加速或冲击，长段落用于沉浸描写"
                            }
                        )
                    )
                }
            }
        }

        // dim 21: Hedge word density
        val totalChars = content.length
        if (totalChars > 0) {
            var hedgeCount = 0
            val words = HEDGE_WORDS[langKey] ?: emptyList()
            for (word in words) {
                val regex = Regex(Regex.escape(word), if (isEnglish) setOf(RegexOption.IGNORE_CASE) else emptySet())
                val matches = regex.findAll(content).toList()
                hedgeCount += matches.size
            }
            val hedgeDensity = hedgeCount.toDouble() / (totalChars / 1000.0)
            if (hedgeDensity > 3.0) {
                issues.add(
                    AITellIssue(
                        severity = "warning",
                        category = if (isEnglish) "Hedge density" else "套话密度",
                        description = if (isEnglish) {
                            "Hedge-word density is ${String.format("%.1f", hedgeDensity)} per 1k characters (threshold >3), making the prose sound overly tentative"
                        } else {
                            "套话词（似乎/可能/或许等）密度为${String.format("%.1f", hedgeDensity)}次/千字（阈值>3），语气过于模糊犹豫"
                        },
                        suggestion = if (isEnglish) {
                            "Replace hedges with firmer narration: remove vague qualifiers and use concrete detail instead"
                        } else {
                            "用确定性叙述替代模糊表达：去掉「似乎」直接描述状态，用具体细节替代「可能」"
                        }
                    )
                )
            }
        }

        // dim 22: Formulaic transition repetition
        val transitionCounts = mutableMapOf<String, Int>()
        val transitions = TRANSITION_WORDS[langKey] ?: emptyList()
        for (word in transitions) {
            val regex = Regex(Regex.escape(word), if (isEnglish) setOf(RegexOption.IGNORE_CASE) else emptySet())
            val count = regex.findAll(content).toList().size
            if (count > 0) {
                val key = if (isEnglish) word.lowercase() else word
                transitionCounts[key] = count
            }
        }
        val repeatedTransitions = transitionCounts.filter { it.value >= 3 }
        if (repeatedTransitions.isNotEmpty()) {
            val detail = repeatedTransitions.entries.joinToString(joiner) { (word, count) -> "\"$word\"×$count" }
            issues.add(
                AITellIssue(
                    severity = "warning",
                    category = if (isEnglish) "Formulaic transitions" else "公式化转折",
                    description = if (isEnglish) {
                        "Transition words repeat too often: $detail. Reusing the same transition pattern 3+ times creates a formulaic AI texture"
                    } else {
                        "转折词重复使用：$detail。同一转折模式≥3次暴露AI生成痕迹"
                    },
                    suggestion = if (isEnglish) {
                        "Let scenes pivot through action, timing, or viewpoint shifts instead of repeating the same transitions"
                    } else {
                        "用情节自然转折替代转折词，或换用不同的过渡手法（动作切入、时间跳跃、视角切换）"
                    }
                )
            )
        }

        // dim 23: List-like structure (consecutive sentences with same prefix pattern)
        val sentences = content
            .split(if (isEnglish) Regex("[.!?\\n]") else Regex("[。！？\\n]"))
            .map { it.trim() }
            .filter { it.length > 2 }

        if (sentences.size >= 3) {
            var consecutiveSamePrefix = 1
            var maxConsecutive = 1
            for (i in 1 until sentences.size) {
                val prevPrefix = if (isEnglish) {
                    sentences[i - 1].split(Regex("\\s+")).firstOrNull()?.lowercase() ?: ""
                } else {
                    if (sentences[i - 1].length >= 2) sentences[i - 1].substring(0, 2) else sentences[i - 1]
                }
                val currPrefix = if (isEnglish) {
                    sentences[i].split(Regex("\\s+")).firstOrNull()?.lowercase() ?: ""
                } else {
                    if (sentences[i].length >= 2) sentences[i].substring(0, 2) else sentences[i]
                }
                if (prevPrefix.isNotEmpty() && prevPrefix == currPrefix) {
                    consecutiveSamePrefix++
                    maxConsecutive = Math.max(maxConsecutive, consecutiveSamePrefix)
                } else {
                    consecutiveSamePrefix = 1
                }
            }
            if (maxConsecutive >= 3) {
                issues.add(
                    AITellIssue(
                        severity = "info",
                        category = if (isEnglish) "List-like structure" else "列表式结构",
                        description = if (isEnglish) {
                            "Detected $maxConsecutive consecutive sentences with the same opening pattern, creating a list-like generated cadence"
                        } else {
                            "检测到${maxConsecutive}句连续以相同开头的句子，呈现列表式AI生成结构"
                        },
                        suggestion = if (isEnglish) {
                            "Vary how sentences open: change subject, timing, or action entry to break the list effect"
                        } else {
                            "变换句式开头：用不同主语、时间词、动作词开头，打破列表感"
                        }
                    )
                )
            }
        }

        return AITellResult(issues)
    }
}
