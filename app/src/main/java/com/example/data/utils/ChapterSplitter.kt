package com.example.data.utils

/**
 * ChapterSplitter - Splits text into chapters.
 *
 * This is the Kotlin Android equivalent of the TypeScript ChapterSplitter module.
 * It handles:
 * - Splitting text by chapter titles
 * - Supporting various chapter title patterns
 * - Extracting chapter content
 */

data class SplitChapter(
    val title: String,
    val content: String
)

/**
 * ChapterSplitter - Main class for splitting text into chapters.
 */
class ChapterSplitter {

    companion object {
        // Default pattern matches:
        // - "第一章 xxxx" / "第1章 xxxx"
        // - "第一回 xxxx" / "第1回 xxxx"
        // - "# 第1章 xxxx" / "## 第23章 xxxx"
        // - "CHAPTER I." / "CHAPTER II."
        private val DEFAULT_PATTERN = Regex(
            """^#{0,2}\s*(?:第[零〇○Ｏ０一二三四五六七八九十百千万\d]+(?:章|回)(?:[:：]|\s+)?\s*(.*)|Chapter\s+(?:\d+|[IVXLCDM]+)(?:\.|:|\s+)?\s*(.*))""",
            RegexOption.IGNORE_CASE
        )

        /**
         * Split a single text file into chapters by matching title lines.
         *
         * @param text The text to split
         * @param pattern Optional custom pattern to match chapter titles
         * @return List of SplitChapter objects
         */
        fun splitChapters(text: String, pattern: String? = null): List<SplitChapter> {
            val regex = if (pattern != null) {
                Regex(pattern, RegexOption.MULTILINE)
            } else {
                DEFAULT_PATTERN
            }

            val lines = text.split("\n")
            val chapters = mutableListOf<ChapterInfo>()

            for (i in lines.indices) {
                val match = regex.find(lines[i])
                if (match != null) {
                    val title = (match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
                        ?: match.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }
                        ?: "").trim()
                    chapters.add(ChapterInfo(title = title, startLine = i))
                }
            }

            if (chapters.isEmpty()) {
                return emptyList()
            }

            val result = mutableListOf<SplitChapter>()

            for (i in chapters.indices) {
                val chapter = chapters[i]
                val nextStart = if (i + 1 < chapters.size) {
                    chapters[i + 1].startLine
                } else {
                    lines.size
                }

                // Content starts after the title line
                val contentLines = lines.subList(chapter.startLine + 1, nextStart)
                val content = stripTrailingLicense(contentLines.joinToString("\n")).trim()

                result.add(
                    SplitChapter(
                        title = chapter.title.ifEmpty {
                            inferFallbackTitle(lines[chapter.startLine], i + 1)
                        },
                        content = content
                    )
                )
            }

            return result
        }

        private fun stripTrailingLicense(content: String): String {
            val trailerMatch = Regex("""^\s*Project Gutenberg(?:™|\(TM\))?.*${""}", RegexOption.MULTILINE)
                .find(content)
            if (trailerMatch == null) {
                return content
            }

            return content.substring(0, trailerMatch.range.first).trimEnd()
        }

        private fun inferFallbackTitle(headingLine: String, chapterNumber: Int): String {
            if (Regex("""chapter\s+(?:\d+|[ivxlcdm]+)""", RegexOption.IGNORE_CASE).containsMatchIn(headingLine)) {
                return "Chapter $chapterNumber"
            }

            if (Regex("""第[零一二三四五六七八九十百千万\d]+回""").containsMatchIn(headingLine)) {
                return "第${chapterNumber}回"
            }

            return "第${chapterNumber}章"
        }

        private data class ChapterInfo(
            val title: String,
            val startLine: Int
        )
    }
}

/**
 * Extension function to split text into chapters.
 */
fun String.splitChapters(pattern: String? = null): List<SplitChapter> {
    return ChapterSplitter.splitChapters(this, pattern)
}

/**
 * Extension function to split text into chapters with a custom regex.
 */
fun String.splitChapters(regex: Regex): List<SplitChapter> {
    val lines = this.split("\n")
    val chapters = mutableListOf<ChapterInfo>()

    for (i in lines.indices) {
        val match = regex.find(lines[i])
        if (match != null) {
            val title = (match.groupValues.getOrNull(1)?.takeIf { it.isNotEmpty() }
                ?: match.groupValues.getOrNull(2)?.takeIf { it.isNotEmpty() }
                ?: "").trim()
            chapters.add(ChapterInfo(title = title, startLine = i))
        }
    }

    if (chapters.isEmpty()) {
        return emptyList()
    }

    val result = mutableListOf<SplitChapter>()

    for (i in chapters.indices) {
        val chapter = chapters[i]
        val nextStart = if (i + 1 < chapters.size) {
            chapters[i + 1].startLine
        } else {
            lines.size
        }

        // Content starts after the title line
        val contentLines = lines.subList(chapter.startLine + 1, nextStart)
        val content = contentLines.joinToString("\n").trim()

        result.add(
            SplitChapter(
                title = chapter.title.ifEmpty {
                    "Chapter ${i + 1}"
                },
                content = content
            )
        )
    }

    return result
}

private data class ChapterInfo(
    val title: String,
    val startLine: Int
)
