package com.example.data.models

import org.json.JSONObject

/**
 * GenreProfile - Genre profile models.
 *
 * This is the Kotlin Android equivalent of the TypeScript genre-profile.ts models.
 * It contains:
 * - GenreProfile - Genre configuration
 * - ParsedGenreProfile - Parsed genre profile with body
 */

data class GenreProfile(
    val name: String,
    val id: String,
    val language: String = "zh", // "zh", "en"
    val chapterTypes: List<String>,
    val fatigueWords: List<String>,
    val numericalSystem: Boolean = false,
    val powerScaling: Boolean = false,
    val eraResearch: Boolean = false,
    val pacingRule: String = "",
    val satisfactionTypes: List<String> = emptyList(),
    val auditDimensions: List<Int> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("id", id)
            put("language", language)
            put("chapterTypes", org.json.JSONArray(chapterTypes))
            put("fatigueWords", org.json.JSONArray(fatigueWords))
            put("numericalSystem", numericalSystem)
            put("powerScaling", powerScaling)
            put("eraResearch", eraResearch)
            put("pacingRule", pacingRule)
            put("satisfactionTypes", org.json.JSONArray(satisfactionTypes))
            put("auditDimensions", org.json.JSONArray(auditDimensions))
        }
    }

    companion object {
        fun fromJson(json: JSONObject): GenreProfile {
            return GenreProfile(
                name = json.getString("name"),
                id = json.getString("id"),
                language = json.optString("language", "zh"),
                chapterTypes = json.optJSONArray("chapterTypes")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                fatigueWords = json.optJSONArray("fatigueWords")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                numericalSystem = json.optBoolean("numericalSystem", false),
                powerScaling = json.optBoolean("powerScaling", false),
                eraResearch = json.optBoolean("eraResearch", false),
                pacingRule = json.optString("pacingRule", ""),
                satisfactionTypes = json.optJSONArray("satisfactionTypes")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                auditDimensions = json.optJSONArray("auditDimensions")?.let { array ->
                    (0 until array.length()).map { array.getInt(it) }
                } ?: emptyList()
            )
        }
    }
}

data class ParsedGenreProfile(
    val profile: GenreProfile,
    val body: String
)

/**
 * Parse genre profile from YAML frontmatter.
 */
fun parseGenreProfile(raw: String): ParsedGenreProfile {
    val fmMatch = Regex("""^---\s*\n([\s\S]*?)\n---\s*\n([\s\S]*)$""").find(raw)
        ?: throw IllegalArgumentException("Genre profile missing YAML frontmatter (--- ... ---)")

    val frontmatterStr = fmMatch.groupValues[1]
    val body = fmMatch.groupValues[2].trim()

    // Simple YAML parser for frontmatter
    val frontmatter = parseSimpleYaml(frontmatterStr)

    val profile = GenreProfile(
        name = frontmatter["name"] as? String ?: "",
        id = frontmatter["id"] as? String ?: "",
        language = frontmatter["language"] as? String ?: "zh",
        chapterTypes = (frontmatter["chapterTypes"] as? List<*>)?.map { it.toString() } ?: emptyList(),
        fatigueWords = (frontmatter["fatigueWords"] as? List<*>)?.map { it.toString() } ?: emptyList(),
        numericalSystem = frontmatter["numericalSystem"] as? Boolean ?: false,
        powerScaling = frontmatter["powerScaling"] as? Boolean ?: false,
        eraResearch = frontmatter["eraResearch"] as? Boolean ?: false,
        pacingRule = frontmatter["pacingRule"] as? String ?: "",
        satisfactionTypes = (frontmatter["satisfactionTypes"] as? List<*>)?.map { it.toString() } ?: emptyList(),
        auditDimensions = (frontmatter["auditDimensions"] as? List<*>)?.map { (it as Number).toInt() } ?: emptyList()
    )

    return ParsedGenreProfile(profile, body)
}

/**
 * Simple YAML parser for frontmatter.
 * This is a basic implementation - for production use, consider using a proper YAML library.
 */
private fun parseSimpleYaml(yaml: String): Map<String, Any?> {
    val result = mutableMapOf<String, Any?>()
    val lines = yaml.lines()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue

        val colonIndex = trimmed.indexOf(":")
        if (colonIndex > 0) {
            val key = trimmed.substring(0, colonIndex).trim()
            val valueStr = trimmed.substring(colonIndex + 1).trim()

            val value: Any? = when {
                valueStr == "true" -> true
                valueStr == "false" -> false
                valueStr.matches(Regex("^-?\\d+$")) -> valueStr.toInt()
                valueStr.matches(Regex("^-?\\d+\\.\\d+$")) -> valueStr.toDouble()
                valueStr.startsWith("[") && valueStr.endsWith("]") -> {
                    // Parse array
                    val arrayContent = valueStr.substring(1, valueStr.length - 1)
                    arrayContent.split(",").map { it.trim().removeSurrounding("\"") }
                }
                valueStr.startsWith("\"") && valueStr.endsWith("\"") -> {
                    valueStr.substring(1, valueStr.length - 1)
                }
                else -> valueStr
            }

            result[key] = value
        }
    }

    return result
}
