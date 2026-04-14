package com.katafract.safeopen.data

import android.content.Context
import androidx.room.Database
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Dao
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val rawValue: String,
    val payloadType: String,
    val riskLevel: String, // "LOW", "CAUTION", "HIGH", "UNKNOWN"
    val riskScore: Int,
    val resolvedUrl: String?,
    val redirectCount: Int,
    val scannedAt: Long = System.currentTimeMillis(),
    val source: String, // "QR_CODE", "PASTED_TEXT", "SHARED_LINK"
    val signalsJson: String = "[]", // JSON array of threat signal strings
    val summary: String = ""
)

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY scannedAt DESC")
    fun getAllScans(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY scannedAt DESC LIMIT :limit")
    fun getRecentScans(limit: Int): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history WHERE id = :id")
    suspend fun getScanById(id: Long): ScanHistoryEntity?

    @Insert
    suspend fun insert(scan: ScanHistoryEntity): Long

    @Delete
    suspend fun delete(scan: ScanHistoryEntity)

    @Query("DELETE FROM scan_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM scan_history")
    suspend fun count(): Int
}

@Database(entities = [ScanHistoryEntity::class], version = 1, exportSchema = false)
abstract class ScanHistoryDatabase : RoomDatabase() {
    abstract fun dao(): ScanHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: ScanHistoryDatabase? = null

        fun getInstance(context: Context): ScanHistoryDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ScanHistoryDatabase::class.java,
                    "scan_history.db"
                ).build().also { INSTANCE = it }
            }
    }
}
