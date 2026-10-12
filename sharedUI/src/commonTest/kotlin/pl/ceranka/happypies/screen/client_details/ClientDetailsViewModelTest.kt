package pl.ceranka.happypies.screen.client_details

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
import pl.ceranka.happypies.fakes.sessionOf
import pl.ceranka.happypies.fakes.trainer
import pl.ceranka.happypies.fakes.video
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ClientDetailsViewModelTest {

    private val clients = FakeClientRepository()
    private val reports = FakeReportRepository()
    private val session = sessionOf(trainer)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun createViewModel() = ClientDetailsViewModel(clients, reports, session, anna.id, TimeZone.UTC)

    private val ClientDetailsViewModel.loadedReports get() = assertIs<ReportsState.Loaded>(uiState.value.reports)

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

        assertEquals(ClientState.Success(anna), viewModel.uiState.value.client)
        assertEquals(
            listOf(
                ReportListItem("r1", "Raport r1", "12.10.2026, 18:40", "Anna Kowalska", mediaCount = 1, hasComment = false),
                ReportListItem("r2", "Raport r2", "05.10.2026, 09:05", "Jan Trener", mediaCount = 0, hasComment = true),
            ),
            viewModel.loadedReports.items,
        )
    }

    @Test
    fun clientErrorDoesNotHideReports() {
        clients.error = DataException.Unavailable("offline")
        reports.reports = listOf(report("r1"))

        val viewModel = createViewModel()

        assertEquals(
            ClientState.Error("Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie."),
            viewModel.uiState.value.client,
        )
        assertEquals(1, viewModel.loadedReports.items.size)
    }

    @Test
    fun emptyListIsLoaded() {
        val viewModel = createViewModel()

        assertEquals(emptyList(), viewModel.loadedReports.items)
    }

    @Test
    fun reportsErrorCanBeRetried() {
        reports.reportsError = DataException.PermissionDenied("denied")
        val viewModel = createViewModel()
        assertEquals(
            ReportsState.Error("Nie masz dostępu do tych danych."),
            viewModel.uiState.value.reports,
        )

        reports.reportsError = null
        reports.reports = listOf(report("r1"))
        viewModel.retry()

        assertEquals(listOf("r1"), viewModel.loadedReports.items.map { it.id })
    }


    @Test
    fun refreshReplacesTheList() {
        reports.reports = listOf(report("r1"))
        val viewModel = createViewModel()

        reports.reports = listOf(report("r2"), report("r1"))
        viewModel.refresh()

        assertEquals(listOf("r2", "r1"), viewModel.loadedReports.items.map { it.id })
        assertEquals(false, viewModel.loadedReports.isRefreshing)
    }

    @Test
    fun failedRefreshKeepsTheList() {
        reports.reports = listOf(report("r1"))
        val viewModel = createViewModel()

        reports.reportsError = DataException.Unavailable("offline")
        viewModel.refresh()

        assertEquals(listOf("r1"), viewModel.loadedReports.items.map { it.id })
        assertEquals(false, viewModel.loadedReports.isRefreshing)
        assertEquals("Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie.", viewModel.loadedReports.actionError)
    }

    @Test
    fun reloadsAfterAReportChange() {
        val viewModel = createViewModel()
        reports.reports = listOf(report("new"))

        reports.changes.tryEmit(Unit)

        assertEquals(listOf("new"), viewModel.loadedReports.items.map { it.id })
        assertNull(viewModel.loadedReports.actionError)
    }

    @Test
    fun unknownAuthorFallsBackToTheRole() {
        assertEquals("Trener", authorName("someone", UserRole.TRAINER, client = null, profile = null))
        assertEquals("Klient", authorName("someone", UserRole.CLIENT, client = null, profile = null))
        assertEquals("Anna Kowalska", authorName(anna.id, UserRole.CLIENT, client = anna, profile = null))
    }
}
