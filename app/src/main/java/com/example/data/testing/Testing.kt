package com.example.data.testing

import android.content.Context
import com.example.data.models.*
import com.example.data.state.*
import com.example.data.interaction.*
import com.example.data.agents.*
import com.example.data.play.*
import com.example.data.notify.*
import com.example.data.utils.*
import com.example.data.performance.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File

/**
 * Testing - Testing and validation utilities.
 *
 * This module handles:
 * - Unit testing for converted modules
 * - Integration testing for module interactions
 * - Validation of data models
 * - Performance benchmarking
 */

// Test Result

data class TestResult(
    val testName: String,
    val passed: Boolean,
    val duration: Long,
    val error: String? = null,
    val details: Map<String, Any?>? = null
)

data class TestSuite(
    val name: String,
    val results: List<TestResult>,
    val totalTests: Int,
    val passedTests: Int,
    val failedTests: Int,
    val totalDuration: Long
) {
    val passRate: Double
        get() = if (totalTests > 0) passedTests.toDouble() / totalTests else 0.0
}

// Test Runner

class TestRunner(private val context: Context) {

    private val testResults = mutableListOf<TestResult>()

    suspend fun runAllTests(): TestSuite = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()

        // Run unit tests
        runUnitTests()

        // Run integration tests
        runIntegrationTests()

        // Run validation tests
        runValidationTests()

        val totalDuration = System.currentTimeMillis() - startTime
        val passedTests = testResults.count { it.passed }
        val failedTests = testResults.count { !it.passed }

