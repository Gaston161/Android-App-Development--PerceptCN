package com.perceptnote.domain.usecase

import com.perceptnote.data.remote.AIApiService
import com.perceptnote.data.remote.dto.Message
import com.perceptnote.data.remote.dto.MessageRequest
import com.perceptnote.data.remote.dto.extractText
import com.perceptnote.domain.repository.SessionRepository
import javax.inject.Inject

class GenerateSummaryUseCase @Inject constructor(
    private val aiApi: AIApiService,
    private val sessionRepository: SessionRepository
) {
    suspend operator fun invoke(sessionId: Long, notesContent: String): Result<String> {
        return try {
            val response = aiApi.sendMessage(
                MessageRequest(
                    messages = listOf(
                        Message(
                            role = "system",
                            content = """Tu es un assistant académique expert en synthèse de cours.
                                |Génère une fiche de révision structurée et concise en français.
                                |Utilise des titres clairs, des points importants en gras, et un résumé final.
                                |Format : ## Titre principal\n**Points clés :**\n- ...""".trimMargin()
                        ),
                        Message(
                            role = "user",
                            content = "Voici les notes brutes de ma session de cours. Génère une fiche structurée :\n\n$notesContent"
                        )
                    )
                )
            )
            val summary = response.extractText()
            // Sauvegarder le résumé en base de données
            sessionRepository.saveSummary(sessionId, summary)
            Result.success(summary)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}