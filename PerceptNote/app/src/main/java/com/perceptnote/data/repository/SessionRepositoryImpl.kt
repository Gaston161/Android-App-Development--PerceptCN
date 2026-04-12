// 📄 app/src/main/java/com/perceptnote/data/repository/SessionRepositoryImpl.kt
package com.perceptnote.data.repository

import android.location.Location
import com.perceptnote.data.local.dao.SessionDao
import com.perceptnote.data.local.entities.SessionEntity
import com.perceptnote.domain.model.Session
import com.perceptnote.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class SessionRepositoryImpl @Inject constructor(
    private val sessionDao: SessionDao
) : SessionRepository {

    override suspend fun createSession(session: Session): Long =
        sessionDao.insert(session.toEntity())

    override suspend fun updateSession(session: Session) =
        sessionDao.update(session.toEntity())

    override suspend fun getSessionById(id: Long): Session? =
        sessionDao.getSessionById(id)?.toDomain()

    override fun getAllSessions(): Flow<List<Session>> =
        sessionDao.getAllSessions().map { list -> list.map { it.toDomain() } }

    override suspend fun getActiveSession(): Session? =
        sessionDao.getActiveSession()?.toDomain()

    override suspend fun getSessionsNear(
        lat: Double, lon: Double, radiusMeters: Float
    ): List<Session> {
        // Présélection large depuis Room, puis filtrage précis avec distanceTo()
        val candidates = sessionDao.getSessionsNear(lat, lon)
        return candidates.filter { entity ->
            if (entity.latitude == null || entity.longitude == null) return@filter false
            val current = Location("").apply { latitude = lat; longitude = lon }
            val sessionLoc = Location("").apply {
                latitude = entity.latitude
                longitude = entity.longitude
            }
            // IMPORTANT : rayon 20m comme spécifié dans le document de projet
            current.distanceTo(sessionLoc) <= radiusMeters
        }.map { it.toDomain() }
    }

    override suspend fun deactivateAllSessions() = sessionDao.deactivateAllSessions()

    override suspend fun saveSummary(sessionId: Long, summary: String) =
        sessionDao.updateSummary(sessionId, summary)

    override suspend fun deleteSession(session: Session) =
        sessionDao.delete(session.toEntity())

    // === Mappers ===
    private fun Session.toEntity() = SessionEntity(
        id = id, title = title, latitude = latitude, longitude = longitude,
        locationName = locationName, startTimestampMs = startTimestampMs,
        endTimestampMs = endTimestampMs, audioFilePath = audioFilePath,
        summary = summary, isActive = isActive
    )

    private fun SessionEntity.toDomain() = Session(
        id = id, title = title, latitude = latitude, longitude = longitude,
        locationName = locationName, startTimestampMs = startTimestampMs,
        endTimestampMs = endTimestampMs, audioFilePath = audioFilePath,
        summary = summary, isActive = isActive
    )
}
