// 📄 app/src/test/java/com/perceptnote/domain/SaveNoteUseCaseTest.kt
package com.perceptnote.domain

import com.perceptnote.domain.model.Note
import com.perceptnote.domain.model.NoteType
import com.perceptnote.domain.repository.NoteRepository
import com.perceptnote.domain.usecase.SaveNoteUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires — SaveNoteUseCase
 */
class SaveNoteUseCaseTest {

    private lateinit var noteRepository: NoteRepository
    private lateinit var saveNoteUseCase: SaveNoteUseCase

    @Before
    fun setup() {
        noteRepository = mockk()
        saveNoteUseCase = SaveNoteUseCase(noteRepository)
    }

    @Test
    fun `invoke single note should call repository saveNote`() = runTest {
        val note = Note(
            sessionId = 1L,
            content = "Test note OCR",
            type = NoteType.OCR,
            timestampMs = System.currentTimeMillis()
        )
        coEvery { noteRepository.saveNote(note) } returns 42L

        val result = saveNoteUseCase(note)

        coVerify(exactly = 1) { noteRepository.saveNote(note) }
        assertEquals(42L, result)
    }

    @Test
    fun `invoke list of notes should call repository saveNotes`() = runTest {
        val notes = listOf(
            Note(sessionId = 1L, content = "Note 1", type = NoteType.TRANSCRIPTION, timestampMs = 1000L),
            Note(sessionId = 1L, content = "Note 2", type = NoteType.OCR, timestampMs = 2000L)
        )
        coEvery { noteRepository.saveNotes(notes) } returns Unit

        saveNoteUseCase(notes)

        coVerify(exactly = 1) { noteRepository.saveNotes(notes) }
    }
}
