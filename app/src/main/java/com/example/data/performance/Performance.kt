package com.example.data.performance

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.util.concurrent.ConcurrentHashMap

/**
 * Performance - Performance optimization utilities.
 *
 * This module handles:
 * - Streaming support for LLM responses
 * - Caching mechanisms for frequently accessed data
 * - Memory management for large datasets
 * - Background processing optimization
 */

// Streaming Support

interface StreamCallback {
    fun onToken(token: String)
    fun onComplete(fullText: String)
    fun onError(error: String)
}

data class StreamingConfig(
    val enabled: Boolean = true,
    val bufferSize: Int = 1024,
    val flushInterval: Long = 100 // ms
)

class StreamingManager(private val config: StreamingConfig = StreamingConfig()) {

    private val buffer = StringBuilder()
    private var lastFlushTime = System.currentTimeMillis()

    fun processToken(token: String, callback: StreamCallback) {
        if (!config.enabled) {
            buffer.append(token)
            return
        }

        buffer.append(token)
        callback.onToken(token)

        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFlushTime >= config.flushInterval || buffer.length >= config.bufferSize) {
            flush(callback)
        }
    }

    fun flush(callback: StreamCallback) {
        if (buffer.isNotEmpty()) {
            callback.onComplete(buffer.toString())
            buffer.clear()
            lastFlushTime = System.currentTimeMillis()
        }
    }

    fun reset() {
        buffer.clear()
        lastFlushTime = System.currentTimeMillis()
    }
}

// Caching Mechanisms

data class CacheConfig(
    val maxSize: Int = 1000,
    val ttl: Long = 3600000, // 1 hour in milliseconds
    val cleanupInterval: Long = 300000 // 5 minutes in milliseconds
)

class CacheManager<T>(private val config: CacheConfig = CacheConfig()) {

    private val cache = ConcurrentHashMap<String, CacheEntry<T>>()
    private var lastCleanupTime = System.currentTimeMillis()

    data class CacheEntry<T>(
        val data: T,
        val timestamp: Long = System.currentTimeMillis(),
        var accessCount: Int = 0
    )

    fun get(key: String): T? {
        val entry = cache[key] ?: return null

        // Check if entry has expired
        if (System.currentTimeMillis() - entry.timestamp > config.ttl) {
            cache.remove(key)
            return null
        }

        entry.accessCount++
        return entry.data
    }

    fun put(key: String, data: T) {
        // Cleanup if needed
        cleanupIfNeeded()

        // Remove oldest entries if cache is full
        if (cache.size >= config.maxSize) {
            val oldestKey = cache.entries
                .sortedBy { it.value.timestamp }
                .firstOrNull()?.key
            oldestKey?.let { cache.remove(it) }
        }

        cache[key] = CacheEntry(data)
    }

    fun remove(key: String) {
        cache.remove(key)
    }

    fun clear() {
        cache.clear()
    }

    private fun cleanupIfNeeded() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastCleanupTime >= config.cleanupInterval) {
            cleanup()
            lastCleanupTime = currentTime
        }
    }

    private fun cleanup() {
        val currentTime = System.currentTimeMillis()
        val expiredKeys = cache.entries
            .filter { currentTime - it.value.timestamp > config.ttl }
            .map { it.key }

        expiredKeys.forEach { cache.remove(it) }
    }
}

// Memory Management

data class MemoryConfig(
    val maxMemoryUsage: Long = 100 * 1024 * 1024, // 100 MB
    val warningThreshold: Double = 0.8, // 80%
    val cleanupThreshold: Double = 0.9 // 90%
)

class MemoryManager(private val config: MemoryConfig = MemoryConfig()) {

    private val dataStores = mutableMapOf<String, Any>()

    fun registerDataStore(name: String, store: Any) {
        dataStores[name] = store
    }

    fun unregisterDataStore(name: String) {
        dataStores.remove(name)
    }

    fun checkMemoryUsage(): MemoryStatus {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usagePercentage = usedMemory.toDouble() / maxMemory

        return MemoryStatus(
            usedMemory = usedMemory,
            maxMemory = maxMemory,
            usagePercentage = usagePercentage,
            isWarning = usagePercentage >= config.warningThreshold,
            isCritical = usagePercentage >= config.cleanupThreshold
        )
    }

    fun cleanupIfNeeded() {
        val status = checkMemoryUsage()
        if (status.isCritical) {
            cleanup()
        }
    }

