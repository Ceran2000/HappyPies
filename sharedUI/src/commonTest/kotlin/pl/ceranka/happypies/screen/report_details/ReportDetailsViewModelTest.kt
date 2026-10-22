package pl.ceranka.happypies.screen.report_details

import kotlinx.coroutines.CompletableDeferred
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
import pl.ceranka.happypies.data.report.ReportComment
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.fakes.FakeClientRepository
import pl.ceranka.happypies.fakes.FakeReportRepository
import pl.ceranka.happypies.fakes.accountOf
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.report
import pl.ceranka.happypies.fakes.trainer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReportDetailsViewModelTest {

    private val clients = FakeClientRepository()
    private val reports = FakeReportRepository()
    private val account = accountOf(trainer)
    private val offline = "Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie."

    private val added = 1_791_191_100_000 // 05.10.2026, 09:05 UTC
    private val later = 1_791_830_400_000 // 12.10.2026, 18:40 UTC
    private val r1 = report("r1", createdAt = added).copy(description = "Trzy próby")

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // The state is shared WhileSubscribed, so a test collects it like the screen does.
    private fun TestScope.createViewModel() = ReportDetailsViewModel(reports, clients, account, anna.id, "r1", TimeZone.UTC).also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun showsTheReportWithResolvedPeopleAndTheComment() = runTest {
        reports.report = r1.copy(comment = ReportComment(trainer.uid, UserRole.TRAINER, "Dobra robota", later))

        val viewModel = createViewModel()

        assertEquals(
            ReportDetailsUiState(
                report = ReportDetailsItem(
                    title = "Raport r1",
                    description = "Trzy próby",
                    addedBy = "Anna Kowalska · 05.10.2026, 09:05",
                    editedBy = null,
                    comment = CommentItem("Komentarz trenera", "Dobra robota", "12.10.2026, 18:40"),
                ),
            ),
            viewModel.uiState.value,
        )
    }

    @Test
    fun showsWhoEditedTheReportLast() = runTest {
        reports.report = r1.copy(lastEditedBy = trainer.uid, updatedAt = later)

        val viewModel = createViewModel()

        assertEquals("Jan Trener · 12.10.2026, 18:40", viewModel.uiState.value.report?.editedBy)
    }

    @Test
    fun showsChangesLive() = runTest {
        reports.report = r1
        val viewModel = createViewModel()

        reports.report = r1.copy(title = "Nowy tytuł")

        assertEquals("Nowy tytuł", viewModel.uiState.value.report?.title)
    }

    @Test
    fun reportRemovedElsewhereIsShownAsDeleted() = runTest {
        reports.report = r1
        val viewModel = createViewModel()

        reports.report = null

        assertTrue(viewModel.uiState.value.isDeleted)
    }

    @Test
    fun deletingTheReportFinishes() = runTest {
        reports.report = r1
        val viewModel = createViewModel()

        viewModel.onDelete()

        assertEquals(listOf("r1"), reports.deleted)
        val state = viewModel.uiState.value
        assertTrue(state.isDeleteFinished)
        assertEquals(false, state.isDeleting)
    }

    @Test
    fun ownDeletionIsNotShownAsDeletedElsewhere() = runTest {
        reports.report = r1
        val gate = CompletableDeferred<Unit>()
        reports.deleteGate = gate
        val viewModel = createViewModel()

        viewModel.onDelete()
        reports.report = null // the listener sees the local delete before the server confirms it

        assertTrue(viewModel.uiState.value.isDeleting)
        assertEquals(false, viewModel.uiState.value.isDeleted)

        gate.complete(Unit)

        assertTrue(viewModel.uiState.value.isDeleteFinished)
        assertEquals(false, viewModel.uiState.value.isDeleted)
    }

    @Test
    fun failedDeleteShowsTheMessageAndKeepsTheReport() = runTest {
        reports.report = r1
        reports.deleteError = DataException.Unavailable("offline")
        val viewModel = createViewModel()

        viewModel.onDelete()

        val state = viewModel.uiState.value
        assertEquals(offline, state.deleteError)
        assertEquals(false, state.isDeleting)
        assertEquals("Raport r1", state.report?.title)
    }

    @Test
    fun readErrorShowsTheMessageAndRetryLoadsAgain() = runTest {
        reports.failReport(DataException.Unavailable("offline"))
        val viewModel = createViewModel()
        assertEquals(ReportDetailsUiState(error = offline), viewModel.uiState.value)

        reports.report = r1
        viewModel.onRetry()

        assertEquals("Raport r1", viewModel.uiState.value.report?.title)
        assertNull(viewModel.uiState.value.error)
    }
}