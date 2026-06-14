package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.Repository
import com.example.data.api.GeminiService
import com.example.data.api.LlmPreferences
import com.example.data.api.LlmRouter
import com.example.data.local.BookEntity
import com.example.data.local.ChapterEntity
import com.example.data.local.PlayStateEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

enum class Screen {
    BOOKS_LIST,
    CREATE_BOOK,
    NOVEL_WORKSPACE,
    SHORT_WORKSPACE,
    PLAY_WORKSPACE,
    MODEL_CONFIG
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = Repository(application)

    // Navigation / Screen State
    private val _currentScreen = MutableStateFlow(Screen.BOOKS_LIST)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    // Books state
    private val _books = MutableStateFlow<List<BookEntity>>(emptyList())
    val books: StateFlow<List<BookEntity>> = _books.asStateFlow()

    private val _activeBook = MutableStateFlow<BookEntity?>(null)
    val activeBook: StateFlow<BookEntity?> = _activeBook.asStateFlow()

    // active chapters (for Novels)
    private val _chapters = MutableStateFlow<List<ChapterEntity>>(emptyList())
    val chapters: StateFlow<List<ChapterEntity>> = _chapters.asStateFlow()

    private val _activeChapter = MutableStateFlow<ChapterEntity?>(null)
    val activeChapter: StateFlow<ChapterEntity?> = _activeChapter.asStateFlow()

    // Play sandbox state (for InkOS Play)
    private val _playState = MutableStateFlow<PlayStateEntity?>(null)
    val playState: StateFlow<PlayStateEntity?> = _playState.asStateFlow()

    // Loading states for each AI task
    val isApiKeyAvailable: Boolean
        get() = LlmRouter.isApiKeyAvailable

    val selectedProvider = MutableStateFlow("GEMINI")
    val deepSeekKey = MutableStateFlow("")
    val deepSeekBaseUrl = MutableStateFlow("https://api.deepseek.com")
    val deepSeekModel = MutableStateFlow("deepseek-v4-flash")
    val xiaomiMimoKey = MutableStateFlow("")
    val xiaomiMimoBaseUrl = MutableStateFlow("https://api-ai.xiaomi.com/v1")
    val xiaomiMimoModel = MutableStateFlow("mimo-v2-pro")
    val currentTemperature = MutableStateFlow(1.0f)

    fun updateProvider(prov: String) {
        LlmPreferences.provider = prov
        selectedProvider.value = prov
    }

    fun updateDeepSeekKey(key: String) {
        LlmPreferences.deepSeekKey = key
        deepSeekKey.value = key
    }

    fun updateDeepSeekBaseUrl(url: String) {
        LlmPreferences.deepSeekBaseUrl = url
        deepSeekBaseUrl.value = url
    }

    fun updateDeepSeekModel(model: String) {
        LlmPreferences.deepSeekModel = model
        deepSeekModel.value = model
    }

    fun updateXiaomiMimoKey(key: String) {
        LlmPreferences.xiaomiMimoKey = key
        xiaomiMimoKey.value = key
    }

    fun updateXiaomiMimoBaseUrl(url: String) {
        LlmPreferences.xiaomiMimoBaseUrl = url
        xiaomiMimoBaseUrl.value = url
    }

    fun updateXiaomiMimoModel(model: String) {
        LlmPreferences.xiaomiMimoModel = model
        xiaomiMimoModel.value = model
    }

    fun updateTemperature(temp: Float) {
        LlmPreferences.temperature = temp
        currentTemperature.value = temp
    }

    private val _isPlanning = MutableStateFlow(false)
    val isPlanning: StateFlow<Boolean> = _isPlanning.asStateFlow()

    private val _isComposing = MutableStateFlow(false)
    val isComposing: StateFlow<Boolean> = _isComposing.asStateFlow()

    private val _isAuditing = MutableStateFlow(false)
    val isAuditing: StateFlow<Boolean> = _isAuditing.asStateFlow()

    private val _isRevising = MutableStateFlow(false)
    val isRevising: StateFlow<Boolean> = _isRevising.asStateFlow()

