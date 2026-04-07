// 📄 app/src/main/java/com/perceptnote/domain/model/Note.kt
package com.perceptnote.domain.model

data class Note(
    val id: Long = 0,
    val sessionId: Long,
    val content: String,
    val type: NoteType,
    val timestampMs: Long,
    val imageUri: String? = null,
    val audioSegmentMs: Long? = null
)

enum class NoteType { OCR, TRANSCRIPTION, MANUAL }
