package com.example.data.agent

import android.content.Context
import com.example.data.agents.BaseAgent
import com.example.data.models.AgentContext
import com.example.data.interaction.InteractionSession
import com.example.data.interaction.InteractionMessage
import com.example.data.interaction.ExecutionState
import com.example.data.interaction.ExecutionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Agent - Agent module for managing agent sessions and tools.
 *
 * This is the Kotlin Android equivalent of the TypeScript agent module.
 * It handles:
 * - Building agent system prompts
 * - Creating agent tools
 * - Running agent sessions
 * - Managing agent context
 */

// Agent System Prompt

data class AgentSystemPromptConfig(
    val bookId: String? = null,
    val language: String = "zh",
    val automationMode: String = "semi",
    val sessionKind: String? = null,
    val playMode: String? = null
)

/**
 * Build agent system prompt.
 */
fun buildAgentSystemPrompt(config: AgentSystemPromptConfig): String {
    val language = config.language
    val isEnglish = language == "en"

    return if (isEnglish) {
        """
You are InkOS, an AI writing assistant specialized in web fiction.

## Capabilities
- Create and manage books
- Write and revise chapters
- Analyze story continuity
- Manage story state and hooks
- Export books in various formats

## Automation Mode: ${config.automationMode}
- auto: Execute automatically without confirmation
- semi: Ask for confirmation on important actions
- manual: Always ask for confirmation

## Session Kind: ${config.sessionKind ?: "chat"}

${if (config.bookId != null) "## Active Book: ${config.bookId}" else "## No Active Book"}

Remember to:
1. Maintain consistency with existing story elements
2. Follow the author's style and preferences
3. Check for continuity issues
4. Manage hooks and plot threads
5. Provide helpful suggestions
        """.trimIndent()
    } else {
        """
你是 InkOS，一个专注于网络小说的 AI 写作助手。

## 能力
- 创建和管理书籍
- 撰写和修订章节
- 分析故事连续性
- 管理故事状态和伏笔
- 导出各种格式的书籍

## 自动化模式: ${config.automationMode}
- auto: 自动执行，无需确认
- semi: 重要操作需要确认
- manual: 始终需要确认

## 会话类型: ${config.sessionKind ?: "聊天"}

${if (config.bookId != null) "## 当前书籍: ${config.bookId}" else "## 无当前书籍"}

请记住：
1. 与现有故事元素保持一致
2. 遵循作者的风格和偏好
3. 检查连续性问题
4. 管理伏笔和情节线
5. 提供有用的建议
        """.trimIndent()
    }
}

// Agent Tools

interface AgentTool {
    val name: String
    val description: String
    suspend fun execute(args: Map<String, Any?>): Any?
}

data class ToolResult(
    val success: Boolean,
    val data: Any? = null,
    val error: String? = null
)

/**
 * Create a read tool.
 */
fun createReadTool(projectRoot: File): AgentTool {
    return object : AgentTool {
        override val name = "read"
        override val description = "Read file contents"

        override suspend fun execute(args: Map<String, Any?>): ToolResult {
            val path = args["path"] as? String ?: return ToolResult(false, error = "Path is required")
            val file = File(projectRoot, path)

            return try {
                if (!file.exists()) {
                    ToolResult(false, error = "File not found: $path")
                } else {
                    ToolResult(true, data = file.readText())
                }
            } catch (e: Exception) {
                ToolResult(false, error = e.message)
            }
        }
    }
}

/**
 * Create a write tool.
 */
fun createWriteTool(projectRoot: File): AgentTool {
    return object : AgentTool {
        override val name = "write"
        override val description = "Write file contents"

        override suspend fun execute(args: Map<String, Any?>): ToolResult {
            val path = args["path"] as? String ?: return ToolResult(false, error = "Path is required")
            val content = args["content"] as? String ?: return ToolResult(false, error = "Content is required")
            val file = File(projectRoot, path)

            return try {
                file.parentFile?.mkdirs()
                file.writeText(content)
                ToolResult(true, data = "File written: $path")
            } catch (e: Exception) {
                ToolResult(false, error = e.message)
            }
        }
    }
}

/**
 * Create a grep tool.
 */
