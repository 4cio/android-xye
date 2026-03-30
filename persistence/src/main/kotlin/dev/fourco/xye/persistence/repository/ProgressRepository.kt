package dev.fourco.xye.persistence.repository

import dev.fourco.xye.persistence.dao.ProgressDao
import dev.fourco.xye.persistence.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

class ProgressRepository(private val dao: ProgressDao) {

    fun forPack(packId: String): Flow<List<ProgressEntity>> = dao.forPack(packId)

    suspend fun get(packId: String, levelId: String): ProgressEntity? =
        dao.get(packId, levelId)

    suspend fun completeLevel(packId: String, levelId: String, ticks: Long) {
        val existing = dao.get(packId, levelId)
        dao.upsert(
            ProgressEntity(
                packId = packId,
                levelId = levelId,
                status = "completed",
                bestTicks = minOf(ticks, existing?.bestTicks ?: Long.MAX_VALUE),
                completedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun unlockLevel(packId: String, levelId: String) {
        val existing = dao.get(packId, levelId)
        if (existing == null || existing.status == "locked") {
            dao.upsert(ProgressEntity(packId = packId, levelId = levelId, status = "unlocked"))
        }
    }

    suspend fun completedCount(packId: String): Int = dao.completedCount(packId)
}
