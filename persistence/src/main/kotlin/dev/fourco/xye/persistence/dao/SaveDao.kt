package dev.fourco.xye.persistence.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import dev.fourco.xye.persistence.entity.SaveEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SaveDao {
    @Query("SELECT * FROM saves WHERE slotId = :slotId")
    suspend fun get(slotId: String): SaveEntity?

    @Upsert
    suspend fun upsert(save: SaveEntity)

    @Query("DELETE FROM saves WHERE slotId = :slotId")
    suspend fun delete(slotId: String)

    @Query("SELECT * FROM saves ORDER BY savedAt DESC")
    fun allSaves(): Flow<List<SaveEntity>>
}
