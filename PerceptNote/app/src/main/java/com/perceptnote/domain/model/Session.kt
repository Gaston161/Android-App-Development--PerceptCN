// 📄 app/src/main/java/com/perceptnote/domain/model/Session.kt
package com.perceptnote.domain.model

data class Session(
    val id: Long = 0,
    val title: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationName: String? = null,
    val startTimestampMs: Long,
    val endTimestampMs: Long? = null,
    val audioFilePath: String? = null,
    val summary: String? = null,
    val isActive: Boolean = false,
    val notes: List<Note> = emptyList()
)
