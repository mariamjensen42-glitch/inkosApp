package com.example.data.models

import org.json.JSONObject

/**
 * BookRules - Book rules models.
 *
 * This is the Kotlin Android equivalent of the TypeScript book-rules.ts models.
 * It contains:
 * - BookRules - Book-level rules configuration
 * - Protagonist - Protagonist configuration
 * - GenreLock - Genre lock configuration
 * - NumericalOverrides - Numerical system overrides
 * - EraConstraints - Era constraints
 * - ParsedBookRules - Parsed book rules with body
 */

data class Protagonist(
    val name: String,
    val personalityLock: List<String> = emptyList(),
    val behavioralConstraints: List<String> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("name", name)
            put("personalityLock", org.json.JSONArray(personalityLock))
            put("behavioralConstraints", org.json.JSONArray(behavioralConstraints))
        }
    }

    companion object {
        fun fromJson(json: JSONObject): Protagonist {
            return Protagonist(
                name = json.getString("name"),
                personalityLock = json.optJSONArray("personalityLock")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                behavioralConstraints = json.optJSONArray("behavioralConstraints")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList()
            )
        }
    }
}

data class GenreLock(
    val primary: String,
    val forbidden: List<String> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("primary", primary)
            put("forbidden", org.json.JSONArray(forbidden))
        }
    }

    companion object {
        fun fromJson(json: JSONObject): GenreLock {
            return GenreLock(
                primary = json.getString("primary"),
                forbidden = json.optJSONArray("forbidden")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList()
            )
        }
    }
}

data class NumericalOverrides(
    val hardCap: Any? = null, // number or string
    val resourceTypes: List<String> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            hardCap?.let { put("hardCap", it) }
            put("resourceTypes", org.json.JSONArray(resourceTypes))
        }
    }

    companion object {
        fun fromJson(json: JSONObject): NumericalOverrides {
            return NumericalOverrides(
                hardCap = if (json.has("hardCap") && !json.isNull("hardCap")) json.get("hardCap") else null,
                resourceTypes = json.optJSONArray("resourceTypes")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList()
            )
        }
    }
}

data class EraConstraints(
    val enabled: Boolean = false,
    val period: String? = null,
    val region: String? = null
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("enabled", enabled)
            period?.let { put("period", it) }
            region?.let { put("region", it) }
        }
    }

    companion object {
        fun fromJson(json: JSONObject): EraConstraints {
            return EraConstraints(
                enabled = json.optBoolean("enabled", false),
                period = json.optString("period", null),
                region = json.optString("region", null)
            )
        }
    }
}

data class BookRules(
    val version: String = "1.0",
    val protagonist: Protagonist? = null,
    val genreLock: GenreLock? = null,
    val narrativePerson: String? = null, // "first", "third"
    val numericalSystemOverrides: NumericalOverrides? = null,
    val eraConstraints: EraConstraints? = null,
    val prohibitions: List<String> = emptyList(),
    val chapterTypesOverride: List<String> = emptyList(),
    val fatigueWordsOverride: List<String> = emptyList(),
    val additionalAuditDimensions: List<Any> = emptyList(), // number or string
    val enableFullCastTracking: Boolean = false,
    val fanficMode: String? = null, // "canon", "au", "ooc", "cp"
    val allowedDeviations: List<String> = emptyList()
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("version", version)
            protagonist?.let { put("protagonist", it.toJson()) }
            genreLock?.let { put("genreLock", it.toJson()) }
            narrativePerson?.let { put("narrativePerson", it) }
            numericalSystemOverrides?.let { put("numericalSystemOverrides", it.toJson()) }
            eraConstraints?.let { put("eraConstraints", it.toJson()) }
            put("prohibitions", org.json.JSONArray(prohibitions))
            put("chapterTypesOverride", org.json.JSONArray(chapterTypesOverride))
            put("fatigueWordsOverride", org.json.JSONArray(fatigueWordsOverride))
            put("additionalAuditDimensions", org.json.JSONArray(additionalAuditDimensions))
            put("enableFullCastTracking", enableFullCastTracking)
            fanficMode?.let { put("fanficMode", it) }
            put("allowedDeviations", org.json.JSONArray(allowedDeviations))
        }
    }

    companion object {
        fun fromJson(json: JSONObject): BookRules {
            return BookRules(
                version = json.optString("version", "1.0"),
                protagonist = json.optJSONObject("protagonist")?.let { Protagonist.fromJson(it) },
                genreLock = json.optJSONObject("genreLock")?.let { GenreLock.fromJson(it) },
                narrativePerson = json.optString("narrativePerson", null),
                numericalSystemOverrides = json.optJSONObject("numericalSystemOverrides")?.let { NumericalOverrides.fromJson(it) },
                eraConstraints = json.optJSONObject("eraConstraints")?.let { EraConstraints.fromJson(it) },
                prohibitions = json.optJSONArray("prohibitions")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                chapterTypesOverride = json.optJSONArray("chapterTypesOverride")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                fatigueWordsOverride = json.optJSONArray("fatigueWordsOverride")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList(),
                additionalAuditDimensions = json.optJSONArray("additionalAuditDimensions")?.let { array ->
                    (0 until array.length()).map { array.get(it) }
                } ?: emptyList(),
                enableFullCastTracking = json.optBoolean("enableFullCastTracking", false),
                fanficMode = json.optString("fanficMode", null),
                allowedDeviations = json.optJSONArray("allowedDeviations")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                } ?: emptyList()
            )
        }
    }
}

