package com.example.data.state

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MemoryDB - Temporal memory database for InkOS truth files.
 *
 * This is the Kotlin Android equivalent of the TypeScript MemoryDB class.
 * It stores facts with temporal validity (valid_from/valid_until chapter numbers),
 * enabling precise queries like "what did character X know in chapter 5?"
 *
 * Uses Room database for Android compatibility.
 */

// Entity definitions

@Entity(tableName = "facts")
data class FactEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val predicate: String,
    @ColumnInfo(name = "object_value") val objectValue: String,
    @ColumnInfo(name = "valid_from_chapter") val validFromChapter: Int,
    @ColumnInfo(name = "valid_until_chapter") val validUntilChapter: Int? = null,
    @ColumnInfo(name = "source_chapter") val sourceChapter: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long = 0
)

@Entity(tableName = "chapter_summaries")
data class ChapterSummaryEntity(
    @PrimaryKey val chapter: Int,
    val title: String,
    val characters: String = "",
    val events: String = "",
    @ColumnInfo(name = "state_changes") val stateChanges: String = "",
    @ColumnInfo(name = "hook_activity") val hookActivity: String = "",
    val mood: String = "",
    @ColumnInfo(name = "chapter_type") val chapterType: String = ""
)

@Entity(tableName = "memory_hooks")
data class MemoryHookEntity(
    @PrimaryKey @ColumnInfo(name = "hook_id") val hookId: String,
    @ColumnInfo(name = "start_chapter") val startChapter: Int = 0,
    val type: String = "",
    val status: String = "open",
    @ColumnInfo(name = "last_advanced_chapter") val lastAdvancedChapter: Int = 0,
    @ColumnInfo(name = "expected_payoff") val expectedPayoff: String = "",
    @ColumnInfo(name = "payoff_timing") val payoffTiming: String = "",
    val notes: String = "",
    @ColumnInfo(name = "depends_on") val dependsOn: String = "", // JSON array of hook IDs
    @ColumnInfo(name = "pays_off_in_arc") val paysOffInArc: String = "",
    @ColumnInfo(name = "core_hook") val coreHook: Boolean = false,
    @ColumnInfo(name = "half_life_chapters") val halfLifeChapters: Int? = null,
    @ColumnInfo(name = "advanced_count") val advancedCount: Int = 0,
    val promoted: Boolean = false
)

// Data classes for domain models

data class Fact(
    val id: Long? = null,
    val subject: String,
    val predicate: String,
    val objectValue: String,
    val validFromChapter: Int,
    val validUntilChapter: Int? = null,
    val sourceChapter: Int
)

data class StoredSummary(
    val chapter: Int,
    val title: String,
    val characters: String,
    val events: String,
    val stateChanges: String,
    val hookActivity: String,
    val mood: String,
    val chapterType: String
)

data class StoredHook(
    val hookId: String,
    val startChapter: Int,
    val type: String,
    val status: String,
    val lastAdvancedChapter: Int,
    val expectedPayoff: String,
    val payoffTiming: String? = null,
    val notes: String,
    val dependsOn: List<String> = emptyList(),
    val paysOffInArc: String? = null,
    val coreHook: Boolean = false,
    val halfLifeChapters: Int? = null,
    val advancedCount: Int = 0,
    val promoted: Boolean = false
)

// DAO interfaces

@Dao
interface FactDao {
    @Query("""
        SELECT id, subject, predicate, object_value AS objectValue, 
               valid_from_chapter AS validFromChapter, 
               valid_until_chapter AS validUntilChapter,
               source_chapter AS sourceChapter
        FROM facts 
        WHERE valid_until_chapter IS NULL 
        ORDER BY subject, predicate
    """)
    suspend fun getCurrentFacts(): List<Fact>

    @Query("""
        SELECT id, subject, predicate, object_value AS objectValue, 
               valid_from_chapter AS validFromChapter, 
               valid_until_chapter AS validUntilChapter,
               source_chapter AS sourceChapter
        FROM facts 
        WHERE subject = :subject 
        AND valid_from_chapter <= :chapter 
        AND (valid_until_chapter IS NULL OR valid_until_chapter > :chapter)
        ORDER BY predicate
    """)
    suspend fun getFactsAt(subject: String, chapter: Int): List<Fact>

