package dev.fourco.xye.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.fourco.xye.persistence.entity.ProgressEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProgressDao {
    @Query("SELECT * FROM progress WHERE packId = :packId ORDER BY levelId")
    fun forPack(packId: String): Flow<List<ProgressEntity>>

    @Query("SELECT * FROM progress WHERE packId = :packId AND levelId = :levelId")
    suspend fun get(packId: String, levelId: String): ProgressEntity?

    @Upsert
    suspend fun upsert(progress: ProgressEntity)

    @Query("SELECT COUNT(*) FROM progress WHERE packId = :packId AND status IN ('completed', 'perfected')")
    suspend fun completedCount(packId: String): Int
}
