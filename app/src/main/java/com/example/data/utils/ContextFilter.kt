package com.example.data.utils

/**
 * ContextFilter - Smart context filtering for prompts.
 *
 * This is the Kotlin Android equivalent of the TypeScript ContextFilter module.
 * It handles:
 * - Capping large context blocks
 * - Filtering hooks (removing resolved/closed hooks)
 * - Filtering chapter summaries (keeping only recent chapters)
 * - Filtering subplots (removing closed/resolved subplots)
 * - Filtering emotional arcs (keeping only recent chapters)
 * - Filtering character matrix (keeping only relevant characters)
 */

data class ContextCapOptions(
    val label: String,
    val maxChars: Int,
    val headRatio: Double = 0.45
)

/**
 * ContextFilter - Main class for filtering context.
 */
class ContextFilter {

    companion object {
        private const val DEFAULT_CHAPTER_CADENCE_WINDOW = 10

        /**
         * Cap a large context block while keeping the durable setup at the beginning
         * and the latest tail.
         */
        fun capContextBlock(content: String, options: ContextCapOptions): String {
            if (content.isEmpty() || content == "(文件尚未创建)") return content

            val maxChars = options.maxChars
            if (maxChars <= 0) return ""
            if (content.length <= maxChars) return content

            val omitted = content.length - maxChars
            val note = "\n\n[InkOS context budget: omitted about $omitted chars from ${options.label}; kept beginning and latest tail.]\n\n"
            if (maxChars <= note.length + 2) {
                return content.substring(0, maxChars)
            }

            val keepChars = maxChars - note.length
            val headRatio = clampRatio(options.headRatio)
            val headChars = maxOf(1, (keepChars * headRatio).toInt())
            val tailChars = maxOf(1, keepChars - headChars)

            return "${content.substring(0, headChars)}$note${content.substring(content.length - tailChars)}"
        }

        /**
         * Filter pending_hooks: remove resolved/closed hooks.
         */
        fun filterHooks(hooks: String): String {
            if (hooks.isEmpty() || hooks == "(文件尚未创建)") return hooks
            return filterTableRows(hooks) { row ->
                val lower = row.lowercase()
                !lower.contains("已回收") && !lower.contains("resolved") && !lower.contains("closed")
            }
        }

        /**
         * Filter chapter_summaries: keep only the most recent N chapters.
         */
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

        /**
         * Filter subplot_board: remove closed/resolved subplots.
         */
        fun filterSubplots(board: String): String {
            if (board.isEmpty() || board == "(文件尚未创建)") return board
            return filterTableRows(board) { row ->
                val lower = row.lowercase()
                !lower.contains("已回收") && !lower.contains("closed") && !lower.contains("resolved") && !lower.contains("已完结")
            }
        }

        /**
         * Filter emotional_arcs: keep only the most recent N chapters.
         */
        fun filterEmotionalArcs(
            arcs: String,
            currentChapter: Int,
            keepRecent: Int = DEFAULT_CHAPTER_CADENCE_WINDOW
        ): String {
            if (arcs.isEmpty() || arcs == "(文件尚未创建)") return arcs
            return filterTableRows(arcs) { row ->
                val match = Regex("""\|\s*(\d+)\s*\|""").find(row)
                if (match == null) {
                    true
                } else {
                    match.groupValues[1].toInt() > currentChapter - keepRecent
                }
            }
        }

        /**
         * Filter character_matrix: keep only characters mentioned in the volume outline
         * current section + protagonist.
         */
        fun filterCharacterMatrix(
            matrix: String,
            volumeOutline: String,
            protagonistName: String? = null
        ): String {
            if (matrix.isEmpty() || matrix == "(文件尚未创建)") return matrix

            // Extract names from outline
            val names = extractNames(volumeOutline).toMutableSet()
            if (protagonistName != null) names.add(protagonistName)
            if (names.isEmpty()) return matrix

            // Split into sections (### 角色档案, ### 相遇记录, ### 信息边界)
            val sections = matrix.split(Regex("""(?=^###)""", RegexOption.MULTILINE))
            val filtered = sections.map { section ->
                filterTableRows(section) { row ->
                    names.any { name -> row.contains(name) }
                }
            }

            val result = filtered.joinToString("\n")
            // Fallback: if filtering removed all data rows, return original
            val dataRowCount = result.split("\n").count { line ->
                line.startsWith("|") && !line.contains("---") && !isHeaderRow(line)
            }
            return if (dataRowCount > 0) result else matrix
        }

        /**
         * Extract character names from text.
         * Chinese: 2-4 char sequences before punctuation.
         * English: Capitalized words 3+ chars.
         */
        private fun extractNames(text: String): Set<String> {
            val names = mutableSetOf<String>()

            // Chinese names
            val cnRegex = Regex("""[\u4e00-\u9fff]{2,4}(?=[，、。：\s]|$)""")
            cnRegex.findAll(text).forEach { match ->
                names.add(match.value)
            }

            // English names
            val enRegex = Regex("""\b[A-Z][a-z]{2,}\b""")
            enRegex.findAll(text).forEach { match ->
                names.add(match.value)
            }

            return names
        }

        private fun clampRatio(value: Double): Double {
            if (!value.isFinite()) return 0.45
            return value.coerceIn(0.2, 0.8)
        }

        private fun isHeaderRow(line: String): Boolean {
            // First data-like row in a table (contains column names)
            return Regex("""^\|\s*(章节|角色|支线|hook_id|Chapter|Character|Subplot)""", RegexOption.IGNORE_CASE)
                .containsMatchIn(line)
        }

        /**
         * Generic markdown table row filter.
         * Keeps header rows + separator rows + rows passing the predicate.
         * Falls back to original if filtering empties all data rows.
         */
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

            // Fallback: if no rows pass, return original
            if (filtered.isEmpty() && dataLines.isNotEmpty()) {
                return content
            }

            return (nonTableLines + headerLines + filtered).joinToString("\n")
        }
    }
}

/**
 * Extension function to cap context block.
 */
fun String.capContextBlock(options: ContextCapOptions): String {
    return ContextFilter.capContextBlock(this, options)
}

/**
 * Extension function to filter hooks.
 */
fun String.filterHooks(): String {
    return ContextFilter.filterHooks(this)
}

/**
 * Extension function to filter summaries.
 */
fun String.filterSummaries(currentChapter: Int, keepRecent: Int = 10): String {
    return ContextFilter.filterSummaries(this, currentChapter, keepRecent)
}

/**
 * Extension function to filter subplots.
 */
fun String.filterSubplots(): String {
    return ContextFilter.filterSubplots(this)
}

/**
 * Extension function to filter emotional arcs.
 */
fun String.filterEmotionalArcs(currentChapter: Int, keepRecent: Int = 10): String {
    return ContextFilter.filterEmotionalArcs(this, currentChapter, keepRecent)
}

/**
 * Extension function to filter character matrix.
 */
fun String.filterCharacterMatrix(volumeOutline: String, protagonistName: String? = null): String {
    return ContextFilter.filterCharacterMatrix(this, volumeOutline, protagonistName)
}