    @Query("""
        SELECT id, subject, predicate, object_value AS objectValue, 
               valid_from_chapter AS validFromChapter, 
               valid_until_chapter AS validUntilChapter,
               source_chapter AS sourceChapter
        FROM facts 
        WHERE subject = :subject 
        ORDER BY valid_from_chapter
    """)
    suspend fun getFactHistory(subject: String): List<Fact>

    @Query("""
        SELECT id, subject, predicate, object_value AS objectValue, 
               valid_from_chapter AS validFromChapter, 
               valid_until_chapter AS validUntilChapter,
               source_chapter AS sourceChapter
        FROM facts 
        WHERE predicate = :predicate 
        AND valid_until_chapter IS NULL
        ORDER BY subject
    """)
    suspend fun getFactsByPredicate(predicate: String): List<Fact>

    @Insert
    suspend fun insertFact(fact: FactEntity): Long

    @Query("UPDATE facts SET valid_until_chapter = :untilChapter WHERE id = :id")
    suspend fun invalidateFact(id: Long, untilChapter: Int)

    @Query("DELETE FROM facts WHERE valid_until_chapter IS NULL")
    suspend fun deleteCurrentFacts()

    @Query("DELETE FROM facts")
    suspend fun deleteAllFacts()

    @Insert
    suspend fun insertFacts(facts: List<FactEntity>)
}

@Dao
interface ChapterSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: ChapterSummaryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummaries(summaries: List<ChapterSummaryEntity>)

    @Query("DELETE FROM chapter_summaries")
    suspend fun deleteAllSummaries()

    @Query("""
        SELECT chapter, title, characters, events, 
               state_changes AS stateChanges, hook_activity AS hookActivity,
               mood, chapter_type AS chapterType
        FROM chapter_summaries 
        WHERE chapter >= :fromChapter AND chapter <= :toChapter 
        ORDER BY chapter
    """)
    suspend fun getSummaries(fromChapter: Int, toChapter: Int): List<StoredSummary>

    @Query("SELECT COUNT(*) FROM chapter_summaries")
    suspend fun getChapterCount(): Int

    @Query("""
        SELECT chapter, title, characters, events, 
               state_changes AS stateChanges, hook_activity AS hookActivity,
               mood, chapter_type AS chapterType
        FROM chapter_summaries 
        ORDER BY chapter DESC 
        LIMIT :count
    """)
    suspend fun getRecentSummaries(count: Int): List<StoredSummary>
}

@Dao
interface MemoryHookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHook(hook: MemoryHookEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHooks(hooks: List<MemoryHookEntity>)

    @Query("DELETE FROM memory_hooks")
    suspend fun deleteAllHooks()

    @Query("""
        SELECT hook_id AS hookId, start_chapter AS startChapter, type, status,
               last_advanced_chapter AS lastAdvancedChapter,
               expected_payoff AS expectedPayoff, payoff_timing AS payoffTiming,
               notes, depends_on AS dependsOn, pays_off_in_arc AS paysOffInArc,
               core_hook AS coreHook, half_life_chapters AS halfLifeChapters,
               advanced_count AS advancedCount, promoted
        FROM memory_hooks 
        WHERE LOWER(status) NOT IN ('resolved', 'closed', '已回收', '已解决')
        ORDER BY last_advanced_chapter DESC, start_chapter DESC, hook_id ASC
    """)
    suspend fun getActiveHooks(): List<StoredHook>
}

// Database

