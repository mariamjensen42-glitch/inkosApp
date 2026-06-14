package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Repository
import com.example.data.agent.AgentSession
import com.example.data.agent.AgentSessionConfig
import com.example.data.agent.AgentTool
import com.example.data.interaction.*
import com.example.data.performance.*
import com.example.data.testing.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Extended ViewModel with new module support
 */
class MainViewModelExtended(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application)
    private val stateManager = com.example.data.state.StateManager(application, application.filesDir)
    private val performanceMonitor = PerformanceMonitor(application)
    private val memoryManager = MemoryManager()
    private val backgroundTaskManager = BackgroundTaskManager()

    // Session management
    private val _sessions = MutableStateFlow<List<BookSession>>(emptyList())
    val sessions: StateFlow<List<BookSession>> = _sessions.asStateFlow()

    private val _activeSession = MutableStateFlow<BookSession?>(null)
    val activeSession: StateFlow<BookSession?> = _activeSession.asStateFlow()

    // Agent tools
    private val _agentTools = MutableStateFlow<List<AgentTool>>(emptyList())
    val agentTools: StateFlow<List<AgentTool>> = _agentTools.asStateFlow()

    private val _selectedTool = MutableStateFlow<AgentTool?>(null)
    val selectedTool: StateFlow<AgentTool?> = _selectedTool.asStateFlow()

    // Performance monitoring
    private val _memoryStatus = MutableStateFlow(memoryManager.checkMemoryUsage())
    val memoryStatus: StateFlow<MemoryStatus> = _memoryStatus.asStateFlow()

    private val _cacheStats = MutableStateFlow(CacheStats())
    val cacheStats: StateFlow<CacheStats> = _cacheStats.asStateFlow()

    private val _activeTasks = MutableStateFlow(backgroundTaskManager.getActiveTaskCount())
    val activeTasks: StateFlow<Int> = _activeTasks.asStateFlow()

    private val _queuedTasks = MutableStateFlow(backgroundTaskManager.getQueuedTaskCount())
    val queuedTasks: StateFlow<Int> = _queuedTasks.asStateFlow()

    // Testing
    private val _testResults = MutableStateFlow(TestSuite(
        name = "InkOS Conversion Tests",
        results = emptyList(),
        totalTests = 0,
        passedTests = 0,
        failedTests = 0,
        totalDuration = 0
    ))
    val testResults: StateFlow<TestSuite> = _testResults.asStateFlow()

    private val _isRunningTests = MutableStateFlow(false)
    val isRunningTests: StateFlow<Boolean> = _isRunningTests.asStateFlow()

    init {
        // Initialize agent tools
        initializeAgentTools()
        
        // Start performance monitoring
        startPerformanceMonitoring()
    }

    private fun initializeAgentTools() {
        val tools = mutableListOf<AgentTool>()
        tools.add(com.example.data.agent.createReadTool(getApplication<Application>().filesDir))
        tools.add(com.example.data.agent.createWriteTool(getApplication<Application>().filesDir))
        tools.add(com.example.data.agent.createGrepTool(getApplication<Application>().filesDir))
        tools.add(com.example.data.agent.createLsTool(getApplication<Application>().filesDir))
        _agentTools.value = tools
    }

    private fun startPerformanceMonitoring() {
        viewModelScope.launch {
            while (true) {
                _memoryStatus.value = memoryManager.checkMemoryUsage()
                _activeTasks.value = backgroundTaskManager.getActiveTaskCount()
                _queuedTasks.value = backgroundTaskManager.getQueuedTaskCount()
                kotlinx.coroutines.delay(5000) // Update every 5 seconds
            }
        }
    }

    // Session management methods
    fun createSession() {
        viewModelScope.launch {
            val store = BookSessionStore(getApplication(), getApplication<Application>().filesDir)
            val session = store.createAndPersistBookSession(null)
            refreshSessions()
            _activeSession.value = session
        }
    }

    fun selectSession(sessionId: String) {
        viewModelScope.launch {
            val store = BookSessionStore(getApplication(), getApplication<Application>().filesDir)
            val session = store.loadBookSession(sessionId)
            _activeSession.value = session
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            val store = BookSessionStore(getApplication(), getApplication<Application>().filesDir)
            store.deleteBookSession(sessionId)
            refreshSessions()
            if (_activeSession.value?.sessionId == sessionId) {
                _activeSession.value = null
            }
        }
    }

    fun refreshSessions() {
        viewModelScope.launch {
            val store = BookSessionStore(getApplication(), getApplication<Application>().filesDir)
            val sessionsList = store.listBookSessions(null)
            _sessions.value = sessionsList.map { summary ->
                store.loadBookSession(summary.sessionId) ?: createBookSession(
                    bookId = summary.bookId,
                    sessionId = summary.sessionId,
                    sessionKind = summary.sessionKind,
                    playMode = summary.playMode
                )
            }
        }
    }

    // Agent tools methods
    fun selectTool(toolName: String) {
        _selectedTool.value = _agentTools.value.find { it.name == toolName }
    }

    fun executeTool(toolName: String, args: Map<String, Any?>) {
        viewModelScope.launch {
            val tool = _agentTools.value.find { it.name == toolName }
            tool?.execute(args)
        }
    }

    // Performance methods
    fun clearCaches() {
        // Clear various caches
        _cacheStats.value = CacheStats()
    }

    fun forceGarbageCollection() {
        System.gc()
        _memoryStatus.value = memoryManager.checkMemoryUsage()
    }

    // Testing methods
    fun runTests() {
        _isRunningTests.value = true
        viewModelScope.launch {
            try {
                val testRunner = TestRunner(getApplication())
                val results = testRunner.runAllTests()
                _testResults.value = results
            } catch (e: Exception) {
                // Handle test failure
                _testResults.value = TestSuite(
                    name = "InkOS Conversion Tests",
                    results = listOf(TestResult(
                        testName = "Test Runner",
                        passed = false,
                        duration = 0,
                        error = e.message
                    )),
                    totalTests = 1,
                    passedTests = 0,
                    failedTests = 1,
                    totalDuration = 0
                )
            } finally {
                _isRunningTests.value = false
            }
        }
    }

    // Extended book creation
    fun createBook(
        title: String,
        genre: String,
        platform: String,
        blurb: String,
        targetChapters: Int,
        chapterWordCount: Int
    ) {
        viewModelScope.launch {
            repository.createBook(
                title = title,
                type = "NOVEL",
                genre = genre,
                brief = blurb
            )
        }
    }

    // Extended chapter methods
    fun writeNextChapter() {
        val book = _activeBook.value ?: return
        viewModelScope.launch {
            repository.planChapter(book.id, book.currentChapterIndex, "")
        }
    }

    fun selectChapter(chapterId: Long) {
        viewModelScope.launch {
            val chapter = repository.getChapterById(chapterId)
            _activeChapter.value = chapter
        }
    }

    // Play system methods
    fun startPlay() {
        val book = _activeBook.value ?: return
        viewModelScope.launch {
            repository.executePlayAction(book.id, "start")
        }
    }

    fun resetPlay() {
        // Reset play state
        _playState.value = null
    }

    // Short fiction methods
    fun generateShortFiction() {
        val book = _activeBook.value ?: return
        viewModelScope.launch {
            repository.runShortStoryPipeline(book.id, "", 1)
        }
    }

    // Export methods
    fun exportBook() {
        // Export book
    }

    // Config methods
    fun saveConfig() {
        // Save configuration
    }

    // Private state holders for extended functionality
    private val _activeBook = MutableStateFlow<com.example.data.local.BookEntity?>(null)
    val activeBook: StateFlow<com.example.data.local.BookEntity?> = _activeBook.asStateFlow()

    private val _activeChapter = MutableStateFlow<com.example.data.local.ChapterEntity?>(null)
    val activeChapter: StateFlow<com.example.data.local.ChapterEntity?> = _activeChapter.asStateFlow()

    private val _playState = MutableStateFlow<com.example.data.local.PlayStateEntity?>(null)
    val playState: StateFlow<com.example.data.local.PlayStateEntity?> = _playState.asStateFlow()

    private val _selectedProvider = MutableStateFlow("GEMINI")
    val selectedProvider: StateFlow<String> = _selectedProvider.asStateFlow()

    private val _deepSeekKey = MutableStateFlow("")
    val deepSeekKey: StateFlow<String> = _deepSeekKey.asStateFlow()

    private val _deepSeekBaseUrl = MutableStateFlow("https://api.deepseek.com")
    val deepSeekBaseUrl: StateFlow<String> = _deepSeekBaseUrl.asStateFlow()

    private val _deepSeekModel = MutableStateFlow("deepseek-v4-flash")
    val deepSeekModel: StateFlow<String> = _deepSeekModel.asStateFlow()

    private val _xiaomiMimoKey = MutableStateFlow("")
    val xiaomiMimoKey: StateFlow<String> = _xiaomiMimoKey.asStateFlow()

    private val _xiaomiMimoBaseUrl = MutableStateFlow("https://api-ai.xiaomi.com/v1")
    val xiaomiMimoBaseUrl: StateFlow<String> = _xiaomiMimoBaseUrl.asStateFlow()

    private val _xiaomiMimoModel = MutableStateFlow("mimo-v2-pro")
    val xiaomiMimoModel: StateFlow<String> = _xiaomiMimoModel.asStateFlow()

    private val _currentTemperature = MutableStateFlow(1.0f)
    val currentTemperature: StateFlow<Float> = _currentTemperature.asStateFlow()

    fun selectBook(bookId: String) {
        viewModelScope.launch {
            val book = repository.getBookById(bookId.toLong())
            _activeBook.value = book
        }
    }

    fun updateProvider(provider: String) {
        _selectedProvider.value = provider
    }

    fun updateDeepSeekKey(key: String) {
        _deepSeekKey.value = key
    }

    fun updateDeepSeekBaseUrl(url: String) {
        _deepSeekBaseUrl.value = url
    }

    fun updateDeepSeekModel(model: String) {
        _deepSeekModel.value = model
    }

    fun updateXiaomiMimoKey(key: String) {
        _xiaomiMimoKey.value = key
    }

    fun updateXiaomiMimoBaseUrl(url: String) {
        _xiaomiMimoBaseUrl.value = url
    }

    fun updateXiaomiMimoModel(model: String) {
        _xiaomiMimoModel.value = model
    }

    fun updateTemperature(temp: Float) {
        _currentTemperature.value = temp
    }
}

// Data class for cache stats
data class CacheStats(
    val size: Int = 0,
    val hitRate: Double = 0.0
)
