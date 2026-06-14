package com.example.data.models

import org.json.JSONObject

/**
 * StyleProfile - Style fingerprint profile models.
 *
 * This is the Kotlin Android equivalent of the TypeScript style-profile.ts models.
 * It contains:
 * - StyleProfile - Style fingerprint extracted from reference text
 * - ParagraphLengthRange - Min/max paragraph length range
 */

data class ParagraphLengthRange(
    val min: Int,
    val max: Int
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("min", min)
            put("max", max)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ParagraphLengthRange {
            return ParagraphLengthRange(
                min = json.getInt("min"),
                max = json.getInt("max")
            )
        }
    }
}

data class StyleProfile(
    val avgSentenceLength: Double,
    val sentenceLengthStdDev: Double,
    val avgParagraphLength: Double,
    val paragraphLengthRange: ParagraphLengthRange,
    val vocabularyDiversity: Double, // TTR (Type-Token Ratio)
    val topPatterns: List<String>,
    val rhetoricalFeatures: List<String>,
    val sourceName: String? = null,
    val analyzedAt: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("avgSentenceLength", avgSentenceLength)
            put("sentenceLengthStdDev", sentenceLengthStdDev)
            put("avgParagraphLength", avgParagraphLength)
            put("paragraphLengthRange", paragraphLengthRange.toJson())
            put("vocabularyDiversity", vocabularyDiversity)
            put("topPatterns", org.json.JSONArray(topPatterns))
            put("rhetoricalFeatures", org.json.JSONArray(rhetoricalFeatures))
            sourceName?.let { put("sourceName", it) }
            analyzedAt?.let { put("analyzedAt", it) }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): StyleProfile {
            return StyleProfile(
                avgSentenceLength = json.getDouble("avgSentenceLength"),
                sentenceLengthStdDev = json.getDouble("sentenceLengthStdDev"),
                avgParagraphLength = json.getDouble("avgParagraphLength"),
                paragraphLengthRange = ParagraphLengthRange.fromJson(json.getJSONObject("paragraphLengthRange")),
                vocabularyDiversity = json.getDouble("vocabularyDiversity"),
                topPatterns = json.optJSONArray("topPatterns")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                rhetoricalFeatures = json.optJSONArray("rhetoricalFeatures")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                sourceName = json.optString("sourceName", null),
                analyzedAt = json.optString("analyzedAt", null)
            )
        }
    }
}

/**
 * Analyze style profile from text.
 */
fun analyzeStyleProfile(text: String, sourceName: String? = null): StyleProfile {
    val sentences = text.split(Regex("[。！？.!?]+")).filter { it.isNotBlank() }
    val paragraphs = text.split(Regex("\\n\\n+")).filter { it.isNotBlank() }

    // Calculate sentence statistics
    val sentenceLengths = sentences.map { it.length }
    val avgSentenceLength = sentenceLengths.average()
    val sentenceLengthStdDev = if (sentenceLengths.size > 1) {
        val variance = sentenceLengths.map { (it - avgSentenceLength) * (it - avgSentenceLength) }.average()
        Math.sqrt(variance)
    } else {
        0.0
    }

    // Calculate paragraph statistics
    val paragraphLengths = paragraphs.map { it.length }
    val avgParagraphLength = paragraphLengths.average()
    val minParagraphLength = paragraphLengths.minOrNull() ?: 0
    val maxParagraphLength = paragraphLengths.maxOrNull() ?: 0

    // Calculate vocabulary diversity (TTR)
    val words = text.split(Regex("\\s+")).filter { it.isNotBlank() }
    val uniqueWords = words.toSet()
    val vocabularyDiversity = if (words.isNotEmpty()) {
        uniqueWords.size.toDouble() / words.size
    } else {
        0.0
    }

    // Extract patterns (simple n-gram analysis)
    val topPatterns = extractTopPatterns(text, 5)

    // Extract rhetorical features
    val rhetoricalFeatures = extractRhetoricalFeatures(text)

    return StyleProfile(
        avgSentenceLength = avgSentenceLength,
        sentenceLengthStdDev = sentenceLengthStdDev,
        avgParagraphLength = avgParagraphLength,
        paragraphLengthRange = ParagraphLengthRange(minParagraphLength, maxParagraphLength),
        vocabularyDiversity = vocabularyDiversity,
        topPatterns = topPatterns,
        rhetoricalFeatures = rhetoricalFeatures,
        sourceName = sourceName,
        analyzedAt = java.time.Instant.now().toString()
    )
}

private fun extractTopPatterns(text: String, count: Int): List<String> {
    val words = text.split(Regex("\\s+")).filter { it.length >= 2 }
    val bigrams = mutableMapOf<String, Int>()

    for (i in 0 until words.size - 1) {
        val bigram = "${words[i]} ${words[i + 1]}"
        bigrams[bigram] = (bigrams[bigram] ?: 0) + 1
    }

    return bigrams.entries
        .sortedByDescending { it.value }
        .take(count)
        .map { it.key }
}

private fun extractRhetoricalFeatures(text: String): List<String> {
    val features = mutableListOf<String>()

    // Check for common rhetorical patterns
    if (text.contains(Regex("仿佛|宛如|犹如|好像"))) {
        features.add("比喻")
    }
    if (text.contains(Regex("难道|岂|怎|怎么"))) {
        features.add("反问")
    }
    if (text.contains(Regex("！"))) {
        features.add("感叹")
    }
    if (text.contains(Regex("……|…"))) {
        features.add("省略")
    }
    if (text.contains(Regex(""|「|」"))) {
        features.add("对话")
    }

    return features
}
