// 📄 data/remote/dto/MessageRequest.kt — VERSION OPENROUTER
package com.perceptnote.data.remote.dto

import com.google.gson.annotations.SerializedName

data class MessageRequest(
    // Modèles GRATUITS sur OpenRouter — changer ici selon tes préférences
    val model: String = "qwen/qwen3-8b:free",
    // Autres modèles gratuits disponibles :
    // "meta-llama/llama-3.1-8b-instruct:free"
    // "google/gemma-3-4b-it:free"
    // "mistralai/mistral-7b-instruct:free"
    // "deepseek/deepseek-r1:free"  (très bon pour le résumé)
    @SerializedName("max_tokens") val maxTokens: Int = 1000,
    val messages: List<Message>,
    // system est un message séparé dans OpenRouter (format OpenAI)
    // on l'injecte via les messages directement
)

data class Message(
    val role: String,   // "user" | "assistant" | "system"
    val content: String
)