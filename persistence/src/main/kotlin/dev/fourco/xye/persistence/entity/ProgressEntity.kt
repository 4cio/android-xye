package dev.fourco.xye.persistence.entity

import androidx.room.Entity

@Entity(tableName = "progress", primaryKeys = ["packId", "levelId"])
data class ProgressEntity(
    val packId: String,
    val levelId: String,
    val status: String,
    val bestTicks: Long? = null,
    val completedAt: Long? = null,
)
