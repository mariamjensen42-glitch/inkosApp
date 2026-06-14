package com.example.data.play

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * PlayDB - Database for the Play system.
 */

@Entity(tableName = "play_entities")
data class PlayEntityEntity(
    @PrimaryKey val id: String,
    val type: String,
    val label: String,
    val summary: String = "",
    val status: String = "",
    @ColumnInfo(name = "created_event") val createdEventId: String? = null,
    @ColumnInfo(name = "updated_event") val updatedEventId: String? = null
)

@Entity(tableName = "play_edges")
data class PlayEdgeEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "from_id") val fromId: String,
    val type: String,
    @ColumnInfo(name = "to_id") val toId: String,
    @ColumnInfo(name = "value_json") val valueJson: String = "{}",
    @ColumnInfo(name = "valid_from_event") val validFromEventId: String,
    @ColumnInfo(name = "valid_until_event") val validUntilEventId: String? = null,
    @ColumnInfo(name = "source_event_id") val sourceEventId: String,
    @ColumnInfo(name = "visibility_json") val visibilityJson: String = "{}",
    val strength: Double? = null,
    val confidence: Double? = null
)

@Entity(tableName = "play_state_slots")
data class PlayStateSlotEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "owner_entity_id") val ownerEntityId: String? = null,
    val kind: String,
    val label: String,
    @ColumnInfo(name = "value_json") val valueJson: String,
    @ColumnInfo(name = "updated_event") val updatedEventId: String
)

@Entity(tableName = "play_events")
data class PlayEventEntity(
    @PrimaryKey val id: String,
    val turn: Int,
    @ColumnInfo(name = "action_kind") val actionKind: String,
    @ColumnInfo(name = "raw_input") val rawInput: String,
    @ColumnInfo(name = "outcome_summary") val outcomeSummary: String = "",
    @ColumnInfo(name = "created_at") val createdAt: Long = 0
)

@Dao
interface PlayEntityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntity(entity: PlayEntityEntity)

    @Query("SELECT * FROM play_entities WHERE id = :id")
    suspend fun getEntity(id: String): PlayEntityEntity?

    @Query("SELECT * FROM play_entities ORDER BY id")
    suspend fun getAllEntities(): List<PlayEntityEntity>

    @Query("DELETE FROM play_entities")
    suspend fun deleteAllEntities()
}

@Dao
interface PlayEdgeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEdge(edge: PlayEdgeEntity)

    @Query("UPDATE play_edges SET valid_until_event = :validUntilEventId WHERE id = :edgeId")
    suspend fun expireEdge(edgeId: String, validUntilEventId: String)

    @Query("SELECT * FROM play_edges WHERE (from_id = :entityId OR to_id = :entityId) AND valid_until_event IS NULL ORDER BY type, id")
    suspend fun getCurrentEdgesForEntity(entityId: String): List<PlayEdgeEntity>

    @Query("SELECT * FROM play_edges ORDER BY id")
    suspend fun getAllEdges(): List<PlayEdgeEntity>

    @Query("DELETE FROM play_edges")
    suspend fun deleteAllEdges()
}

@Dao
interface PlayStateSlotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStateSlot(slot: PlayStateSlotEntity)

    @Query("SELECT * FROM play_state_slots WHERE owner_entity_id = :entityId ORDER BY kind, label, id")
    suspend fun getStateSlotsForEntity(entityId: String): List<PlayStateSlotEntity>

    @Query("SELECT * FROM play_state_slots ORDER BY id")
    suspend fun getAllStateSlots(): List<PlayStateSlotEntity>

    @Query("DELETE FROM play_state_slots")
    suspend fun deleteAllStateSlots()
}

@Dao
interface PlayEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun recordEvent(event: PlayEventEntity)

    @Query("SELECT * FROM play_events WHERE id = :id")
    suspend fun getEvent(id: String): PlayEventEntity?

    @Query("SELECT * FROM play_events ORDER BY turn, id")
    suspend fun getAllEvents(): List<PlayEventEntity>

    @Query("DELETE FROM play_events")
    suspend fun deleteAllEvents()
}

@Database(
    entities = [PlayEntityEntity::class, PlayEdgeEntity::class, PlayStateSlotEntity::class, PlayEventEntity::class],
    version = 1,
    exportSchema = false
)
abstract class PlayDatabase : RoomDatabase() {
    abstract fun playEntityDao(): PlayEntityDao
    abstract fun playEdgeDao(): PlayEdgeDao
    abstract fun playStateSlotDao(): PlayStateSlotDao
    abstract fun playEventDao(): PlayEventDao

    companion object {
        @Volatile
        private var INSTANCE: PlayDatabase? = null

        fun getDatabase(context: Context, runId: String): PlayDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PlayDatabase::class.java,
                    "play_db_$runId"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

class PlayDB(private val context: Context, private val runId: String) {
    private val database = PlayDatabase.getDatabase(context, runId)
    private val entityDao = database.playEntityDao()
    private val edgeDao = database.playEdgeDao()
    private val stateSlotDao = database.playStateSlotDao()
    private val eventDao = database.playEventDao()

    suspend fun upsertEntity(entity: PlayEntityEntity) = withContext(Dispatchers.IO) {
        entityDao.upsertEntity(entity)
    }

    suspend fun getEntity(id: String): PlayEntityEntity? = withContext(Dispatchers.IO) {
        entityDao.getEntity(id)
    }

    suspend fun getAllEntities(): List<PlayEntityEntity> = withContext(Dispatchers.IO) {
        entityDao.getAllEntities()
    }

    suspend fun upsertEdge(edge: PlayEdgeEntity) = withContext(Dispatchers.IO) {
        edgeDao.upsertEdge(edge)
    }

    suspend fun getCurrentEdgesForEntity(entityId: String): List<PlayEdgeEntity> = withContext(Dispatchers.IO) {
        edgeDao.getCurrentEdgesForEntity(entityId)
    }

    suspend fun recordEvent(event: PlayEventEntity) = withContext(Dispatchers.IO) {
        eventDao.recordEvent(event)
    }

    suspend fun close() {
        database.close()
    }
}
