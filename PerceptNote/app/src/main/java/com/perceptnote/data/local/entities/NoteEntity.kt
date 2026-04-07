// 📄 app/src/main/java/com/perceptnote/data/local/entities/NoteEntity.kt
package com.perceptnote.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notes",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class NoteEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val content: String,            // Texte OCR ou transcription
    val type: String,               // "OCR" | "TRANSCRIPTION" | "MANUAL"
    val timestampMs: Long,          // Horodatage de capture
    val imageUri: String? = null,   // URI de la photo source (si OCR)
    val audioSegmentMs: Long? = null // Position dans l'audio synchronisé
)
