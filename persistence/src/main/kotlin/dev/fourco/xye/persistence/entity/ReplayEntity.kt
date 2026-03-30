package dev.fourco.xye.persistence.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "replays")
data class ReplayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packId: String,
    val levelId: String,
    val inputsJson: String,
    val totalTicks: Long,
    val recordedAt: Long,
)
