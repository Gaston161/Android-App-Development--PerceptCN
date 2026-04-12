// 📄 app/src/main/java/com/perceptnote/data/local/entities/SessionEntity.kt
package com.perceptnote.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val latitude: Double?,              // Coordonnées GPS de la session
    val longitude: Double?,
    val locationName: String? = null,   // Nom de lieu résolu (ex: "Amphi B")
    val startTimestampMs: Long,
    val endTimestampMs: Long? = null,
    val audioFilePath: String? = null,  // Chemin vers le fichier .m4a
    val summary: String? = null,        // Résumé IA généré
    val isActive: Boolean = false       // Session en cours d'enregistrement
)