    private val _isGeneratingShort = MutableStateFlow(false)
    val isGeneratingShort: StateFlow<Boolean> = _isGeneratingShort.asStateFlow()

    private val _isPlayLoading = MutableStateFlow(false)
    val isPlayLoading: StateFlow<Boolean> = _isPlayLoading.asStateFlow()

    private val _isArchitecting = MutableStateFlow(false)
    val isArchitecting: StateFlow<Boolean> = _isArchitecting.asStateFlow()

    // Model Config test helpers
    private val _testConnectionResult = MutableStateFlow<String?>(null)
    val testConnectionResult: StateFlow<String?> = _testConnectionResult.asStateFlow()

    private val _isTestingConnection = MutableStateFlow(false)
    val isTestingConnection: StateFlow<Boolean> = _isTestingConnection.asStateFlow()

    init {
        LlmPreferences.init(application)
        selectedProvider.value = LlmPreferences.provider
        deepSeekKey.value = LlmPreferences.deepSeekKey
        deepSeekBaseUrl.value = LlmPreferences.deepSeekBaseUrl
        deepSeekModel.value = LlmPreferences.deepSeekModel
        xiaomiMimoKey.value = LlmPreferences.xiaomiMimoKey
        xiaomiMimoBaseUrl.value = LlmPreferences.xiaomiMimoBaseUrl
        xiaomiMimoModel.value = LlmPreferences.xiaomiMimoModel
        currentTemperature.value = LlmPreferences.temperature

        // Collect books automatically from Room Flow
        viewModelScope.launch {
            repository.getAllBooks().collectLatest {
                _books.value = it
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
        _testConnectionResult.value = null
    }

    fun selectBook(book: BookEntity) {
        _activeBook.value = book
        _activeChapter.value = null

        // Collect chapters reactively if it is a Novel
        if (book.type == "NOVEL") {
            viewModelScope.launch {
                repository.getChaptersFlow(book.id).collectLatest {
                    _chapters.value = it
                    // Select chapter 1 by default if empty
                    if (it.isNotEmpty() && _activeChapter.value == null) {
                        _activeChapter.value = it.firstOrNull { c -> c.chapterIndex == book.currentChapterIndex - 1 } ?: it.first()
                    }
                }
            }
            navigateTo(Screen.NOVEL_WORKSPACE)
        } else if (book.type == "SHORT") {
            navigateTo(Screen.SHORT_WORKSPACE)
        } else if (book.type == "PLAY") {
            // Collect open world states reactively
            viewModelScope.launch {
                repository.getPlayStateFlow(book.id).collectLatest {
                    _playState.value = it
                }
            }
            navigateTo(Screen.PLAY_WORKSPACE)
        }
    }

    fun selectChapter(chapter: ChapterEntity) {
        _activeChapter.value = chapter
    }

    // --- Book Generation triggers ---
    fun createBook(title: String, type: String, genre: String, brief: String) {
        _isArchitecting.value = true
        viewModelScope.launch {
            try {
                val bookId = repository.createBook(title, type, genre, brief)
                val newBook = repository.getBookById(bookId)
                if (newBook != null) {
                    selectBook(newBook)
                } else {
                    navigateTo(Screen.BOOKS_LIST)
                }
            } finally {
                _isArchitecting.value = false
            }
        }
    }

    fun redesignFoundation(customBrief: String) {
        val book = _activeBook.value ?: return
        _isArchitecting.value = true
        viewModelScope.launch {
            try {
                val updated = repository.redesignFoundation(book.id, customBrief)
                if (updated != null) {
                    _activeBook.value = updated
                }
            } finally {
                _isArchitecting.value = false
            }
        }
    }

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            repository.deleteBook(book)
            if (_activeBook.value?.id == book.id) {
                _activeBook.value = null
                _activeChapter.value = null
            }
        }
    }

    // --- InkOS Novel Workflows ---
    fun planNextChapter(userFocus: String) {
        val book = _activeBook.value ?: return
        val currentIdx = book.currentChapterIndex
        _isPlanning.value = true
        viewModelScope.launch {
            try {
                repository.planChapter(book.id, currentIdx, userFocus)
                // Re-sync chapter
                val list = repository.getChaptersFlow(book.id)
                // select newly generated chapter
                val latest = repository.getChaptersFlow(book.id)
                // Just let Flow updates flow naturally
            } finally {
                _isPlanning.value = false
            }
        }
    }

