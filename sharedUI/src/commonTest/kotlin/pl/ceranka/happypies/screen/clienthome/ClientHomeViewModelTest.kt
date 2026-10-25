package pl.ceranka.happypies.screen.clienthome

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.fakes.FakeClientRepository
import pl.ceranka.happypies.fakes.FakeReportRepository
import pl.ceranka.happypies.fakes.FakeTrainerRepository
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.report
import pl.ceranka.happypies.fakes.trainer
import pl.ceranka.happypies.ui.common.ReportListItem
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientHomeViewModelTest {

    private val clients = FakeClientRepository()
    private val trainers = FakeTrainerRepository()
    private val reports = FakeReportRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.createViewModel() = ClientHomeViewModel(clients, trainers, reports, anna.id, TimeZone.UTC).also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun showsTheHeaderAndTheReportsWithRealNames() = runTest {
        reports.reports = listOf(
            report("r1", createdAt = 1_791_830_400_000),
            report("r2", createdAt = 1_791_191_100_000, authorId = trainer.uid, authorRole = UserRole.TRAINER)
        )

        val state = createViewModel().uiState.value

        assertEquals("Cześć, Anna", state.greeting)
        assertEquals("Bala · trener Jan Trener", state.subtitle)
        assertEquals("AK", state.initials)
        assertEquals(
            listOf(
                ReportListItem("r1", "Raport r1", "12.10.2026, 18:40", "Anna Kowalska", mediaCount = 0, hasComment = false),
                ReportListItem("r2", "Raport r2", "05.10.2026, 09:05", "Jan Trener", mediaCount = 0, hasComment = false)
            ),
            state.reports
        )
        assertEquals("2", state.reportCount)
    }

    @Test
    fun noReportsShowsTheEmptyState() = runTest {
        val state = createViewModel().uiState.value

        assertTrue(state.showEmptyState)
        assertFalse(state.showLoadingProgress)
    }

    @Test
    fun trainerErrorShowsTheMessageAndRetryLoadsAgain() = runTest {
        trainers.error = DataException.Unavailable("offline")
        val viewModel = createViewModel()
        assertEquals(ClientHomeUiState(error = "Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie."), viewModel.uiState.value)
        assertFalse(viewModel.uiState.value.showEmptyState)

        trainers.error = null
        reports.reports = listOf(report("r1"))
        viewModel.onRetry()

        assertEquals(listOf("r1"), viewModel.uiState.value.reports.map { it.id })
        assertEquals(null, viewModel.uiState.value.error)
    }
}