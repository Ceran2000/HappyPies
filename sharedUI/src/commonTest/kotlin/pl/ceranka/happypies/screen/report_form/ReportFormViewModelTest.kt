package pl.ceranka.happypies.screen.report_form

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.report.NewReport
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.fakes.FakeReportRepository
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.sessionOf
import pl.ceranka.happypies.fakes.trainer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReportFormViewModelTest {

    private val reports = FakeReportRepository()
    private var session = sessionOf(trainer)
    private val viewModel by lazy { ReportFormViewModel(reports, session, anna.id) }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun fill(title: String, description: String = "") {
        viewModel.titleState.setTextAndPlaceCursorAtEnd(title)
        viewModel.descriptionState.setTextAndPlaceCursorAtEnd(description)
    }

    @Test
    fun blankTitleIsRejected() {
        fill(title = "   ")

        viewModel.save()

        assertEquals(ReportFormUiState.Editing(titleError = "Podaj tytuł raportu"), viewModel.uiState.value)
        assertTrue(reports.created.isEmpty())
    }

    @Test
    fun savesWithTheAuthorFromTheSession() {
        fill(title = "  Przywołanie  ", description = "  Trzy próby ")

        viewModel.save()

        assertEquals(ReportFormUiState.Saved, viewModel.uiState.value)
        assertEquals(
            NewReport(anna.id, "Przywołanie", "Trzy próby", authorId = trainer.uid, authorRole = UserRole.TRAINER),
            reports.created.single(),
        )
    }

    @Test
    fun blankDescriptionIsSavedAsNull() {
        fill(title = "Przywołanie", description = "   ")

        viewModel.save()

        assertEquals(null, reports.created.single().description)
    }

    @Test
    fun failedSaveShowsTheErrorAndAllowsRetry() {
        reports.createError = DataException.Unavailable("offline")
        fill(title = "Przywołanie")

        viewModel.save()

        assertEquals(
            ReportFormUiState.Editing(saveError = "Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie."),
            viewModel.uiState.value,
        )
    }

    @Test
    fun tappingSaveTwiceWhileSavingCreatesOneReport() {
        val gate = CompletableDeferred<Unit>()
        reports.createGate = gate
        fill(title = "Przywołanie")

        viewModel.save()
        viewModel.save()
        assertEquals(ReportFormUiState.Saving, viewModel.uiState.value)
        gate.complete(Unit)

        assertEquals(1, reports.created.size)
        assertEquals(ReportFormUiState.Saved, viewModel.uiState.value)
    }

    @Test
    fun missingSessionAsksToSignInAgain() {
        session = sessionOf(null)
        fill(title = "Przywołanie")

        viewModel.save()

        assertEquals(ReportFormUiState.Editing(saveError = "Sesja wygasła. Zaloguj się ponownie."), viewModel.uiState.value)
        assertTrue(reports.created.isEmpty())
    }
}
