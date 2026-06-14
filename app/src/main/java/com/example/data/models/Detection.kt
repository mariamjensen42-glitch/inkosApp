package com.example.data.models

import org.json.JSONObject

/**
 * Detection - Detection history and statistics models.
 *
 * This is the Kotlin Android equivalent of the TypeScript detection.ts models.
 * It contains:
 * - DetectionHistoryEntry - A single detection/rewrite event
 * - DetectionStats - Aggregated detection statistics
 */

data class DetectionHistoryEntry(
    val chapterNumber: Int,
    val timestamp: String,
    val provider: String,
    val score: Double,
    val action: String, // "detect", "rewrite"
    val attempt: Int
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("chapterNumber", chapterNumber)
            put("timestamp", timestamp)
            put("provider", provider)
            put("score", score)
            put("action", action)
            put("attempt", attempt)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DetectionHistoryEntry {
            return DetectionHistoryEntry(
                chapterNumber = json.getInt("chapterNumber"),
                timestamp = json.getString("timestamp"),
                provider = json.getString("provider"),
                score = json.getDouble("score"),
                action = json.getString("action"),
                attempt = json.getInt("attempt")
            )
        }
    }
}

data class ChapterDetectionStats(
    val chapterNumber: Int,
    val originalScore: Double,
    val finalScore: Double,
    val rewriteAttempts: Int
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("chapterNumber", chapterNumber)
            put("originalScore", originalScore)
            put("finalScore", finalScore)
            put("rewriteAttempts", rewriteAttempts)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): ChapterDetectionStats {
            return ChapterDetectionStats(
                chapterNumber = json.getInt("chapterNumber"),
                originalScore = json.getDouble("originalScore"),
                finalScore = json.getDouble("finalScore"),
                rewriteAttempts = json.getInt("rewriteAttempts")
            )
        }
    }
}

data class DetectionStats(
    val totalDetections: Int,
    val totalRewrites: Int,
    val avgOriginalScore: Double,
    val avgFinalScore: Double,
    val avgScoreReduction: Double,
    val passRate: Double,
    val chapterBreakdown: List<ChapterDetectionStats>
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("totalDetections", totalDetections)
            put("totalRewrites", totalRewrites)
            put("avgOriginalScore", avgOriginalScore)
            put("avgFinalScore", avgFinalScore)
            put("avgScoreReduction", avgScoreReduction)
            put("passRate", passRate)
            put("chapterBreakdown", org.json.JSONArray().apply {
                chapterBreakdown.forEach { chapter ->
                    put(chapter.toJson())
                }
            })
        }
    }

    companion object {
        fun fromJson(json: JSONObject): DetectionStats {
            return DetectionStats(
                totalDetections = json.getInt("totalDetections"),
                totalRewrites = json.getInt("totalRewrites"),
                avgOriginalScore = json.getDouble("avgOriginalScore"),
                avgFinalScore = json.getDouble("avgFinalScore"),
                avgScoreReduction = json.getDouble("avgScoreReduction"),
                passRate = json.getDouble("passRate"),
                chapterBreakdown = json.optJSONArray("chapterBreakdown")?.let { array ->
                    (0 until array.length()).map { i ->
                        ChapterDetectionStats.fromJson(array.getJSONObject(i))
                    }
                } ?: emptyList()
            )
        }
    }
}