@Database(
    entities = [
        FactEntity::class,
        ChapterSummaryEntity::class,
        MemoryHookEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MemoryDatabase : RoomDatabase() {
    abstract fun factDao(): FactDao
    abstract fun chapterSummaryDao(): ChapterSummaryDao
    abstract fun memoryHookDao(): MemoryHookDao

    companion object {
        @Volatile
        private var INSTANCE: MemoryDatabase? = null

        fun getDatabase(context: Context, bookId: String): MemoryDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MemoryDatabase::class.java,
                    "memory_db_$bookId"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * MemoryDB - Main class for interacting with the temporal memory database.
 */
class MemoryDB(private val context: Context, private val bookId: String) {
    private val database = MemoryDatabase.getDatabase(context, bookId)
    private val factDao = database.factDao()
    private val summaryDao = database.chapterSummaryDao()
    private val hookDao = database.memoryHookDao()

    // Facts (temporal)

    suspend fun addFact(fact: Fact): Long = withContext(Dispatchers.IO) {
        val entity = FactEntity(
            subject = fact.subject,
            predicate = fact.predicate,
            objectValue = fact.objectValue,
            validFromChapter = fact.validFromChapter,
            validUntilChapter = fact.validUntilChapter,
            sourceChapter = fact.sourceChapter
        )
        factDao.insertFact(entity)
    }

    suspend fun invalidateFact(id: Long, untilChapter: Int) = withContext(Dispatchers.IO) {
        factDao.invalidateFact(id, untilChapter)
    }

    suspend fun getCurrentFacts(): List<Fact> = withContext(Dispatchers.IO) {
        factDao.getCurrentFacts()
    }

    suspend fun getFactsAt(subject: String, chapter: Int): List<Fact> = withContext(Dispatchers.IO) {
        factDao.getFactsAt(subject, chapter)
    }

    suspend fun getFactHistory(subject: String): List<Fact> = withContext(Dispatchers.IO) {
        factDao.getFactHistory(subject)
    }

    suspend fun getFactsByPredicate(predicate: String): List<Fact> = withContext(Dispatchers.IO) {
        factDao.getFactsByPredicate(predicate)
    }

    suspend fun getFactsForCharacters(names: List<String>): List<Fact> = withContext(Dispatchers.IO) {
        if (names.isEmpty()) return@withContext emptyList()
        val allFacts = factDao.getCurrentFacts()
        allFacts.filter { fact -> names.contains(fact.subject) }
    }

    suspend fun replaceCurrentFacts(facts: List<Fact>) = withContext(Dispatchers.IO) {
        factDao.deleteCurrentFacts()
        facts.forEach { fact ->
            addFact(fact)
        }
    }

    suspend fun resetFacts() = withContext(Dispatchers.IO) {
        factDao.deleteAllFacts()
    }

    // Chapter summaries

    suspend fun upsertSummary(summary: StoredSummary) = withContext(Dispatchers.IO) {
        val entity = ChapterSummaryEntity(
            chapter = summary.chapter,
            title = summary.title,
            characters = summary.characters,
            events = summary.events,
            stateChanges = summary.stateChanges,
            hookActivity = summary.hookActivity,
            mood = summary.mood,
            chapterType = summary.chapterType
        )
        summaryDao.upsertSummary(entity)
    }

    suspend fun replaceSummaries(summaries: List<StoredSummary>) = withContext(Dispatchers.IO) {
        summaryDao.deleteAllSummaries()
        summaries.forEach { summary ->
            upsertSummary(summary)
        }
    }

    suspend fun getSummaries(fromChapter: Int, toChapter: Int): List<StoredSummary> = withContext(Dispatchers.IO) {
        summaryDao.getSummaries(fromChapter, toChapter)
    }

    suspend fun getSummariesByCharacters(names: List<String>): List<StoredSummary> = withContext(Dispatchers.IO) {
        if (names.isEmpty()) return@withContext emptyList()
        val allSummaries = summaryDao.getRecentSummaries(1000) // Get all summaries
        allSummaries.filter { summary ->
            names.any { name -> summary.characters.contains(name) }
        }
    }

    suspend fun getChapterCount(): Int = withContext(Dispatchers.IO) {
        summaryDao.getChapterCount()
    }

    suspend fun getRecentSummaries(count: Int): List<StoredSummary> = withContext(Dispatchers.IO) {
        summaryDao.getRecentSummaries(count)
    }

    // Hooks

    suspend fun upsertHook(hook: StoredHook) = withContext(Dispatchers.IO) {
        val entity = MemoryHookEntity(
            hookId = hook.hookId,
            startChapter = hook.startChapter,
            type = hook.type,
            status = hook.status,
            lastAdvancedChapter = hook.lastAdvancedChapter,
            expectedPayoff = hook.expectedPayoff,
            payoffTiming = hook.payoffTiming ?: "",
            notes = hook.notes,
            dependsOn = org.json.JSONArray(hook.dependsOn).toString(),
            paysOffInArc = hook.paysOffInArc ?: "",
            coreHook = hook.coreHook,
            halfLifeChapters = hook.halfLifeChapters,
            advancedCount = hook.advancedCount,
            promoted = hook.promoted
        )
        hookDao.upsertHook(entity)
    }

    suspend fun replaceHooks(hooks: List<StoredHook>) = withContext(Dispatchers.IO) {
        hookDao.deleteAllHooks()
        hooks.forEach { hook ->
            upsertHook(hook)
        }
    }

    suspend fun getActiveHooks(): List<StoredHook> = withContext(Dispatchers.IO) {
        hookDao.getActiveHooks()
    }

    suspend fun close() {
        database.close()
    }
}
