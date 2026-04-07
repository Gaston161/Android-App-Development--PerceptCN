// 📄 app/src/main/java/com/perceptnote/domain/repository/NoteRepository.kt
package com.perceptnote.domain.repository

import com.perceptnote.domain.model.Note
import kotlinx.coroutines.flow.Flow

interface NoteRepository {
    suspend fun saveNote(note: Note): Long
    suspend fun saveNotes(notes: List<Note>)
    fun getNotesBySession(sessionId: Long): Flow<List<Note>>
    suspend fun getNoteById(id: Long): Note?
    fun searchNotes(query: String): Flow<List<Note>>
    suspend fun deleteNote(note: Note)
    suspend fun deleteNotesBySession(sessionId: Long)
}
