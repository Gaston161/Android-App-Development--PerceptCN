// 📄 app/src/test/java/com/perceptnote/domain/GetNotesByLocationUseCaseTest.kt
package com.perceptnote.domain

import com.perceptnote.domain.model.Session
import com.perceptnote.domain.repository.SessionRepository
import com.perceptnote.domain.usecase.GetNotesByLocationUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires — GetNotesByLocationUseCase
 * Vérifie la mémoire géographique (rayon 20m).
 */
class GetNotesByLocationUseCaseTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var useCase: GetNotesByLocationUseCase

    @Before
    fun setup() {
        sessionRepository = mockk()
        useCase = GetNotesByLocationUseCase(sessionRepository)
    }

    @Test
    fun `should return sessions near given coordinates`() = runTest {
        val lat = 3.8667
        val lon = 11.5167
        val expectedSessions = listOf(
            Session(
                id = 1L,
                title = "Cours de maths",
                latitude = lat,
                longitude = lon,
                startTimestampMs = 1000L
            )
        )
        coEvery { sessionRepository.getSessionsNear(lat, lon, 20f) } returns expectedSessions

        val result = useCase(lat, lon)

        coVerify(exactly = 1) { sessionRepository.getSessionsNear(lat, lon, 20f) }
        assertEquals(1, result.size)
        assertEquals("Cours de maths", result.first().title)
    }

    @Test
    fun `should return empty list when no sessions nearby`() = runTest {
        val lat = 0.0
        val lon = 0.0
        coEvery { sessionRepository.getSessionsNear(lat, lon, 20f) } returns emptyList()

        val result = useCase(lat, lon)

        assertTrue(result.isEmpty())
    }
}