    fun composeChapter() {
        val book = _activeBook.value ?: return
        val chapter = _activeChapter.value ?: return
        _isComposing.value = true
        viewModelScope.launch {
            try {
                repository.composeChapter(book.id, chapter.chapterIndex)
                val updatedChapter = repository.getChapterById(chapter.id)
                if (updatedChapter != null) {
                    _activeChapter.value = updatedChapter
                }
            } finally {
                _isComposing.value = false
            }
        }
    }

    fun auditChapter() {
        val book = _activeBook.value ?: return
        val chapter = _activeChapter.value ?: return
        _isAuditing.value = true
        viewModelScope.launch {
            try {
                repository.auditChapter(book.id, chapter.chapterIndex)
                val updatedChapter = repository.getChapterById(chapter.id)
                if (updatedChapter != null) {
                    _activeChapter.value = updatedChapter
                }
                val updatedBook = repository.getBookById(book.id)
                if (updatedBook != null) {
                    _activeBook.value = updatedBook
                }
            } finally {
                _isAuditing.value = false
            }
        }
    }

    fun reviseChapter() {
        val book = _activeBook.value ?: return
        val chapter = _activeChapter.value ?: return
        _isRevising.value = true
        viewModelScope.launch {
            try {
                repository.reviseChapter(book.id, chapter.chapterIndex)
                // update current active book
                val updated = repository.getBookById(book.id)
                if (updated != null) {
                    _activeBook.value = updated
                }
            } finally {
                _isRevising.value = false
            }
        }
    }

    fun rollbackChapter(index: Int) {
        val book = _activeBook.value ?: return
        viewModelScope.launch {
            repository.rollbackChapter(book.id, index)
            val updated = repository.getBookById(book.id)
            if (updated != null) {
                _activeBook.value = updated
            }
        }
    }

    // --- InkOS Short Story Run workflow ---
    fun runShortStory(direction: String, chapterWords: Int, targetChapters: Int) {
        val book = _activeBook.value ?: return
        _isGeneratingShort.value = true
        viewModelScope.launch {
            try {
                // Update book model values first
                val updatedBookConfig = book.copy(
                    brief = direction,
                    targetWordsPerChapter = chapterWords,
                    totalChapters = targetChapters
                )
                repository.updateBook(updatedBookConfig)
                _activeBook.value = updatedBookConfig

                repository.runShortStoryPipeline(book.id, direction, targetChapters)

                // Refresh book reference
                val finalBook = repository.getBookById(book.id)
                if (finalBook != null) {
                    _activeBook.value = finalBook
                }
            } finally {
                _isGeneratingShort.value = false
            }
        }
    }

    // --- InkOS Play open world triggers ---
    fun executePlayAction(userActionStr: String) {
        val book = _activeBook.value ?: return
        _isPlayLoading.value = true
        viewModelScope.launch {
            try {
                repository.executePlayAction(book.id, userActionStr)
            } finally {
                _isPlayLoading.value = false
            }
        }
    }

    // --- Connection Testing ---
    fun testConnection() {
        _isTestingConnection.value = true
        _testConnectionResult.value = null
        viewModelScope.launch {
            try {
                val available = LlmRouter.isApiKeyAvailable
                val providerLabel = LlmRouter.currentProviderLabel
                if (!available) {
                    _testConnectionResult.value = "⚠️ $providerLabel API key is missing or set to placeholder. Proceeding with safe simulation engine fallback!"
                } else {
                    val result = LlmRouter.generateContent(
                        "You are a connection doctor.",
                        "Direct connection test. Respond with: 'Success! InkOS is fully online.'",
                        requireJson = false
                    )
                    _testConnectionResult.value = "✅ Connected Successfully via $providerLabel:\n$result"
                }
            } catch (e: Exception) {
                _testConnectionResult.value = "❌ Connection Failed: ${e.message}"
            } finally {
                _isTestingConnection.value = false
            }
        }
    }
}
