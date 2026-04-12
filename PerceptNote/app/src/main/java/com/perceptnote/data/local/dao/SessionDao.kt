// 📄 app/src/main/java/com/perceptnote/data/local/dao/SessionDao.kt
package com.perceptnote.data.local.dao

import androidx.room.*
import com.perceptnote.data.local.entities.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("SELECT * FROM sessions ORDER BY startTimestampMs DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    /**
     * Récupère les sessions dans un rayon géographique.
     * IMPORTANT : Le filtrage précis est fait en Kotlin avec Location.distanceTo()
     * Cette requête fait une présélection large (±0.01° ≈ 1km) pour limiter les données chargées.
     */
    @Query("""
        SELECT * FROM sessions 
        WHERE latitude IS NOT NULL 
        AND longitude IS NOT NULL
        AND latitude BETWEEN :lat - 0.01 AND :lat + 0.01
        AND longitude BETWEEN :lon - 0.01 AND :lon + 0.01
        ORDER BY startTimestampMs DESC
    """)
    suspend fun getSessionsNear(lat: Double, lon: Double): List<SessionEntity>

    @Query("UPDATE sessions SET isActive = 0")
    suspend fun deactivateAllSessions()

    @Query("UPDATE sessions SET summary = :summary WHERE id = :id")
    suspend fun updateSummary(id: Long, summary: String)
}
