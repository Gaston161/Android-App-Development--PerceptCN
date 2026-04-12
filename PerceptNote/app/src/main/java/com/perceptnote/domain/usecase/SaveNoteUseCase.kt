// 📄 app/src/main/java/com/perceptnote/domain/usecase/SaveNoteUseCase.kt
package com.perceptnote.domain.usecase

import com.perceptnote.domain.model.Note
import com.perceptnote.domain.repository.NoteRepository
import javax.inject.Inject

class SaveNoteUseCase @Inject constructor(
    private val noteRepository: NoteRepository
) {
    suspend operator fun invoke(note: Note): Long = noteRepository.saveNote(note)
    suspend operator fun invoke(notes: List<Note>) = noteRepository.saveNotes(notes)
}
