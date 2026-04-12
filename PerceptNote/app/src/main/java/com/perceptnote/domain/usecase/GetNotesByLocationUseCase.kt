// 📄 app/src/main/java/com/perceptnote/domain/usecase/GetNotesByLocationUseCase.kt
package com.perceptnote.domain.usecase

import com.perceptnote.domain.model.Session
import com.perceptnote.domain.repository.SessionRepository
import javax.inject.Inject

class GetNotesByLocationUseCase @Inject constructor(
    private val sessionRepository: SessionRepository
) {
    /**
     * Retourne les sessions capturées à moins de 20m de la position actuelle.
     * C'est la fonctionnalité "mémoire géographique" de PerceptNote.
     */
    suspend operator fun invoke(lat: Double, lon: Double): List<Session> =
        sessionRepository.getSessionsNear(lat, lon, radiusMeters = 20f)
}
