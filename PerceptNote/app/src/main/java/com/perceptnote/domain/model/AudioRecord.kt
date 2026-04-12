// 📄 app/src/main/java/com/perceptnote/domain/model/AudioRecord.kt
package com.perceptnote.domain.model

data class AudioRecord(
    val id: Long = 0,
    val sessionId: Long,
    val transcriptionText: String,
    val startMs: Long,
    val endMs: Long,
    val confidence: Float = 1.0f
)
