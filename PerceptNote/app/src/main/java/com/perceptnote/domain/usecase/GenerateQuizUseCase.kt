package com.perceptnote.domain.usecase

import com.perceptnote.data.remote.AIApiService
import com.perceptnote.data.remote.dto.Message
import com.perceptnote.data.remote.dto.MessageRequest
import com.perceptnote.data.remote.dto.extractText
import javax.inject.Inject

data class QuizQuestion(
    val question: String,
    val options: List<String>,
    val correctIndex: Int,
    val explanation: String
)

class GenerateQuizUseCase @Inject constructor(
    private val aiApi: AIApiService
) {
    suspend operator fun invoke(notesContent: String, questionCount: Int = 5): Result<List<QuizQuestion>> {
        return try {
            val response = aiApi.sendMessage(
                MessageRequest(
                    messages = listOf(
                        Message(
                            role = "system",
                            content = """Tu es un professeur expert qui crée des QCM de révision.
                                |Réponds UNIQUEMENT avec un JSON valide, sans texte avant ou après.
                                |Format JSON strict :
                                |[{"question":"...","options":["A","B","C","D"],"correctIndex":0,"explanation":"..."}]
                                |correctIndex est l'index (0-3) de la bonne réponse dans options.""".trimMargin()
                        ),
                        Message(
                            role = "user",
                            content = "Génère exactement $questionCount questions QCM basées sur ces notes :\n\n$notesContent"
                        )
                    )
                )
            )
            val jsonText = response.extractText().trim()
            // Parser le JSON en liste de QuizQuestion
            val questions = parseQuizJson(jsonText)
            Result.success(questions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseQuizJson(json: String): List<QuizQuestion> {
        // Implémentation basique — en production utiliser Gson/Moshi
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<Map<String, Any>>>() {}.type
        val rawList: List<Map<String, Any>> = gson.fromJson(json, type)
        return rawList.map { map ->
            val options = (map["options"] as List<*>).map { it.toString() }
            QuizQuestion(
                question = map["question"].toString(),
                options = options,
                correctIndex = (map["correctIndex"] as Double).toInt(),
                explanation = map["explanation"]?.toString() ?: ""
            )
        }
    }
}