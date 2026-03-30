package dev.fourco.xye.persistence.repository

import dev.fourco.xye.persistence.dao.ReplayDao
import dev.fourco.xye.persistence.entity.ReplayEntity

class ReplayRepository(private val dao: ReplayDao) {

    suspend fun save(
        packId: String,
        levelId: String,
        inputsJson: String,
        totalTicks: Long,
    ): Long {
        return dao.insert(
            ReplayEntity(
                packId = packId,
                levelId = levelId,
                inputsJson = inputsJson,
                totalTicks = totalTicks,
                recordedAt = System.currentTimeMillis(),
            )
        )
    }

    suspend fun bestReplay(packId: String, levelId: String): ReplayEntity? =
        dao.bestReplay(packId, levelId)

    suspend fun delete(id: Long) = dao.delete(id)
}
