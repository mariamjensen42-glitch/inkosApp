package com.example.data.play

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * PlayDB - Database for the Play system.
 *
 * This is the Kotlin Android equivalent of the TypeScript PlayDB class.
 * It manages entities, edges, state slots, and events for interactive fiction.
 */

// Entity definitions

@Entity(tableName = "play_entities")
data class PlayEntityEntity(
    @PrimaryKey val id: String,
    val type: String, // "character", "location", "item", "clue", "evidence", "claim", etc.
    val label: String,
    val summary: String = "",
    val status: String = "",
    val createdEventId: String? = null,
    val updatedEventId: String? = null
)

@Entity(tableName = "play_edges")
data class PlayEdgeEntity(
    @PrimaryKey val id: String,
    val fromId: String,
    val type: String,
    val toId: String,
    val valueJson: String = "{}",
    val validFromEventId: String,
    val validUntilEventId: String? = null,
    val sourceEventId: String,
    val visibilityJson: String = "{}",
    val strength: Double? = null,
    val confidence: Double? = null
)

@Entity(tableName = "play_state_slots")
data class PlayStateSlotEntity(
    @PrimaryKey val id: String,
    val ownerEntityId: String? = null,
    val kind: String, // "attribute", "status", "inventory", etc.
    val label: String,
    val valueJson: String,
    val updatedEventId: String
)