fun createGrepTool(projectRoot: File): AgentTool {
    return object : AgentTool {
        override val name = "grep"
        override val description = "Search file contents"

        override suspend fun execute(args: Map<String, Any?>): ToolResult {
            val pattern = args["pattern"] as? String ?: return ToolResult(false, error = "Pattern is required")
            val directory = args["directory"] as? String ?: "."
            val dir = File(projectRoot, directory)

            return try {
                val results = mutableListOf<String>()
                dir.walkTopDown().forEach { file ->
                    if (file.isFile) {
                        val content = file.readText()
                        if (content.contains(pattern, ignoreCase = true)) {
                            results.add(file.absolutePath)
                        }
                    }
                }
                ToolResult(true, data = results)
            } catch (e: Exception) {
                ToolResult(false, error = e.message)
            }
        }
    }
}

/**
 * Create an ls tool.
 */
fun createLsTool(projectRoot: File): AgentTool {
    return object : AgentTool {
        override val name = "ls"
        override val description = "List directory contents"

        override suspend fun execute(args: Map<String, Any?>): ToolResult {
            val path = args["path"] as? String ?: "."
            val dir = File(projectRoot, path)

            return try {
                if (!dir.exists()) {
                    ToolResult(false, error = "Directory not found: $path")
                } else {
                    val files = dir.listFiles()?.map { it.name } ?: emptyList()
                    ToolResult(true, data = files)
                }
            } catch (e: Exception) {
                ToolResult(false, error = e.message)
            }
        }
    }
}

// Agent Session

data class AgentSessionConfig(
    val sessionId: String,
    val projectRoot: File,
    val bookId: String? = null,
    val language: String = "zh",
    val automationMode: String = "semi",
    val tools: List<AgentTool> = emptyList()
)

data class AgentSessionResult(
    val session: InteractionSession,
    val response: String,
    val toolCalls: List<ToolCallRecord> = emptyList()
)

data class ToolCallRecord(
    val tool: String,
    val args: Map<String, Any?>,
    val result: Any?,
    val duration: Long
)

/**
 * AgentSession - Main class for managing agent sessions.
 */
class AgentSession(
    private val context: Context,
    private val config: AgentSessionConfig
) {
    private val tools = mutableMapOf<String, AgentTool>()
    private val toolCallHistory = mutableListOf<ToolCallRecord>()

    init {
        // Register default tools
        registerTool(createReadTool(config.projectRoot))
        registerTool(createWriteTool(config.projectRoot))
        registerTool(createGrepTool(config.projectRoot))
        registerTool(createLsTool(config.projectRoot))

        // Register custom tools
        config.tools.forEach { tool ->
            registerTool(tool)
        }
    }

    fun registerTool(tool: AgentTool) {
        tools[tool.name] = tool
    }

    suspend fun processMessage(
        session: InteractionSession,
        message: String
    ): AgentSessionResult = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // Build system prompt
        val systemPrompt = buildAgentSystemPrompt(
            AgentSystemPromptConfig(
                bookId = config.bookId,
                language = config.language,
                automationMode = config.automationMode
            )
        )

        // In a real implementation, this would use an LLM to process the message
        // For now, return a simple response
        val response = "I received your message: $message"

        // Create response message
        val responseMessage = InteractionMessage(
            role = "assistant",
            content = response,
            timestamp = System.currentTimeMillis()
        )

        // Update session
        val updatedSession = session.copy(
            messages = session.messages + listOf(
                InteractionMessage(
                    role = "user",
                    content = message,
                    timestamp = startTime
                ),
                responseMessage
            )
        )

        AgentSessionResult(
            session = updatedSession,
            response = response,
            toolCalls = toolCallHistory.toList()
        )
    }

    suspend fun executeTool(
        toolName: String,
        args: Map<String, Any?>
    ): ToolResult {
        val tool = tools[toolName] ?: return ToolResult(false, error = "Tool not found: $toolName")

        val startTime = System.currentTimeMillis()
        val result = try {
            tool.execute(args)
        } catch (e: Exception) {
            ToolResult(false, error = e.message)
        }
        val duration = System.currentTimeMillis() - startTime

        toolCallHistory.add(
            ToolCallRecord(
                tool = toolName,
                args = args,
                result = result,
                duration = duration
            )
        )

        return result as? ToolResult ?: ToolResult(true, data = result)
    }
}

// Context Transform

data class BookContextTransform(
    val bookId: String,
    val projectRoot: File
) {
    fun transform(context: AgentContext): AgentContext {
        // In a real implementation, this would transform the context based on the book
        return context
    }
}

/**
 * Create book context transform.
 */
fun createBookContextTransform(bookId: String, projectRoot: File): BookContextTransform {
    return BookContextTransform(bookId, projectRoot)
}
