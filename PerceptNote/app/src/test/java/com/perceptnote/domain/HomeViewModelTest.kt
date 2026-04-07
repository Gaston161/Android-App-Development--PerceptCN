// 📄 app/src/test/java/com/perceptnote/domain/HomeViewModelTest.kt
package com.perceptnote.domain

import com.perceptnote.domain.model.Session
import com.perceptnote.domain.repository.SessionRepository
import com.perceptnote.domain.usecase.GetNotesByLocationUseCase
import com.perceptnote.features.home.HomeViewModel
import com.perceptnote.sensors.location.LocationModule
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    private lateinit var sessionRepository: SessionRepository
    private lateinit var getNotesByLocationUseCase: GetNotesByLocationUseCase
    private lateinit var locationModule: LocationModule
    private lateinit var viewModel: HomeViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        sessionRepository = mockk(relaxed = true)
        getNotesByLocationUseCase = mockk()
        locationModule = mockk(relaxed = true)

        every { sessionRepository.getAllSessions() } returns flowOf(emptyList())
        coEvery { locationModule.getLastLocation() } returns null
        coEvery { getNotesByLocationUseCase(any(), any()) } returns emptyList()

        viewModel = HomeViewModel(sessionRepository, getNotesByLocationUseCase, locationModule)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have empty sessions`() = runTest {
        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.sessions.isEmpty())
    }

    @Test
    fun `sessions should be loaded from repository`() = runTest {
        val sessions = listOf(
            Session(id = 1L, title = "Cours Android", startTimestampMs = 1000L)
        )
        every { sessionRepository.getAllSessions() } returns flowOf(sessions)
        coEvery { locationModule.getLastLocation() } returns null

        val vm = HomeViewModel(sessionRepository, getNotesByLocationUseCase, locationModule)

        assertEquals(1, vm.uiState.value.sessions.size)
        assertEquals("Cours Android", vm.uiState.value.sessions.first().title)
    }

    @Test
    fun `dismissNearbyNotification should clear notification flag`() = runTest {
        viewModel.dismissNearbyNotification()
        assertFalse(viewModel.uiState.value.hasNearbyNotification)
    }

    @Test
    fun `clearError should remove error message`() = runTest {
        viewModel.clearError()
        assertNull(viewModel.uiState.value.errorMessage)
    }
}