data class ParsedBookRules(
    val rules: BookRules,
    val body: String
)

/**
 * Parse book rules from YAML frontmatter or Markdown.
 */
fun parseBookRules(raw: String): ParsedBookRules? {
    // Strip markdown code block wrappers if present
    val stripped = raw.replace(Regex("^```(?:md|markdown|yaml)?\\s*\\n"), "").replace(Regex("\\n```\\s*$"), "")

    // Try to find YAML frontmatter anywhere in the text
    val fmMatch = Regex("""---\s*\n([\s\S]*?)\n---\s*\n?([\s\S]*)$""").find(stripped)
    if (fmMatch != null) {
        try {
            val frontmatterStr = fmMatch.groupValues[1]
            val body = fmMatch.groupValues[2].trim()

            // Simple YAML parser for frontmatter
            val frontmatter = parseSimpleYaml(frontmatterStr)

            val rules = parseBookRulesFromMap(frontmatter)
            return ParsedBookRules(rules, body)
        } catch (e: Exception) {
            // YAML parse failed — fall through to default check
        }
    }

    // Check for legacy compat pointer
    if (isBookRulesShim(stripped)) {
        return null
    }

    // Parse as Markdown
    val rules = parseMarkdownBookRules(stripped)
    return ParsedBookRules(rules, stripped.trim())
}

private fun isBookRulesShim(raw: String): Boolean {
    return raw.contains("本书规则（兼容指针——已废弃）")
        || raw.contains("Book Rules (compat pointer — deprecated)")
        || raw.contains("本文件仅为外部读取保留")
        || raw.contains("This file is kept for external readers only")
}

private fun parseBookRulesFromMap(map: Map<String, Any?>): BookRules {
    return BookRules(
        version = map["version"] as? String ?: "1.0",
        protagonist = (map["protagonist"] as? Map<*, *>)?.let { m ->
            Protagonist(
                name = m["name"] as? String ?: "",
                personalityLock = (m["personalityLock"] as? List<*>)?.map { it.toString() } ?: emptyList(),
                behavioralConstraints = (m["behavioralConstraints"] as? List<*>)?.map { it.toString() } ?: emptyList()
            )
        },
        genreLock = (map["genreLock"] as? Map<*, *>)?.let { m ->
            GenreLock(
                primary = m["primary"] as? String ?: "",
                forbidden = (m["forbidden"] as? List<*>)?.map { it.toString() } ?: emptyList()
            )
        },
        narrativePerson = map["narrativePerson"] as? String,
        prohibitions = (map["prohibitions"] as? List<*>)?.map { it.toString() } ?: emptyList(),
        chapterTypesOverride = (map["chapterTypesOverride"] as? List<*>)?.map { it.toString() } ?: emptyList(),
        fatigueWordsOverride = (map["fatigueWordsOverride"] as? List<*>)?.map { it.toString() } ?: emptyList(),
        enableFullCastTracking = map["enableFullCastTracking"] as? Boolean ?: false,
        fanficMode = map["fanficMode"] as? String,
        allowedDeviations = (map["allowedDeviations"] as? List<*>)?.map { it.toString() } ?: emptyList()
    )
}

private fun parseMarkdownBookRules(markdown: String): BookRules {
    // Extract rules from markdown
    val prohibitions = mutableListOf<String>()
    val lines = markdown.lines()

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
            prohibitions.add(trimmed.substring(2))
        }
    }

    return BookRules(
        prohibitions = prohibitions
    )
}

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
