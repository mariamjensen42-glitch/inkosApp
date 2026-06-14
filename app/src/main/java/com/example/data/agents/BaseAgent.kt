package com.example.data.agents

import android.util.Log
import com.example.data.llm.LLMClient
import com.example.data.llm.LLMMessage
import com.example.data.llm.LLMResponse
import com.example.data.llm.LLMProvider
import com.example.data.llm.ChatCompletionOptions

/**
 * Agent context - matches TypeScript AgentContext interface
 */
data class AgentContext(
    val client: LLMClient,
    val model: String,
    val projectRoot: String,
    val bookId: String? = null,
    val logger: Logger? = null,
    val onStreamProgress: ((com.example.data.llm.StreamProgress) -> Unit)? = null
)

/**
 * Simple logger interface
 */
interface Logger {
    fun info(message: String)
    fun warn(message: String)
    fun error(message: String)
    fun child(name: String): Logger
}

/**
 * Android logcat logger implementation
 */
class LogcatLogger(private val tag: String = "InkOS") : Logger {
    override fun info(message: String) = Log.i(tag, message)
    override fun warn(message: String) = Log.w(tag, message)
    override fun error(message: String) = Log.e(tag, message)
    override fun child(name: String): Logger = LogcatLogger("$tag.$name")
}

/**
 * Base agent - matches TypeScript BaseAgent abstract class
 * Provides common functionality for all agents
 */
abstract class BaseAgent(protected val ctx: AgentContext) {
    
    abstract val name: String
    
    protected val log: Logger?
        get() = ctx.logger

    /**
     * Chat with LLM - matches TypeScript chat method
     */
    protected suspend fun chat(
        messages: List<LLMMessage>,
        options: ChatCompletionOptions = ChatCompletionOptions()
    ): LLMResponse {
        return LLMProvider.chatCompletion(
            client = ctx.client,
            model = ctx.model,
            messages = messages,
            options = options.copy(
                onStreamProgress = options.onStreamProgress ?: ctx.onStreamProgress
            )
        )
    }

    /**
     * Helper to create a system message
     */
    protected fun systemMessage(content: String): LLMMessage {
        return LLMMessage(role = "system", content = content)
    }

    /**
     * Helper to create a user message
     */
    protected fun userMessage(content: String): LLMMessage {
        return LLMMessage(role = "user", content = content)
    }

    /**
     * Helper to create an assistant message
     */
    protected fun assistantMessage(content: String): LLMMessage {
        return LLMMessage(role = "assistant", content = content)
    }

    /**
     * Determine language from text content
     */
    protected fun determineLanguage(title: String, brief: String): String {
        val content = title + brief
        for (char in content) {
            if (char.code in 0x4E00..0x9FFF) {
                return "zh"
            }
        }
        return "en"
    }

    /**
     * Check if language is Chinese
     */
    protected fun isChineseLanguage(language: String?): Boolean {
        return (language ?: "zh").lowercase().startsWith("zh")
    }

    /**
     * Localize message based on language
     */
    protected fun localize(language: String, messages: Map<String, String>): String {
        return if (language == "en") messages["en"]!! else messages["zh"]!!
    }

    /**
     * Extract section from markdown content
     */
    protected fun extractSection(content: String, headings: List<String>): String? {
        val lines = content.split("\n")
        val normalizedHeadings = headings.map { normalizeHeading(it) }
        var buffer: MutableList<String>? = null
        var sectionLevel = 0

        for (line in lines) {
            val headingMatch = Regex("^(#+)\\s*(.+?)\\s*$").find(line)
            if (headingMatch != null) {
                val level = headingMatch.groupValues[1].length
                val heading = normalizeHeading(headingMatch.groupValues[2])

                if (buffer != null && level <= sectionLevel) {
                    break
                }

                if (normalizedHeadings.contains(heading)) {
                    buffer = mutableListOf()
                    sectionLevel = level
                    continue
                }
            }

            buffer?.add(line)
        }

        val section = buffer?.joinToString("\n")?.trim()
        return if (section.isNullOrEmpty()) null else section
    }

    /**
     * Normalize heading for comparison
     */
    private fun normalizeHeading(heading: String): String {
        return heading
            .lowercase()
            .replace(Regex("[*`:#]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * Extract list items from content
     */
    protected fun extractListItems(content: String, limit: Int): List<String> {
        return content.split("\n")
            .map { it.trim() }
            .filter { it.startsWith("-") }
            .map { it.removePrefix("-").trim() }
            .filter { it.isNotEmpty() && !it.matches(Regex("^[-|]+$")) }
            .take(limit)
    }

    /**
     * Read file with default fallback
     */
    protected suspend fun readFileOrDefault(path: String, default: String = "(文件尚未创建)"): String {
        return try {
            java.io.File(path).readText()
        } catch (e: Exception) {
            default
        }
    }

    /**
     * Parse sections from architect output
     */
    protected fun parseArchitectSections(content: String): Map<String, String> {
        val sectionRegex = Regex("(?m)^\\s{0,3}(?:#{1,6}\\s*)?===\\s*SECTION\\s*[：:]\\s*([^\\n=]+?)\\s*===\\s*(?:#+\\s*)?$")
        val matches = sectionRegex.findAll(content).toList()
        val sections = mutableMapOf<String, String>()

        if (matches.isNotEmpty()) {
            for (i in matches.indices) {
                val match = matches[i]
                val name = normalizeSectionName(match.groupValues[1])
                val start = match.range.last + 1
                val end = if (i + 1 < matches.size) matches[i + 1].range.first else content.length
                val sectionContent = content.substring(start, end).trim()
                sections[name] = sectionContent
            }
        }

        return sections
    }

    /**
     * Normalize section name
     */
    private fun normalizeSectionName(name: String): String {
        return name.trim()
            .lowercase()
            .replace(Regex("[`\"'*_]"), " ")
            .replace(Regex("[^a-z0-9]+"), "_")
            .replace(Regex("^_+|_+$"), "")
    }
}
