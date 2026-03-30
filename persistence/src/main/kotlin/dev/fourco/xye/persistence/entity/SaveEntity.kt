package dev.fourco.xye.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saves")
data class SaveEntity(
    @PrimaryKey val slotId: String,
    val levelId: String,
    val packId: String,
    val tick: Long,
    val stateJson: String,
    val undoJson: String,
    val savedAt: Long,
)
