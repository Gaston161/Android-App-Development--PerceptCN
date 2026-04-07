// 📄 app/src/main/java/com/perceptnote/data/remote/dto/MessageResponse.kt
package com.perceptnote.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MessageResponse(
    val id: String,
    val type: String,
    val role: String,
    val content: List<ContentBlock>,
    val model: String,
    @SerializedName("stop_reason") val stopReason: String?,
    val usage: Usage?
)

data class ContentBlock(
    val type: String, // "text"
    val text: String
)

data class Usage(
    @SerializedName("input_tokens") val inputTokens: Int,
    @SerializedName("output_tokens") val outputTokens: Int
)

// Extension pour extraire le texte facilement
fun MessageResponse.extractText(): String =
    content.filter { it.type == "text" }.joinToString("\n") { it.text }
