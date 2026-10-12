package pl.ceranka.happypies.screen.client_details

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.report.ReportComment
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.fakes.FakeClientRepository
import pl.ceranka.happypies.fakes.FakeReportRepository
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.report
import pl.ceranka.happypies.fakes.accountOf
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
    private val account = accountOf(trainer)
    private val offline = "Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie."

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() = ClientDetailsViewModel(clients, reports, account, anna.id, TimeZone.UTC)

    private val ClientDetailsViewModel.reportIds get() = uiState.value.reports.map { it.id }

    @Test
    fun showsTheClientAndTheReportsWithResolvedAuthors() {
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
    fun clientErrorShowsTheMessageAndRefreshLoadsAgain() {
        clients.error = DataException.Unavailable("offline")
        val viewModel = createViewModel()
        assertEquals(ClientDetailsUiState(error = offline), viewModel.uiState.value)

        clients.error = null
        reports.reports = listOf(report("r1"))
        viewModel.onRefresh()

        assertEquals(anna, viewModel.uiState.value.client)
        assertEquals(listOf("r1"), viewModel.reportIds)
    }

    @Test
    fun reportsErrorShowsTheMessage() {
        reports.reportsError = DataException.PermissionDenied("denied")

        val viewModel = createViewModel()

        assertEquals(ClientDetailsUiState(error = "Nie masz dostępu do tych danych."), viewModel.uiState.value)
    }

    @Test
    fun refreshReplacesTheList() {
        reports.reports = listOf(report("r1"))
        val viewModel = createViewModel()

        reports.reports = listOf(report("r2"), report("r1"))
        viewModel.onRefresh()

        assertEquals(listOf("r2", "r1"), viewModel.reportIds)
        assertEquals(false, viewModel.uiState.value.isRefreshing)
    }

    @Test
    fun failedRefreshKeepsTheShownData() {
        reports.reports = listOf(report("r1"))
        val viewModel = createViewModel()

        reports.reportsError = DataException.Unavailable("offline")
        viewModel.onRefresh()

        val state = viewModel.uiState.value
        assertEquals(anna, state.client)
        assertEquals(listOf("r1"), viewModel.reportIds)
        assertEquals(offline, state.error)
        assertEquals(false, state.isRefreshing)
    }

    @Test
    fun newerLoadWinsOverAnOlderOneStillRunning() {
        val slowFirstLoad = CompletableDeferred<Unit>()
        reports.reports = listOf(report("old"))
        reports.reportsGate = slowFirstLoad
        val viewModel = createViewModel()

        reports.reportsGate = null
        reports.reports = listOf(report("new"))
        viewModel.onRefresh()
        slowFirstLoad.complete(Unit)

        assertEquals(listOf("new"), viewModel.reportIds)
    }

    @Test
    fun reloadsAfterAReportChange() {
        val viewModel = createViewModel()
        reports.reports = listOf(report("new"))

        reports.changes.tryEmit(Unit)

        assertEquals(listOf("new"), viewModel.reportIds)
    }

    @Test
    fun authorNameFallsBackToTheRole() {
        assertEquals("Anna Kowalska", authorName(anna.id, UserRole.CLIENT, anna, account = null))
        assertEquals("Jan Trener", authorName(trainer.uid, UserRole.TRAINER, anna, trainer))
        assertEquals("Trener", authorName("someone", UserRole.TRAINER, anna, account = null))
        assertEquals("Klient", authorName("someone", UserRole.CLIENT, anna, account = null))
    }
}
