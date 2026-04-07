// 📄 app/src/main/java/com/perceptnote/data/repository/NoteRepositoryImpl.kt
package com.perceptnote.data.repository

import com.perceptnote.data.local.dao.NoteDao
import com.perceptnote.data.local.entities.NoteEntity
import com.perceptnote.domain.model.Note
import com.perceptnote.domain.model.NoteType
import com.perceptnote.domain.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class NoteRepositoryImpl @Inject constructor(
    private val noteDao: NoteDao
) : NoteRepository {

    override suspend fun saveNote(note: Note): Long = noteDao.insert(note.toEntity())

    override suspend fun saveNotes(notes: List<Note>) = noteDao.insertAll(notes.map { it.toEntity() })

    override fun getNotesBySession(sessionId: Long): Flow<List<Note>> =
        noteDao.getNotesBySession(sessionId).map { list -> list.map { it.toDomain() } }

    override suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)?.toDomain()

    override fun searchNotes(query: String): Flow<List<Note>> =
        noteDao.searchNotes(query).map { list -> list.map { it.toDomain() } }

    override suspend fun deleteNote(note: Note) = noteDao.delete(note.toEntity())

    override suspend fun deleteNotesBySession(sessionId: Long) = noteDao.deleteBySession(sessionId)

    // === Mappers ===
    private fun Note.toEntity() = NoteEntity(
        id = id, sessionId = sessionId, content = content,
        type = type.name, timestampMs = timestampMs,
        imageUri = imageUri, audioSegmentMs = audioSegmentMs
    )

    private fun NoteEntity.toDomain() = Note(
        id = id, sessionId = sessionId, content = content,
        type = NoteType.valueOf(type), timestampMs = timestampMs,
        imageUri = imageUri, audioSegmentMs = audioSegmentMs
    )
}
