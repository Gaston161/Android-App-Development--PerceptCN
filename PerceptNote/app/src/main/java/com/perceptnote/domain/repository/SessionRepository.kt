// 📄 app/src/main/java/com/perceptnote/domain/repository/SessionRepository.kt
package com.perceptnote.domain.repository

import com.perceptnote.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun createSession(session: Session): Long
    suspend fun updateSession(session: Session)
    suspend fun getSessionById(id: Long): Session?
    fun getAllSessions(): Flow<List<Session>>
    suspend fun getActiveSession(): Session?
    suspend fun getSessionsNear(lat: Double, lon: Double, radiusMeters: Float = 20f): List<Session>
    suspend fun deactivateAllSessions()
    suspend fun saveSummary(sessionId: Long, summary: String)
    suspend fun deleteSession(session: Session)
}
