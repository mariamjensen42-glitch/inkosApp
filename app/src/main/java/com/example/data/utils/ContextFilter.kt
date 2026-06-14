package com.example.data.utils

/**
 * ContextFilter - Smart context filtering for prompts.
 */

class ContextFilter {

    companion object {
        private const val DEFAULT_CHAPTER_CADENCE_WINDOW = 10

        fun capContextBlock(content: String, maxChars: Int, label: String): String {
            if (content.isEmpty() || content == "(文件尚未创建)") return content
            if (maxChars <= 0) return ""
            if (content.length <= maxChars) return content

            val omitted = content.length - maxChars
            val note = "\n\n[InkOS context budget: omitted about $omitted chars from $label; kept beginning and latest tail.]\n\n"
            if (maxChars <= note.length + 2) {
                return content.substring(0, maxChars)
            }

            val keepChars = maxChars - note.length
            val headChars = maxOf(1, (keepChars * 0.45).toInt())
            val tailChars = maxOf(1, keepChars - headChars)

            return "${content.substring(0, headChars)}$note${content.substring(content.length - tailChars)}"
        }

        fun filterHooks(hooks: String): String {
            if (hooks.isEmpty() || hooks == "(文件尚未创建)") return hooks
            return filterTableRows(hooks) { row ->
                val lower = row.lowercase()
                !lower.contains("已回收") && !lower.contains("resolved") && !lower.contains("closed")
            }
        }

        fun filterSummaries(
            summaries: String,
            currentChapter: Int,
            keepRecent: Int = DEFAULT_CHAPTER_CADENCE_WINDOW
        ): String {
            if (summaries.isEmpty() || summaries == "(文件尚未创建)") return summaries
            return filterTableRows(summaries) { row ->
                val match = Regex("""\|\s*(\d+)\s*\|""").find(row)
                if (match == null) {
                    true
                } else {
                    match.groupValues[1].toInt() > currentChapter - keepRecent
                }
            }
        }

        private fun filterTableRows(content: String, predicate: (String) -> Boolean): String {
            val lines = content.split("\n")
            val nonTableLines = mutableListOf<String>()
            val headerLines = mutableListOf<String>()
            val dataLines = mutableListOf<String>()

            for (line in lines) {
                if (!line.startsWith("|")) {
                    nonTableLines.add(line)
                } else if (line.contains("---") || isHeaderRow(line)) {
                    headerLines.add(line)
                } else {
                    dataLines.add(line)
                }
            }

            val filtered = dataLines.filter(predicate)

            if (filtered.isEmpty() && dataLines.isNotEmpty()) {
                return content
            }

            return (nonTableLines + headerLines + filtered).joinToString("\n")
        }

        private fun isHeaderRow(line: String): Boolean {
            return Regex("""^\|\s*(章节|角色|支线|hook_id|Chapter|Character|Subplot)""", RegexOption.IGNORE_CASE)
                .containsMatchIn(line)
        }
    }
}
