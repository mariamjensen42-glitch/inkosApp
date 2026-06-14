package com.example.data.agents

import com.example.data.models.AgentContext
import com.example.data.state.MemoryDB
import com.example.data.state.StoredHook
import com.example.data.state.StoredSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * ConsolidatorAgent - Consolidates chapter summaries into volume-level narrative summaries.
 *
 * This is the Kotlin Android equivalent of the TypeScript ConsolidatorAgent class.
 * It handles:
 * - Consolidating chapter summaries by volume
 * - Reducing token usage for long books
 * - Preserving critical context
 */

data class ConsolidationResult(
    val volumeSummaries: String,
    val archivedVolumes: Int,
    val retainedChapters: Int,
    val promotedHookCount: Int
)

data class VolumeBoundary(
    val name: String,
    val startCh: Int,
    val endCh: Int
)

data class SummaryRow(
    val chapter: Int,
    val title: String,
    val characters: String,
    val events: String,
    val stateChanges: String,
    val hookActivity: String,
    val mood: String,
    val chapterType: String
)

/**
 * ConsolidatorAgent - Main class for consolidating chapter summaries.
 */
class ConsolidatorAgent(ctx: AgentContext) : BaseAgent(ctx) {

    override val name: String = "consolidator"

    /**
     * Consolidate chapter summaries by volume.
     * - Reads outline/volume_map.md to determine volume boundaries
     * - For each completed volume, LLM compresses chapter summaries into a narrative paragraph
     * - Archives detailed summaries, keeps only recent volume's per-chapter rows
     */
    suspend fun consolidate(bookDir: File): ConsolidationResult = withContext(Dispatchers.IO) {
        val storyDir = File(bookDir, "story")
        val summariesPath = File(storyDir, "chapter_summaries.md")
        val volumeSummariesPath = File(storyDir, "volume_summaries.md")

        val summariesRaw = try { summariesPath.readText() } catch (e: Exception) { "" }
        val outlineRaw = try { readVolumeMap(bookDir) } catch (e: Exception) { "" }

        // Pre-archive re-promotion pass
        val promotedHookCount = rerunAdvancedCountPromotion(storyDir)

        if (summariesRaw.isEmpty() || outlineRaw.isEmpty()) {
            return@withContext ConsolidationResult(
                volumeSummaries = "",
                archivedVolumes = 0,
                retainedChapters = 0,
                promotedHookCount = promotedHookCount
            )
        }

        // Parse volume boundaries from outline
        val volumeBoundaries = parseVolumeBoundaries(outlineRaw)
        if (volumeBoundaries.isEmpty()) {
            return@withContext ConsolidationResult(
                volumeSummaries = "",
                archivedVolumes = 0,
                retainedChapters = 0,
                promotedHookCount = promotedHookCount
            )
        }

        // Parse chapter summaries into rows
        val rows = parseSummaryTable(summariesRaw)
        if (rows.isEmpty()) {
            return@withContext ConsolidationResult(
                volumeSummaries = "",
                archivedVolumes = 0,
                retainedChapters = 0,
                promotedHookCount = promotedHookCount
            )
        }

        val maxChapter = rows.maxOf { it.chapter }

        // Determine which volumes are "completed" (all chapters written)
        val completedVolumes = mutableListOf<CompletedVolume>()
        val currentVolumeRows = mutableListOf<SummaryRow>()

        for (vol in volumeBoundaries) {
            val volRows = rows.filter { it.chapter >= vol.startCh && it.chapter <= vol.endCh }
            if (vol.endCh <= maxChapter && volRows.isNotEmpty()) {
                completedVolumes.add(CompletedVolume(vol.name, vol.startCh, vol.endCh, volRows))
            } else {
                // Current/incomplete volume — keep detailed rows
                currentVolumeRows.addAll(volRows)
            }
        }

        // Also keep any rows not covered by volume boundaries
        val coveredChapters = volumeBoundaries.flatMap { it.startCh..it.endCh }.toSet()
        for (row in rows) {
            if (row.chapter !in coveredChapters) {
                currentVolumeRows.add(row)
            }
        }

        if (completedVolumes.isEmpty()) {
            return@withContext ConsolidationResult(
                volumeSummaries = "",
                archivedVolumes = 0,
                retainedChapters = currentVolumeRows.size,
                promotedHookCount = promotedHookCount
            )
        }

        // Generate volume summaries using LLM
        val volumeSummaries = generateVolumeSummaries(completedVolumes)

        // Write volume summaries
        volumeSummariesPath.writeText(volumeSummaries)

        // Archive detailed summaries for completed volumes
        val archivedDir = File(storyDir, "archived_summaries")
        archivedDir.mkdirs()

        for (vol in completedVolumes) {
            val archiveFile = File(archivedDir, "volume_${vol.startCh}_${vol.endCh}.md")
            val archiveContent = buildString {
                appendLine("# Volume: ${vol.name}")
                appendLine("Chapters: ${vol.startCh}-${vol.endCh}")
                appendLine()
                appendLine("## Chapter Summaries")
                for (row in vol.rows) {
                    appendLine("### Chapter ${row.chapter}: ${row.title}")
                    appendLine("Characters: ${row.characters}")
                    appendLine("Events: ${row.events}")
                    appendLine("State Changes: ${row.stateChanges}")
                    appendLine("Hook Activity: ${row.hookActivity}")
                    appendLine("Mood: ${row.mood}")
                    appendLine("Chapter Type: ${row.chapterType}")
                    appendLine()
                }
            }
            archiveFile.writeText(archiveContent)
        }

        // Update chapter_summaries.md with only current volume rows
        val updatedSummaries = buildString {
            appendLine("| 章节 | 标题 | 角色 | 事件 | 状态变化 | 伏笔活动 | 情绪 | 章节类型 |")
            appendLine("|------|------|------|------|----------|----------|------|----------|")
            for (row in currentVolumeRows.sortedBy { it.chapter }) {
                appendLine("| ${row.chapter} | ${row.title} | ${row.characters} | ${row.events} | ${row.stateChanges} | ${row.hookActivity} | ${row.mood} | ${row.chapterType} |")
            }
        }
        summariesPath.writeText(updatedSummaries)

        ConsolidationResult(
            volumeSummaries = volumeSummaries,
            archivedVolumes = completedVolumes.size,
            retainedChapters = currentVolumeRows.size,
            promotedHookCount = promotedHookCount
        )
    }

