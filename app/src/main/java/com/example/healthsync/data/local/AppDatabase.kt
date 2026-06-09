package com.example.healthsync.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "sync_queue")
data class QueuedBatchEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val payloadJson: String,
    val createdAt: Long,
    val attempts: Int = 0,
    val lastError: String? = null,
)

@Entity(tableName = "sync_log")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val success: Boolean,
    val message: String,
    val itemsSent: Int,
)

@Dao
interface SyncQueueDao {
    @Insert
    suspend fun enqueue(batch: QueuedBatchEntity): Long

    @Query("SELECT * FROM sync_queue ORDER BY createdAt ASC LIMIT :limit")
    suspend fun next(limit: Int = 5): List<QueuedBatchEntity>

    @Query("DELETE FROM sync_queue WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("UPDATE sync_queue SET attempts = attempts + 1, lastError = :error WHERE id = :id")
    suspend fun markFailed(id: Long, error: String)

    @Query("SELECT COUNT(*) FROM sync_queue")
    fun queueSize(): Flow<Int>
}

@Dao
interface SyncLogDao {
    @Insert
    suspend fun insert(log: SyncLogEntity)

    @Query("SELECT * FROM sync_log ORDER BY timestamp DESC LIMIT 50")
    fun recent(): Flow<List<SyncLogEntity>>

    @Query("SELECT * FROM sync_log WHERE success = 1 ORDER BY timestamp DESC LIMIT 1")
    fun lastSuccess(): Flow<SyncLogEntity?>
}

@Database(
    entities = [QueuedBatchEntity::class, SyncLogEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun syncQueueDao(): SyncQueueDao
    abstract fun syncLogDao(): SyncLogDao
}
