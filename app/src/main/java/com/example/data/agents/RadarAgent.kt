package com.example.data.agents

import com.example.data.models.AgentContext
import com.example.data.models.BookGenre
import com.example.data.models.Platform
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * RadarAgent - Analyzes market trends and provides recommendations.
 *
 * This is the Kotlin Android equivalent of the TypeScript RadarAgent class.
 * It handles:
 * - Scanning market trends
 * - Analyzing platform rankings
 * - Providing book recommendations
 * - Identifying market opportunities
 */

data class RadarResult(
    val recommendations: List<RadarRecommendation>,
    val marketSummary: String,
    val timestamp: String
)

data class RadarRecommendation(
    val platform: Platform,
    val genre: BookGenre,
    val concept: String,
    val confidence: Double,
    val reasoning: String,
    val benchmarkTitles: List<String>
)

data class PlatformRankings(
    val platform: Platform,
    val entries: List<RankingEntry>
)

data class RankingEntry(
    val title: String,
    val author: String? = null,
    val category: String? = null,
    val extra: String = ""
)

/**
 * RadarAgent - Main class for market analysis.
 */
class RadarAgent(ctx: AgentContext) : BaseAgent(ctx) {

    override val name: String = "radar"

    /**
     * Scan market trends and provide recommendations.
     */
    suspend fun scan(): RadarResult = withContext(Dispatchers.IO) {
        // Fetch rankings from platforms
        val rankings = fetchRankings()
        val rankingsText = formatRankingsForPrompt(rankings)

        val systemPrompt = """
你是一个专业的网络小说市场分析师。下面是从各平台实时抓取的排行榜数据，请基于这些真实数据分析市场趋势。

## 实时排行榜数据

$rankingsText

分析维度：
1. 从排行榜数据中识别当前热门题材和标签
2. 分析哪些类型的作品占据榜单高位
3. 发现市场空白和机会点（榜单上缺少但有潜力的方向）
4. 风险提示（榜单上过度扎堆的题材）

输出格式必须为 JSON：
{
  "recommendations": [
    {
      "platform": "平台名",
      "genre": "题材类型",
      "concept": "一句话概念描述",
      "confidence": 0.0-1.0,
      "reasoning": "推荐理由（引用具体榜单数据）",
      "benchmarkTitles": ["对标书1", "对标书2"]
    }
  ],
  "marketSummary": "整体市场概述（基于真实榜单数据）"
}

推荐数量：3-5个，按 confidence 降序排列。
        """.trimIndent()

        val userPrompt = "请基于上面的实时排行榜数据，分析当前网文市场热度，给出开书建议。"

        val response = chat(systemPrompt, userPrompt, 0.6)
        parseResult(response)
    }

    private suspend fun fetchRankings(): List<PlatformRankings> {
        // In a full implementation, this would fetch real rankings from platforms
        // For now, return sample data
        return listOf(
            PlatformRankings(
                platform = Platform.FANQIE,
                entries = listOf(
                    RankingEntry("斗破苍穹", "天蚕土豆", "玄幻", "经典爽文"),
                    RankingEntry("完美世界", "辰东", "玄幻", "热血战斗"),
                    RankingEntry("遮天", "辰东", "玄幻", "宏大世界观")
                )
            ),
            PlatformRankings(
                platform = Platform.QIDIAN,
                entries = listOf(
                    RankingEntry("诡秘之主", "爱潜水的乌贼", "玄幻", "克苏鲁风格"),
                    RankingEntry("大奉打更人", "卖报小郎君", "仙侠", "轻松幽默"),
                    RankingEntry("万族之劫", "老鹰吃小鸡", "玄幻", "热血战斗")
                )
            )
        )
    }

    private fun formatRankingsForPrompt(rankings: List<PlatformRankings>): String {
        val sections = rankings
            .filter { it.entries.isNotEmpty() }
            .map { ranking ->
                val lines = ranking.entries.map { entry ->
                    "- ${entry.title}${entry.author?.let { " ($it)" } ?: ""}${entry.category?.let { " [$it]" } ?: ""} ${entry.extra}"
                }
                "### ${ranking.platform}\n${lines.joinToString("\n")}"
            }

        return if (sections.isNotEmpty()) {
            sections.joinToString("\n\n")
        } else {
            "（未能获取到实时排行数据，请基于你的知识分析）"
        }
    }

    private fun parseResult(response: String): RadarResult {
        return try {
            val json = JSONObject(response)
            val recommendations = json.optJSONArray("recommendations")?.let { array ->
                (0 until array.length()).map { i ->
                    val rec = array.getJSONObject(i)
                    RadarRecommendation(
                        platform = Platform.valueOf(rec.optString("platform", "FANQIE").uppercase()),
                        genre = BookGenre.valueOf(rec.optString("genre", "XUANHUAN").uppercase()),
                        concept = rec.optString("concept", ""),
                        confidence = rec.optDouble("confidence", 0.5),
                        reasoning = rec.optString("reasoning", ""),
                        benchmarkTitles = rec.optJSONArray("benchmarkTitles")?.let { titles ->
                            (0 until titles.length()).map { titles.getString(it) }
                        } ?: emptyList()
                    )
                }
            } ?: emptyList()

            RadarResult(
                recommendations = recommendations,
                marketSummary = json.optString("marketSummary", ""),
                timestamp = java.time.Instant.now().toString()
            )
        } catch (e: Exception) {
            RadarResult(
                recommendations = emptyList(),
                marketSummary = "Failed to parse radar result",
                timestamp = java.time.Instant.now().toString()
            )
        }
    }
}
