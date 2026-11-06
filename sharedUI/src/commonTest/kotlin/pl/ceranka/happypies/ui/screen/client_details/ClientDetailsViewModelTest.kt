package pl.ceranka.happypies.ui.screen.client_details

import androidx.paging.testing.asSnapshot
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
import pl.ceranka.happypies.fakes.FakeTrainerRepository
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.annasTrainer
import pl.ceranka.happypies.fakes.report
import pl.ceranka.happypies.fakes.trainer
import pl.ceranka.happypies.fakes.video
import pl.ceranka.happypies.ui.common.ReportListItem
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

    private fun TestScope.createViewModel() = ClientDetailsViewModel(clients, FakeTrainerRepository(), reports, anna.id, TimeZone.UTC).also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    @Test
    fun showsTheClientAndTheReportsWithResolvedAuthors() = runTest {
        reports.reports = listOf(
            report("r1", createdAt = 1_791_830_400_000, media = listOf(video)),
            report(
                "r2",
                createdAt = 1_791_191_100_000,
                authorId = trainer.uid,
                authorRole = UserRole.TRAINER,
                comment = ReportComment(anna.id, UserRole.CLIENT, "Dzięki", 2_000)
            )
        )

        val viewModel = createViewModel()

        assertEquals(ClientDetailsUiState.ShowContent(anna, annasTrainer), viewModel.uiState.value)
        assertEquals(
            listOf(
                ReportListItem("r1", "Raport r1", "12.10.2026, 18:40", "Anna Kowalska", mediaCount = 1, hasComment = false),
                ReportListItem("r2", "Raport r2", "05.10.2026, 09:05", "Jan Trener", mediaCount = 0, hasComment = true)
            ),
            viewModel.reports.asSnapshot()
        )
    }

    @Test
    fun cachedReloadShowsTheOfflineMessageUntilItIsShown() = runTest {
        val viewModel = createViewModel()
        assertEquals(false, viewModel.showOfflineMessage.value)

        reports.cachedReloads.emit(Unit)
        assertEquals(true, viewModel.showOfflineMessage.value)

        viewModel.onOfflineMessageShown()
        assertEquals(false, viewModel.showOfflineMessage.value)
    }

    @Test
    fun clientErrorShowsTheMessageAndRetryLoadsAgain() = runTest {
        clients.error = DataException.Unavailable("offline")
        val viewModel = createViewModel()
        assertEquals(ClientDetailsUiState.Error(offline), viewModel.uiState.value)

        clients.error = null
        reports.reports = listOf(report("r1"))
        viewModel.onRetry()

        assertEquals(ClientDetailsUiState.ShowContent(anna, annasTrainer), viewModel.uiState.value)
        assertEquals(listOf("r1"), viewModel.reports.asSnapshot().map { it.id })
    }
}