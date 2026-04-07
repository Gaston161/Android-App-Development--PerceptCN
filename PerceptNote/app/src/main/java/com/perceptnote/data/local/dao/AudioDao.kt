// 📄 app/src/main/java/com/perceptnote/data/local/dao/AudioDao.kt
package com.perceptnote.data.local.dao

import androidx.room.*
import com.perceptnote.data.local.entities.AudioEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(audio: AudioEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(segments: List<AudioEntity>)

    @Query("SELECT * FROM audio_segments WHERE sessionId = :sessionId ORDER BY startMs ASC")
    fun getSegmentsBySession(sessionId: Long): Flow<List<AudioEntity>>

    @Query("SELECT * FROM audio_segments WHERE sessionId = :sessionId ORDER BY startMs ASC")
    suspend fun getSegmentsBySessionSync(sessionId: Long): List<AudioEntity>

    @Query("DELETE FROM audio_segments WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)
}
