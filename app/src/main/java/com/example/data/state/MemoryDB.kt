package com.example.data.state

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MemoryDB - Temporal memory database for InkOS truth files.
 */

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
    @ColumnInfo(name = "depends_on") val dependsOn: String = "",
    @ColumnInfo(name = "pays_off_in_arc") val paysOffInArc: String = "",
    @ColumnInfo(name = "core_hook") val coreHook: Boolean = false,
    @ColumnInfo(name = "half_life_chapters") val halfLifeChapters: Int? = null,
    @ColumnInfo(name = "advanced_count") val advancedCount: Int = 0,
    val promoted: Boolean = false
)

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
    val dependsOn: String = "",
    val paysOffInArc: String? = null,
    val coreHook: Boolean = false,
    val halfLifeChapters: Int? = null,
    val advancedCount: Int = 0,
    val promoted: Boolean = false
)

@Dao
interface FactDao {
    @Query("SELECT * FROM facts WHERE valid_until_chapter IS NULL ORDER BY subject, predicate")
    suspend fun getCurrentFacts(): List<FactEntity>

    @Insert
    suspend fun insertFact(fact: FactEntity): Long

    @Query("UPDATE facts SET valid_until_chapter = :untilChapter WHERE id = :id")
    suspend fun invalidateFact(id: Long, untilChapter: Int)

    @Query("DELETE FROM facts WHERE valid_until_chapter IS NULL")
    suspend fun deleteCurrentFacts()
}

@Dao
interface ChapterSummaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSummary(summary: ChapterSummaryEntity)

    @Query("DELETE FROM chapter_summaries")
    suspend fun deleteAllSummaries()

    @Query("SELECT * FROM chapter_summaries WHERE chapter >= :fromChapter AND chapter <= :toChapter ORDER BY chapter")
    suspend fun getSummaries(fromChapter: Int, toChapter: Int): List<ChapterSummaryEntity>

    @Query("SELECT COUNT(*) FROM chapter_summaries")
    suspend fun getChapterCount(): Int
}

@Dao
interface MemoryHookDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHook(hook: MemoryHookEntity)

    @Query("DELETE FROM memory_hooks")
    suspend fun deleteAllHooks()

    @Query("SELECT * FROM memory_hooks WHERE LOWER(status) NOT IN ('resolved', 'closed', '已回收', '已解决') ORDER BY last_advanced_chapter DESC, start_chapter DESC, hook_id ASC")
    suspend fun getActiveHooks(): List<MemoryHookEntity>
}

@Database(
    entities = [FactEntity::class, ChapterSummaryEntity::class, MemoryHookEntity::class],
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
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class MemoryDB(private val context: Context, private val bookId: String) {
    private val database = MemoryDatabase.getDatabase(context, bookId)
    private val factDao = database.factDao()
    private val summaryDao = database.chapterSummaryDao()
    private val hookDao = database.memoryHookDao()

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

    suspend fun getCurrentFacts(): List<Fact> = withContext(Dispatchers.IO) {
        factDao.getCurrentFacts().map { entity ->
            Fact(
                id = entity.id,
                subject = entity.subject,
                predicate = entity.predicate,
                objectValue = entity.objectValue,
                validFromChapter = entity.validFromChapter,
                validUntilChapter = entity.validUntilChapter,
                sourceChapter = entity.sourceChapter
            )
        }
    }

    suspend fun getActiveHooks(): List<StoredHook> = withContext(Dispatchers.IO) {
        hookDao.getActiveHooks().map { entity ->
            StoredHook(
                hookId = entity.hookId,
                startChapter = entity.startChapter,
                type = entity.type,
                status = entity.status,
                lastAdvancedChapter = entity.lastAdvancedChapter,
                expectedPayoff = entity.expectedPayoff,
                payoffTiming = entity.payoffTiming,
                notes = entity.notes,
                dependsOn = entity.dependsOn,
                paysOffInArc = entity.paysOffInArc,
                coreHook = entity.coreHook,
                halfLifeChapters = entity.halfLifeChapters,
                advancedCount = entity.advancedCount,
                promoted = entity.promoted
            )
        }
    }

    suspend fun close() {
        database.close()
    }
}