@Entity(tableName = "play_events")
data class PlayEventEntity(
    @PrimaryKey val id: String,
    val turn: Int,
    val actionKind: String, // "narrate", "dialogue", "action", "system", etc.
    val rawInput: String,
    val outcomeSummary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

// Data classes for domain models

data class PlayEntity(
    val id: String,
    val type: String,
    val label: String,
    val summary: String = "",
    val status: String = "",
    val createdEventId: String? = null,
    val updatedEventId: String? = null
)

data class PlayEdge(
    val id: String,
    val fromId: String,
    val type: String,
    val toId: String,
    val value: Map<String, Any?> = emptyMap(),
    val validFromEventId: String,
    val validUntilEventId: String? = null,
    val sourceEventId: String,
    val visibility: Map<String, Any?> = emptyMap(),
    val strength: Double? = null,
    val confidence: Double? = null
)

data class PlayStateSlot(
    val id: String,
    val ownerEntityId: String? = null,
    val kind: String,
    val label: String,
    val value: Any? = null,
    val updatedEventId: String
)

data class PlayEvent(
    val id: String,
    val turn: Int,
    val actionKind: String,
    val rawInput: String,
    val outcomeSummary: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

data class PlayGraphSnapshot(
    val entities: List<PlayEntity>,
    val edges: List<PlayEdge>,
    val stateSlots: List<PlayStateSlot>,
    val events: List<PlayEvent>
)

// DAO interfaces

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

    @Query("""
        SELECT * FROM play_edges 
        WHERE (from_id = :entityId OR to_id = :entityId) 
        AND valid_until_event IS NULL 
        ORDER BY type, id
    """)
    suspend fun getCurrentEdgesForEntity(entityId: String): List<PlayEdgeEntity>

    @Query("""
        SELECT pe.* FROM play_entities pe
        INNER JOIN play_edges pe2 ON pe.id = pe2.from_id
        WHERE pe2.to_id = :claimId
        AND pe2.type = 'supports'
        AND pe2.valid_until_event IS NULL
        AND pe.type IN ('evidence', 'clue')
        ORDER BY COALESCE(pe2.strength, 0) DESC, pe.id ASC
    """)
    suspend fun getEvidenceForClaim(claimId: String): List<PlayEntityEntity>

    @Query("SELECT * FROM play_edges ORDER BY id")
    suspend fun getAllEdges(): List<PlayEdgeEntity>

    @Query("DELETE FROM play_edges")
    suspend fun deleteAllEdges()
}

@Dao
interface PlayStateSlotDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertStateSlot(slot: PlayStateSlotEntity)

    @Query("""
        SELECT * FROM play_state_slots 
        WHERE owner_entity_id = :entityId 
        ORDER BY kind, label, id
    """)
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

// Database

@Database(
    entities = [
        PlayEntityEntity::class,
        PlayEdgeEntity::class,
        PlayStateSlotEntity::class,
        PlayEventEntity::class
    ],
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
 * PlayDB - Main class for interacting with the Play database.
 */
class PlayDB(private val context: Context, private val runId: String) {
    private val database = PlayDatabase.getDatabase(context, runId)
    private val entityDao = database.playEntityDao()
    private val edgeDao = database.playEdgeDao()
    private val stateSlotDao = database.playStateSlotDao()
    private val eventDao = database.playEventDao()

    suspend fun upsertEntity(entity: PlayEntity) = withContext(Dispatchers.IO) {
        val dbEntity = PlayEntityEntity(
            id = entity.id,
            type = entity.type,
            label = entity.label,
            summary = entity.summary,
            status = entity.status,
            createdEventId = entity.createdEventId,
            updatedEventId = entity.updatedEventId
        )
        entityDao.upsertEntity(dbEntity)
    }

    suspend fun getEntity(id: String): PlayEntity? = withContext(Dispatchers.IO) {
        entityDao.getEntity(id)?.let { dbEntity ->
            PlayEntity(
                id = dbEntity.id,
                type = dbEntity.type,
                label = dbEntity.label,
                summary = dbEntity.summary,
                status = dbEntity.status,
                createdEventId = dbEntity.createdEventId,
                updatedEventId = dbEntity.updatedEventId
            )
        }
    }

    suspend fun upsertEdge(edge: PlayEdge) = withContext(Dispatchers.IO) {
        val dbEdge = PlayEdgeEntity(
            id = edge.id,
            fromId = edge.fromId,
            type = edge.type,
            toId = edge.toId,
            valueJson = org.json.JSONObject(edge.value).toString(),
            validFromEventId = edge.validFromEventId,
            validUntilEventId = edge.validUntilEventId,
            sourceEventId = edge.sourceEventId,
            visibilityJson = org.json.JSONObject(edge.visibility).toString(),
            strength = edge.strength,
            confidence = edge.confidence
        )
        edgeDao.upsertEdge(dbEdge)
    }

    suspend fun expireEdge(edgeId: String, validUntilEventId: String) = withContext(Dispatchers.IO) {
        edgeDao.expireEdge(edgeId, validUntilEventId)
    }

    suspend fun getCurrentEdgesForEntity(entityId: String): List<PlayEdge> = withContext(Dispatchers.IO) {
        edgeDao.getCurrentEdgesForEntity(entityId).map { dbEdge ->
            PlayEdge(
                id = dbEdge.id,
                fromId = dbEdge.fromId,
                type = dbEdge.type,
                toId = dbEdge.toId,
                value = parseJsonObject(dbEdge.valueJson),
                validFromEventId = dbEdge.validFromEventId,
                validUntilEventId = dbEdge.validUntilEventId,
                sourceEventId = dbEdge.sourceEventId,
                visibility = parseJsonObject(dbEdge.visibilityJson),
                strength = dbEdge.strength,
                confidence = dbEdge.confidence
            )
        }
    }

    suspend fun getEvidenceForClaim(claimId: String): List<PlayEntity> = withContext(Dispatchers.IO) {
        edgeDao.getEvidenceForClaim(claimId).map { dbEntity ->
            PlayEntity(
                id = dbEntity.id,
                type = dbEntity.type,
                label = dbEntity.label,
                summary = dbEntity.summary,
                status = dbEntity.status,
                createdEventId = dbEntity.createdEventId,
                updatedEventId = dbEntity.updatedEventId
            )
        }
    }

    suspend fun upsertStateSlot(slot: PlayStateSlot) = withContext(Dispatchers.IO) {
        val dbSlot = PlayStateSlotEntity(
            id = slot.id,
            ownerEntityId = slot.ownerEntityId,
            kind = slot.kind,
            label = slot.label,
            valueJson = org.json.JSONObject(mapOf("value" to slot.value)).toString(),
            updatedEventId = slot.updatedEventId
        )
        stateSlotDao.upsertStateSlot(dbSlot)
    }

    suspend fun getStateSlotsForEntity(entityId: String): List<PlayStateSlot> = withContext(Dispatchers.IO) {
        stateSlotDao.getStateSlotsForEntity(entityId).map { dbSlot ->
            PlayStateSlot(
                id = dbSlot.id,
                ownerEntityId = dbSlot.ownerEntityId,
                kind = dbSlot.kind,
                label = dbSlot.label,
                value = parseJsonObject(dbSlot.valueJson)["value"],
                updatedEventId = dbSlot.updatedEventId
            )
        }
    }

    suspend fun recordEvent(event: PlayEvent) = withContext(Dispatchers.IO) {
        val dbEvent = PlayEventEntity(
            id = event.id,
            turn = event.turn,
            actionKind = event.actionKind,
            rawInput = event.rawInput,
            outcomeSummary = event.outcomeSummary,
            createdAt = event.createdAt
        )
        eventDao.recordEvent(dbEvent)
    }

    suspend fun getEvent(id: String): PlayEvent? = withContext(Dispatchers.IO) {
        eventDao.getEvent(id)?.let { dbEvent ->
            PlayEvent(
                id = dbEvent.id,
                turn = dbEvent.turn,
                actionKind = dbEvent.actionKind,
                rawInput = dbEvent.rawInput,
                outcomeSummary = dbEvent.outcomeSummary,
                createdAt = dbEvent.createdAt
            )
        }
    }

    suspend fun snapshot(): PlayGraphSnapshot = withContext(Dispatchers.IO) {
        val entities = entityDao.getAllEntities().map { dbEntity ->
            PlayEntity(
                id = dbEntity.id,
                type = dbEntity.type,
                label = dbEntity.label,
                summary = dbEntity.summary,
                status = dbEntity.status,
                createdEventId = dbEntity.createdEventId,
                updatedEventId = dbEntity.updatedEventId
            )
        }

        val edges = edgeDao.getAllEdges().map { dbEdge ->
            PlayEdge(
                id = dbEdge.id,
                fromId = dbEdge.fromId,
                type = dbEdge.type,
                toId = dbEdge.toId,
                value = parseJsonObject(dbEdge.valueJson),
                validFromEventId = dbEdge.validFromEventId,
                validUntilEventId = dbEdge.validUntilEventId,
                sourceEventId = dbEdge.sourceEventId,
                visibility = parseJsonObject(dbEdge.visibilityJson),
                strength = dbEdge.strength,
                confidence = dbEdge.confidence
            )
        }

        val stateSlots = stateSlotDao.getAllStateSlots().map { dbSlot ->
            PlayStateSlot(
                id = dbSlot.id,
                ownerEntityId = dbSlot.ownerEntityId,
                kind = dbSlot.kind,
                label = dbSlot.label,
                value = parseJsonObject(dbSlot.valueJson)["value"],
                updatedEventId = dbSlot.updatedEventId
            )
        }

        val events = eventDao.getAllEvents().map { dbEvent ->
            PlayEvent(
                id = dbEvent.id,
                turn = dbEvent.turn,
                actionKind = dbEvent.actionKind,
                rawInput = dbEvent.rawInput,
                outcomeSummary = dbEvent.outcomeSummary,
                createdAt = dbEvent.createdAt
            )
        }

        PlayGraphSnapshot(
            entities = entities,
            edges = edges,
            stateSlots = stateSlots,
            events = events
        )
    }

    suspend fun replaceWithSnapshot(snapshot: PlayGraphSnapshot) = withContext(Dispatchers.IO) {
        // Clear all tables
        stateSlotDao.deleteAllStateSlots()
        edgeDao.deleteAllEdges()
        entityDao.deleteAllEntities()
        eventDao.deleteAllEvents()

        // Insert new data
        snapshot.events.forEach { event ->
            recordEvent(event)
        }
        snapshot.entities.forEach { entity ->
            upsertEntity(entity)
        }
        snapshot.edges.forEach { edge ->
            upsertEdge(edge)
        }
        snapshot.stateSlots.forEach { slot ->
            upsertStateSlot(slot)
        }
    }

    suspend fun close() {
        database.close()
    }

    private fun parseJsonObject(json: String): Map<String, Any?> {
        return try {
            val jsonObject = org.json.JSONObject(json)
            val map = mutableMapOf<String, Any?>()
            jsonObject.keys().forEach { key ->
                map[key] = jsonObject.get(key)
            }
            map
        } catch (e: Exception) {
            emptyMap()
        }
    }
}
