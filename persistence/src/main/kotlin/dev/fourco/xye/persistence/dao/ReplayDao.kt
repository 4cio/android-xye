package dev.fourco.xye.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import dev.fourco.xye.persistence.entity.ReplayEntity

@Dao
interface ReplayDao {
    @Insert
    suspend fun insert(replay: ReplayEntity): Long

    @Query("SELECT * FROM replays WHERE packId = :packId AND levelId = :levelId ORDER BY totalTicks ASC LIMIT 1")
    suspend fun bestReplay(packId: String, levelId: String): ReplayEntity?

    @Query("DELETE FROM replays WHERE id = :id")
    suspend fun delete(id: Long)
}
