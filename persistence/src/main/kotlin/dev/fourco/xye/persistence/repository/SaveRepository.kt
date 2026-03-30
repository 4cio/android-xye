package dev.fourco.xye.persistence.repository

import dev.fourco.xye.engine.model.GameState
import dev.fourco.xye.persistence.dao.SaveDao
import dev.fourco.xye.persistence.entity.SaveEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SaveRepository(private val dao: SaveDao) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun save(
        slotId: String,
        packId: String,
        state: GameState,
        undoHistory: List<GameState>,
    ) {
        dao.upsert(
            SaveEntity(
                slotId = slotId,
                levelId = state.levelId,
                packId = packId,
                tick = state.tick,
                stateJson = json.encodeToString(state),
                undoJson = json.encodeToString(undoHistory),
                savedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun load(slotId: String): Pair<GameState, List<GameState>>? {
        val entity = dao.get(slotId) ?: return null
        val state = json.decodeFromString<GameState>(entity.stateJson)
        val undo = json.decodeFromString<List<GameState>>(entity.undoJson)
        return state to undo
    }

    suspend fun delete(slotId: String) = dao.delete(slotId)

    fun allSaves(): Flow<List<SaveEntity>> = dao.allSaves()
}