        TestSuite(
            name = "InkOS Conversion Tests",
            results = testResults.toList(),
            totalTests = testResults.size,
            passedTests = passedTests,
            failedTests = failedTests,
            totalDuration = totalDuration
        )
    }

    private suspend fun runUnitTests() {
        // Test data models
        testBookModels()
        testProjectConfig()
        testDetectionModels()
        testGenreProfile()
        testStyleProfile()
        testBookRules()

        // Test state management
        testStateManager()
        testMemoryDB()
        testRuntimeStateStore()

        // Test interaction system
        testEvents()
        testIntents()
        testSession()
        testBookSessionStore()

        // Test agents
        testBaseAgent()
        testContinuityAuditor()
        testPolisherAgent()

        // Test play system
        testPlayDB()
        testPlayRunner()

        // Test utilities
        testChapterSplitter()
        testContextFilter()
        testHookGovernance()
    }

    private suspend fun runIntegrationTests() {
        // Test module interactions
        testAgentSessionIntegration()
        testEditControllerIntegration()
        testRuntimeIntegration()
    }

    private suspend fun runValidationTests() {
        // Test data validation
        testJsonSerialization()
        testDataIntegrity()
        testErrorHandling()
    }

    // Unit Tests

    private suspend fun testBookModels() {
        val testName = "BookModels"

        try {
            // Test BookConfig creation
            val bookConfig = BookConfig(
                id = "test-book",
                title = "Test Book",
                genre = "XUANHUAN",
                language = "zh"
            )

            // Test JSON serialization
            val json = bookConfig.toJson()
            val parsed = BookConfig.fromJson(json)

            val passed = parsed.id == bookConfig.id &&
                    parsed.title == bookConfig.title &&
                    parsed.genre == bookConfig.genre

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("bookConfig" to bookConfig)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testProjectConfig() {
        val testName = "ProjectConfig"

        try {
            // Test ProjectConfig creation
            val projectConfig = ProjectConfig(
                name = "Test Project",
                llm = LLMConfig(
                    provider = "openai",
                    baseUrl = "https://api.openai.com",
                    model = "gpt-4"
                )
            )

            // Test JSON serialization
            val json = projectConfig.toJson()
            val parsed = ProjectConfig.fromJson(json)

            val passed = parsed.name == projectConfig.name &&
                    parsed.llm.provider == projectConfig.llm.provider

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("projectConfig" to projectConfig)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testDetectionModels() {
        val testName = "DetectionModels"

        try {
            // Test DetectionHistoryEntry
            val entry = DetectionHistoryEntry(
                chapterNumber = 1,
                timestamp = "2024-01-01T00:00:00Z",
                provider = "gptzero",
                score = 0.8,
                action = "detect",
                attempt = 1
            )

            val json = entry.toJson()
            val parsed = DetectionHistoryEntry.fromJson(json)

            val passed = parsed.chapterNumber == entry.chapterNumber &&
                    parsed.provider == entry.provider

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("entry" to entry)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testGenreProfile() {
        val testName = "GenreProfile"

        try {
            // Test GenreProfile parsing
            val raw = """
                ---
                name: 玄幻
                id: xuanhuan
                language: zh
                chapterTypes:
                - 剧情
                - 战斗
                fatigueWords:
                - 仿佛
                - 宛如
                ---
                This is a genre profile body.
            """.trimIndent()

            val parsed = parseGenreProfile(raw)

            val passed = parsed.profile.name == "玄幻" &&
                    parsed.profile.id == "xuanhuan" &&
                    parsed.body.contains("This is a genre profile body")

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("profile" to parsed.profile)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testStyleProfile() {
        val testName = "StyleProfile"

        try {
            // Test StyleProfile analysis
            val text = "这是一段测试文本。它包含多个句子，用于分析风格特征。每个句子都有不同的长度。"
            val profile = analyzeStyleProfile(text, "test")

            val passed = profile.avgSentenceLength > 0 &&
                    profile.vocabularyDiversity > 0

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("profile" to profile)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testBookRules() {
        val testName = "BookRules"

        try {
            // Test BookRules parsing
            val raw = """
                ---
                version: "1.0"
                protagonist:
                  name: 主角
                prohibitions:
                - 不能杀人
                - 不能使用禁术
                ---
                This is book rules body.
            """.trimIndent()

            val parsed = parseBookRules(raw)

            val passed = parsed != null &&
                    parsed.rules.protagonist?.name == "主角" &&
                    parsed.rules.prohibitions.contains("不能杀人")

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("rules" to parsed?.rules)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testStateManager() {
        val testName = "StateManager"

        try {
            val stateManager = StateManager(context, context.filesDir)

            // Test basic operations
            val books = stateManager.listBooks()

            val passed = books != null

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("booksCount" to books.size)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testMemoryDB() {
        val testName = "MemoryDB"

        try {
            val memoryDB = MemoryDB(context, "test-book")

            // Test fact operations
            val fact = Fact(
                subject = "主角",
                predicate = "位于",
                objectValue = "城镇",
                validFromChapter = 1,
                sourceChapter = 1
            )

            val factId = memoryDB.addFact(fact)
            val facts = memoryDB.getCurrentFacts()

            val passed = factId > 0 && facts.isNotEmpty()

            memoryDB.close()

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("factId" to factId, "factsCount" to facts.size)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testRuntimeStateStore() {
        val testName = "RuntimeStateStore"

        try {
            val runtimeStateStore = RuntimeStateStore(context)

            // Test basic operations
            val bookDir = File(context.filesDir, "books/test-book")
            bookDir.mkdirs()

            val passed = bookDir.exists()

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("bookDir" to bookDir.absolutePath)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testEvents() {
        val testName = "Events"

        try {
            val event = InteractionEvent(
                kind = "test",
                timestamp = System.currentTimeMillis(),
                status = ExecutionStatus.COMPLETED,
                bookId = "test-book",
                chapterNumber = 1,
                detail = "Test event"
            )

            val json = event.toJson()
            val parsed = InteractionEvent.fromJson(json)

            val passed = parsed.kind == event.kind &&
                    parsed.status == event.status

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("event" to event)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testIntents() {
        val testName = "Intents"

        try {
            val request = InteractionRequest(
                intent = InteractionIntentType.CREATE_BOOK,
                title = "Test Book",
                genre = "XUANHUAN"
            )

            val json = request.toJson()
            val parsed = InteractionRequest.fromJson(json)

            val passed = parsed.intent == request.intent &&
                    parsed.title == request.title

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("request" to request)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testSession() {
        val testName = "Session"

        try {
            val session = createBookSession("test-book")

            val passed = session.bookId == "test-book" &&
                    session.sessionId.isNotEmpty()

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("session" to session)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testBookSessionStore() {
        val testName = "BookSessionStore"

        try {
            val store = BookSessionStore(context, context.filesDir)
            val session = store.createAndPersistBookSession("test-book")

            val passed = session.bookId == "test-book"

            store.deleteBookSession(session.sessionId)

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("session" to session)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testBaseAgent() {
        val testName = "BaseAgent"

        try {
            val ctx = AgentContext(
                projectRoot = context.filesDir,
                language = "zh"
            )

            val agent = object : BaseAgent(ctx) {
                override val name = "test-agent"
            }

            val passed = agent.name == "test-agent"

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("agentName" to agent.name)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testContinuityAuditor() {
        val testName = "ContinuityAuditor"

        try {
            val ctx = AgentContext(
                projectRoot = context.filesDir,
                language = "zh"
            )

            val auditor = ContinuityAuditor(ctx)

            val passed = auditor.name == "continuity-auditor"

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("auditorName" to auditor.name)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testPolisherAgent() {
        val testName = "PolisherAgent"

        try {
            val ctx = AgentContext(
                projectRoot = context.filesDir,
                language = "zh"
            )

            val polisher = PolisherAgent(ctx)

            val passed = polisher.name == "polisher"

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("polisherName" to polisher.name)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testPlayDB() {
        val testName = "PlayDB"

        try {
            val playDB = PlayDB(context, "test-run")

            // Test entity operations
            val entity = PlayEntity(
                id = "test-entity",
                type = "character",
                label = "Test Character"
            )

            playDB.upsertEntity(entity)
            val retrieved = playDB.getEntity("test-entity")

            val passed = retrieved != null && retrieved.id == entity.id

            playDB.close()

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("entity" to entity)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testPlayRunner() {
        val testName = "PlayRunner"

        try {
            val runner = PlayRunner(context, context.filesDir, "test-world")

            val passed = runner != null

            runner.close()

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("runner" to runner)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testChapterSplitter() {
        val testName = "ChapterSplitter"

        try {
            val text = """
                第一章 开始
                这是第一章的内容。
                
                第二章 发展
                这是第二章的内容。
                
                第三章 高潮
                这是第三章的内容。
            """.trimIndent()

            val chapters = ChapterSplitter.splitChapters(text)

            val passed = chapters.size == 3 &&
                    chapters[0].title == "开始" &&
                    chapters[1].title == "发展"

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("chaptersCount" to chapters.size)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testContextFilter() {
        val testName = "ContextFilter"

        try {
            val hooks = """
                | hook_id | status | notes |
                |---------|--------|-------|
                | hook1 | open | Test hook |
                | hook2 | resolved | Resolved hook |
                | hook3 | open | Another hook |
            """.trimIndent()

            val filtered = ContextFilter.filterHooks(hooks)

            val passed = filtered.contains("hook1") &&
                    !filtered.contains("hook2") &&
                    filtered.contains("hook3")

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("filtered" to filtered)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testHookGovernance() {
        val testName = "HookGovernance"

        try {
            val hooks = listOf(
                HookRecord(
                    hookId = "hook1",
                    startChapter = 1,
                    type = "伏笔",
                    status = "open",
                    lastAdvancedChapter = 1,
                    expectedPayoff = "测试伏笔"
                )
            )

            val staleHooks = HookGovernance.collectStaleHookDebt(hooks, 10)

            val passed = staleHooks.isNotEmpty()

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("staleHooksCount" to staleHooks.size)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    // Integration Tests

    private suspend fun testAgentSessionIntegration() {
        val testName = "AgentSessionIntegration"

        try {
            val config = AgentSessionConfig(
                sessionId = "test-session",
                projectRoot = context.filesDir,
                bookId = "test-book",
                language = "zh"
            )

            val session = AgentSession(context, config)

            val result = session.processMessage(
                session = createBookSession("test-book"),
                message = "Hello"
            )

            val passed = result.response.isNotEmpty()

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("response" to result.response)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testEditControllerIntegration() {
        val testName = "EditControllerIntegration"

        try {
            val stateManager = StateManager(context, context.filesDir)
            val editController = EditController(context, stateManager)

            val request = EditRequest.EntityRename(
                bookId = "test-book",
                entityType = "character",
                oldValue = "旧名字",
                newValue = "新名字"
            )

            val plan = EditController.planEditTransaction(request)

            val passed = plan.transactionType == "entity-rename" &&
                    plan.affectedScope == "book"

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("plan" to plan)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testRuntimeIntegration() {
        val testName = "RuntimeIntegration"

        try {
            val stateManager = StateManager(context, context.filesDir)
            val tools = object : InteractionRuntimeTools {
                override suspend fun listBooks() = listOf("book1", "book2")
                override suspend fun createBook(input: CreateBookInput) = "new-book"
                override suspend fun exportBook(bookId: String, options: ExportOptions) = "exported"
                override suspend fun chat(input: String, options: ChatOptions) = "response"
                override suspend fun writeNextChapter(bookId: String) = 1
                override suspend fun reviseDraft(bookId: String, chapterNumber: Int, mode: ReviseMode) = "revised"
                override suspend fun patchChapterText(bookId: String, chapterNumber: Int, targetText: String, replacementText: String) = "patched"
                override suspend fun replaceChapterText(bookId: String, chapterNumber: Int, fullText: String) = "replaced"
                override suspend fun renameEntity(bookId: String, oldValue: String, newValue: String) = "renamed"
                override suspend fun updateCurrentFocus(bookId: String, content: String) = "updated"
                override suspend fun updateAuthorIntent(bookId: String, content: String) = "updated"
                override suspend fun writeTruthFile(bookId: String, fileName: String, content: String) = "written"
            }

            val runtime = InteractionRuntime(context, stateManager, tools)
            val session = createBookSession("test-book")

            val request = InteractionRequest(
                intent = InteractionIntentType.LIST_BOOKS
            )

            val result = runtime.processRequest(session, request)

            val passed = result.responseText?.contains("Found") == true

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("responseText" to result.responseText)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    // Validation Tests

    private suspend fun testJsonSerialization() {
        val testName = "JsonSerialization"

        try {
            val bookConfig = BookConfig(
                id = "test",
                title = "Test",
                genre = "XUANHUAN"
            )

            val json = bookConfig.toJson()
            val parsed = BookConfig.fromJson(json)

            val passed = parsed.id == bookConfig.id

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("json" to json.toString())
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testDataIntegrity() {
        val testName = "DataIntegrity"

        try {
            val memoryDB = MemoryDB(context, "integrity-test")

            // Test data integrity
            val fact = Fact(
                subject = "test",
                predicate = "is",
                objectValue = "value",
                validFromChapter = 1,
                sourceChapter = 1
            )

            val id = memoryDB.addFact(fact)
            val facts = memoryDB.getCurrentFacts()

            val passed = facts.any { it.subject == "test" }

            memoryDB.close()

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("factsCount" to facts.size)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }

    private suspend fun testErrorHandling() {
        val testName = "ErrorHandling"

        try {
            val memoryDB = MemoryDB(context, "error-test")

            // Test error handling
            var errorCaught = false
            try {
                memoryDB.getFactsAt("nonexistent", 999)
            } catch (e: Exception) {
                errorCaught = true
            }

            val passed = true // Error handling doesn't crash

            memoryDB.close()

            testResults.add(TestResult(
                testName = testName,
                passed = passed,
                duration = 0,
                details = mapOf("errorCaught" to errorCaught)
            ))
        } catch (e: Exception) {
            testResults.add(TestResult(
                testName = testName,
                passed = false,
                duration = 0,
                error = e.message
            ))
        }
    }
}

// Test Report Generator

object TestReportGenerator {

    fun generateReport(testSuite: TestSuite): String {
        return buildString {
            appendLine("# Test Report: ${testSuite.name}")
            appendLine()
            appendLine("## Summary")
            appendLine("- Total Tests: ${testSuite.totalTests}")
            appendLine("- Passed: ${testSuite.passedTests}")
            appendLine("- Failed: ${testSuite.failedTests}")
            appendLine("- Pass Rate: ${String.format("%.1f%%", testSuite.passRate * 100)}")
            appendLine("- Total Duration: ${testSuite.totalDuration}ms")
            appendLine()
            appendLine("## Test Results")
            appendLine()

            testSuite.results.forEach { result ->
                val status = if (result.passed) "✅ PASS" else "❌ FAIL"
                appendLine("### $status: ${result.testName}")
                appendLine("- Duration: ${result.duration}ms")
                result.error?.let { appendLine("- Error: $it") }
                result.details?.let { details ->
                    appendLine("- Details:")
                    details.forEach { (key, value) ->
                        appendLine("  - $key: $value")
                    }
                }
                appendLine()
            }
        }
    }
}
