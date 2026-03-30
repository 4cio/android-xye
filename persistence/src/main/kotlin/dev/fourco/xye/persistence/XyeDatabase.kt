package dev.fourco.xye.persistence

import androidx.room.Database
import androidx.room.RoomDatabase
import dev.fourco.xye.persistence.dao.ProgressDao
import dev.fourco.xye.persistence.dao.ReplayDao
import dev.fourco.xye.persistence.dao.SaveDao
import dev.fourco.xye.persistence.entity.ProgressEntity
import dev.fourco.xye.persistence.entity.ReplayEntity
import dev.fourco.xye.persistence.entity.SaveEntity

@Database(
    entities = [SaveEntity::class, ProgressEntity::class, ReplayEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class XyeDatabase : RoomDatabase() {
    abstract fun saveDao(): SaveDao
    abstract fun progressDao(): ProgressDao
    abstract fun replayDao(): ReplayDao
}