    private fun cleanup() {
        // Clear caches and temporary data
        dataStores.values.forEach { store ->
            when (store) {
                is CacheManager<*> -> store.clear()
            }
        }

        // Suggest garbage collection
        System.gc()
    }
}

data class MemoryStatus(
    val usedMemory: Long,
    val maxMemory: Long,
    val usagePercentage: Double,
    val isWarning: Boolean,
    val isCritical: Boolean
)

// Background Processing

data class BackgroundTaskConfig(
    val maxConcurrentTasks: Int = 3,
    val taskTimeout: Long = 30000 // 30 seconds
)

class BackgroundTaskManager(private val config: BackgroundTaskConfig = BackgroundTaskConfig()) {

    private val activeTasks = ConcurrentHashMap<String, Job>()
    private val taskQueue = mutableListOf<() -> Unit>()

    data class Job(
        val id: String,
        val startTime: Long = System.currentTimeMillis(),
        val task: () -> Unit
    )

    fun submitTask(taskId: String, task: () -> Unit) {
        if (activeTasks.size >= config.maxConcurrentTasks) {
            taskQueue.add(task)
            return
        }

        val job = Job(id = taskId, task = task)
        activeTasks[taskId] = job

        // Execute task in background
        Thread {
            try {
                task()
            } finally {
                activeTasks.remove(taskId)
                processQueue()
            }
        }.start()
    }

    private fun processQueue() {
        if (taskQueue.isNotEmpty() && activeTasks.size < config.maxConcurrentTasks) {
            val task = taskQueue.removeAt(0)
            submitTask("queued_${System.currentTimeMillis()}", task)
        }
    }

    fun cancelTask(taskId: String) {
        activeTasks.remove(taskId)
    }

    fun cancelAllTasks() {
        activeTasks.clear()
        taskQueue.clear()
    }

    fun getActiveTaskCount(): Int = activeTasks.size

    fun getQueuedTaskCount(): Int = taskQueue.size
}

// Performance Monitor

class PerformanceMonitor(private val context: Context) {

    private val metrics = ConcurrentHashMap<String, MutableList<Long>>()

    fun recordMetric(name: String, value: Long) {
        metrics.getOrPut(name) { mutableListOf() }.add(value)
    }

    fun getAverageMetric(name: String): Double {
        val values = metrics[name] ?: return 0.0
        return if (values.isEmpty()) 0.0 else values.average()
    }

    fun getMetricSummary(name: String): MetricSummary {
        val values = metrics[name] ?: return MetricSummary(0, 0.0, 0, 0)
        return MetricSummary(
            count = values.size,
            average = values.average(),
            min = values.minOrNull() ?: 0,
            max = values.maxOrNull() ?: 0
        )
    }

    fun clearMetrics() {
        metrics.clear()
    }

    data class MetricSummary(
        val count: Int,
        val average: Double,
        val min: Long,
        val max: Long
    )
}

// Performance Optimization Utilities

object PerformanceUtils {

    /**
     * Optimize file reading by using buffered streams.
     */
    suspend fun readFileOptimized(file: File): String = withContext(Dispatchers.IO) {
        file.bufferedReader().use { it.readText() }
    }

    /**
     * Optimize file writing by using buffered streams.
     */
    suspend fun writeFileOptimized(file: File, content: String) = withContext(Dispatchers.IO) {
        file.parentFile?.mkdirs()
        file.bufferedWriter().use { it.write(content) }
    }

    /**
     * Batch process items with configurable batch size.
     */
    suspend fun <T, R> batchProcess(
        items: List<T>,
        batchSize: Int = 100,
        processor: suspend (List<T>) -> List<R>
    ): List<R> = withContext(Dispatchers.Default) {
        items.chunked(batchSize).flatMap { batch ->
            processor(batch)
        }
    }

    /**
     * Debounce function calls.
     */
    fun <T> debounce(
        delayMs: Long = 300,
        action: (T) -> Unit
    ): (T) -> Unit {
        var lastCallTime = 0L
        return { param ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastCallTime >= delayMs) {
                action(param)
                lastCallTime = currentTime
            }
        }
    }

    /**
     * Throttle function calls.
     */
    fun <T> throttle(
        intervalMs: Long = 100,
        action: (T) -> Unit
    ): (T) -> Unit {
        var lastCallTime = 0L
        return { param ->
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastCallTime >= intervalMs) {
                action(param)
                lastCallTime = currentTime
            }
        }
    }
}
