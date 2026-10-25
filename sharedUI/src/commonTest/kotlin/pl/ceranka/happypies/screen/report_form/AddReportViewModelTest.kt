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
import pl.ceranka.happypies.fakes.accountOf
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.trainer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Also covers what [ReportFormViewModel] shares with editing: validation, the session and the save lock. */
@OptIn(ExperimentalCoroutinesApi::class)
class AddReportViewModelTest {

    private val reports = FakeReportRepository()
    private var account = accountOf(trainer)
    private val viewModel by lazy { AddReportViewModel(reports, account, anna.id) }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun fill(title: String, description: String = "") {
        viewModel.titleState.setTextAndPlaceCursorAtEnd(title)
        viewModel.descriptionState.setTextAndPlaceCursorAtEnd(description)
    }

    @Test
    fun startsWithAnEmptyForm() {
        assertEquals(ReportFormUiState(), viewModel.uiState.value)
        assertEquals("", viewModel.titleState.text.toString())
    }

    @Test
    fun blankTitleIsRejected() {
        fill(title = "   ")

        viewModel.onSave()

        assertEquals("Podaj tytuł raportu", viewModel.uiState.value.titleError)
        assertTrue(reports.created.isEmpty())
    }

    @Test
    fun savesWithTheAuthorFromTheSession() {
        fill(title = "  Przywołanie  ", description = "  Trzy próby ")

        viewModel.onSave()

        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(
            NewReport(anna.id, "Przywołanie", "Trzy próby", authorId = trainer.uid, authorRole = UserRole.TRAINER),
            reports.created.single()
        )
    }

    @Test
    fun blankDescriptionIsSavedAsNull() {
        fill(title = "Przywołanie", description = "   ")

        viewModel.onSave()

        assertEquals(null, reports.created.single().description)
    }

    @Test
    fun failedSaveShowsTheErrorAndAllowsRetry() {
        reports.createError = DataException.Unavailable("offline")
        fill(title = "Przywołanie")

        viewModel.onSave()

        val state = viewModel.uiState.value
        assertEquals("Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie.", state.saveError)
        assertFalse(state.isSaving)
        assertTrue(state.saveButtonEnabled)
    }

    @Test
    fun tappingSaveTwiceWhileSavingCreatesOneReport() {
        val gate = CompletableDeferred<Unit>()
        reports.createGate = gate
        fill(title = "Przywołanie")

        viewModel.onSave()
        viewModel.onSave()
        assertTrue(viewModel.uiState.value.isSaving)
        assertFalse(viewModel.uiState.value.saveButtonEnabled)
        gate.complete(Unit)

        assertEquals(1, reports.created.size)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun missingProfileAsksToSignInAgain() {
        account = accountOf(null)
        fill(title = "Przywołanie")

        viewModel.onSave()

        assertEquals("Sesja wygasła. Zaloguj się ponownie.", viewModel.uiState.value.saveError)
        assertTrue(reports.created.isEmpty())
    }
}