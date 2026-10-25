package pl.ceranka.happypies.screen.client_details

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.report.ReportComment
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.fakes.FakeClientRepository
import pl.ceranka.happypies.fakes.FakeTrainerRepository
import pl.ceranka.happypies.fakes.FakeReportRepository
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.report
import pl.ceranka.happypies.fakes.trainer
import pl.ceranka.happypies.fakes.video
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class ClientDetailsViewModelTest {

    private val clients = FakeClientRepository()
    private val reports = FakeReportRepository()
    private val offline = "Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie."

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // The state is shared WhileSubscribed, so a test collects it like the screen does.
    private fun TestScope.createViewModel() = ClientDetailsViewModel(clients, FakeTrainerRepository(), reports, anna.id, TimeZone.UTC).also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private val ClientDetailsViewModel.reportIds get() = uiState.value.reports.map { it.id }

    @Test
    fun showsTheClientAndTheReportsWithResolvedAuthors() = runTest {
        reports.reports = listOf(
            report("r1", createdAt = 1_791_830_400_000, media = listOf(video)),
            report(
                "r2",
                createdAt = 1_791_191_100_000,
                authorId = trainer.uid,
                authorRole = UserRole.TRAINER,
                comment = ReportComment(anna.id, UserRole.CLIENT, "Dzięki", 2_000),
            ),
        )

        val viewModel = createViewModel()

        assertEquals(
            ClientDetailsUiState(
                client = anna,
                reports = listOf(
                    ReportListItem("r1", "Raport r1", "12.10.2026, 18:40", "Anna Kowalska", mediaCount = 1, hasComment = false),
                    ReportListItem("r2", "Raport r2", "05.10.2026, 09:05", "Jan Trener", mediaCount = 0, hasComment = true),
                ),
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun clientErrorShowsTheMessageAndRetryLoadsAgain() = runTest {
        clients.error = DataException.Unavailable("offline")
        val viewModel = createViewModel()
        assertEquals(ClientDetailsUiState(error = offline), viewModel.uiState.value)

        clients.error = null
        reports.reports = listOf(report("r1"))
        viewModel.onRetry()

        assertEquals(anna, viewModel.uiState.value.client)
        assertEquals(listOf("r1"), viewModel.reportIds)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun reportsErrorShowsTheMessage() = runTest {
        reports.failReports(DataException.PermissionDenied("denied"))

        val viewModel = createViewModel()

        assertEquals(ClientDetailsUiState(error = "Nie masz dostępu do tych danych."), viewModel.uiState.value)
    }

    @Test
    fun showsReportChangesLive() = runTest {
        reports.reports = listOf(report("r1"))
        val viewModel = createViewModel()

        reports.reports = listOf(report("r2"), report("r1"))

        assertEquals(listOf("r2", "r1"), viewModel.reportIds)
    }

    @Test
    fun failureWhileObservingKeepsTheShownDataUntilRetry() = runTest {
        reports.reports = listOf(report("r1"))
        val viewModel = createViewModel()

        reports.failReports(DataException.Unavailable("offline"))

        val state = viewModel.uiState.value
        assertEquals(anna, state.client)
        assertEquals(listOf("r1"), viewModel.reportIds)
        assertEquals(offline, state.error)

        reports.reports = listOf(report("r2"))
        viewModel.onRetry()

        assertEquals(listOf("r2"), viewModel.reportIds)
        assertEquals(null, viewModel.uiState.value.error)
    }

    @Test
    fun stopsListeningFiveSecondsAfterTheScreenStopsCollecting() = runTest {
        val viewModel = ClientDetailsViewModel(clients, FakeTrainerRepository(), reports, anna.id, TimeZone.UTC)
        val screen = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        assertEquals(1, reports.activeListeners)

        screen.cancel()
        advanceTimeBy(4_000)
        assertEquals(1, reports.activeListeners)
        advanceTimeBy(1_001)

        assertEquals(0, reports.activeListeners)
    }
}