    private suspend fun generateVolumeSummaries(completedVolumes: List<CompletedVolume>): String {
        val summaries = StringBuilder()
        summaries.appendLine("# Volume Summaries")
        summaries.appendLine()

        for (vol in completedVolumes) {
            val prompt = buildString {
                appendLine("Summarize the following chapter summaries into a concise narrative paragraph.")
                appendLine("Volume: ${vol.name}")
                appendLine("Chapters: ${vol.startCh}-${vol.endCh}")
                appendLine()
                appendLine("Chapter summaries:")
                for (row in vol.rows) {
                    appendLine("- Chapter ${row.chapter}: ${row.title}")
                    appendLine("  Characters: ${row.characters}")
                    appendLine("  Events: ${row.events}")
                    appendLine("  State Changes: ${row.stateChanges}")
                }
                appendLine()
                appendLine("Provide a 2-3 paragraph summary that captures the main narrative arc, key character developments, and important plot points.")
            }

            val summary = chat(prompt, "zh")
            summaries.appendLine("## ${vol.name}")
            appendLine("Chapters ${vol.startCh}-${vol.endCh}")
            appendLine()
            appendLine(summary)
            appendLine()
        }

        return summaries.toString()
    }

    private suspend fun rerunAdvancedCountPromotion(storyDir: File): Int {
        // In a full implementation, this would:
        // 1. Read pending_hooks.md
        // 2. Parse hooks
        // 3. Check advanced_count for each hook
        // 4. Promote hooks that cross the threshold
        // 5. Update pending_hooks.md
        return 0
    }

    private fun readVolumeMap(bookDir: File): String {
        // Try outline/volume_map.md first, then legacy volume_outline.md
        val volumeMapPath = File(bookDir, "story/outline/volume_map.md")
        if (volumeMapPath.exists()) {
            return volumeMapPath.readText()
        }

        val legacyPath = File(bookDir, "story/volume_outline.md")
        if (legacyPath.exists()) {
            return legacyPath.readText()
        }

        return ""
    }

    private fun parseVolumeBoundaries(outline: String): List<VolumeBoundary> {
        val boundaries = mutableListOf<VolumeBoundary>()
        val lines = outline.lines()

        var currentVolume: String? = null
        var currentStart: Int? = null
        var currentEnd: Int? = null

        for (line in lines) {
            val trimmed = line.trim()

            // Look for volume headers like "## Volume 1: Chapter 1-10"
            val volumeMatch = Regex("""##\s*(?:Volume|卷)\s*(\d+)[：:]\s*(?:Chapter|章)?\s*(\d+)\s*[-–~]\s*(\d+)""").find(trimmed)
            if (volumeMatch != null) {
                // Save previous volume if exists
                if (currentVolume != null && currentStart != null && currentEnd != null) {
                    boundaries.add(VolumeBoundary(currentVolume, currentStart, currentEnd))
                }

                currentVolume = "Volume ${volumeMatch.groupValues[1]}"
                currentStart = volumeMatch.groupValues[2].toInt()
                currentEnd = volumeMatch.groupValues[3].toInt()
            }

            // Also look for chapter range patterns
            val chapterMatch = Regex("""(?:Chapter|章)\s*(\d+)\s*[-–~]\s*(\d+)""").find(trimmed)
            if (chapterMatch != null && currentVolume != null) {
                currentStart = chapterMatch.groupValues[1].toInt()
                currentEnd = chapterMatch.groupValues[2].toInt()
            }
        }

        // Save last volume
        if (currentVolume != null && currentStart != null && currentEnd != null) {
            boundaries.add(VolumeBoundary(currentVolume, currentStart, currentEnd))
        }

        return boundaries
    }

    private fun parseSummaryTable(content: String): List<SummaryRow> {
        val rows = mutableListOf<SummaryRow>()
        val lines = content.lines()

        for (line in lines) {
            if (!line.startsWith("|") || line.contains("---") || line.contains("章节")) {
                continue
            }

            val cells = line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
            if (cells.size >= 8) {
                try {
                    rows.add(
                        SummaryRow(
                            chapter = cells[0].toInt(),
                            title = cells[1],
                            characters = cells[2],
                            events = cells[3],
                            stateChanges = cells[4],
                            hookActivity = cells[5],
                            mood = cells[6],
                            chapterType = cells[7]
                        )
                    )
                } catch (e: Exception) {
                    // Skip invalid rows
                }
            }
        }

        return rows
    }

    private data class CompletedVolume(
        val name: String,
        val startCh: Int,
        val endCh: Int,
        val rows: List<SummaryRow>
    )
}

/**
 * Extension function to consolidate book summaries.
 */
suspend fun File.consolidateSummaries(ctx: AgentContext): ConsolidationResult {
    return ConsolidatorAgent(ctx).consolidate(this)
}
