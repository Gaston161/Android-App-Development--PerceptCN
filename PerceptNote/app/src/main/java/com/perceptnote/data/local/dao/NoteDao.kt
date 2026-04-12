// 📄 app/src/main/java/com/perceptnote/data/local/dao/NoteDao.kt
package com.perceptnote.data.local.dao

import androidx.room.*
import com.perceptnote.data.local.entities.NoteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(note: NoteEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(notes: List<NoteEntity>)

    @Update
    suspend fun update(note: NoteEntity)

    @Delete
    suspend fun delete(note: NoteEntity)

    @Query("SELECT * FROM notes WHERE sessionId = :sessionId ORDER BY timestampMs ASC")
    fun getNotesBySession(sessionId: Long): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): NoteEntity?

    @Query("SELECT * FROM notes WHERE content LIKE '%' || :query || '%' ORDER BY timestampMs DESC")
    fun searchNotes(query: String): Flow<List<NoteEntity>>

    @Query("DELETE FROM notes WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: Long)
}
