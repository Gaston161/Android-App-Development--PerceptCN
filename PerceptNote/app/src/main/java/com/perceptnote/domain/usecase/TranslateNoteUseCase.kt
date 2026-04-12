package com.perceptnote.domain.usecase

import com.perceptnote.data.remote.AIApiService
import com.perceptnote.data.remote.dto.Message
import com.perceptnote.data.remote.dto.MessageRequest
import com.perceptnote.data.remote.dto.extractText
import javax.inject.Inject

class TranslateNoteUseCase @Inject constructor(
    private val aiApi: AIApiService
) {
    suspend operator fun invoke(text: String, targetLanguage: String): Result<String> {
        return try {
            val response = aiApi.sendMessage(
                MessageRequest(
                    messages = listOf(
                        Message(
                            role = "system",
                            content = "Tu es un traducteur professionnel. Traduis le texte fourni en $targetLanguage. Conserve la structure et le formatage d'origine. Réponds uniquement avec la traduction, sans introduction."
                        ),
                        Message(role = "user", content = text)
                    )
                )
            )
            Result.success(response.extractText())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}