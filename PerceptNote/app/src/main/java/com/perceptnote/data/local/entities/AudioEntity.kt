// 📄 app/src/main/java/com/perceptnote/data/local/entities/AudioEntity.kt
package com.perceptnote.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "audio_segments",
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
data class AudioEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val transcriptionText: String,  // Texte transcrit par SpeechRecognizer
    val startMs: Long,              // Début du segment dans l'enregistrement
    val endMs: Long,                // Fin du segment
    val confidence: Float = 1.0f   // Score de confiance (0.0-1.0)
)
