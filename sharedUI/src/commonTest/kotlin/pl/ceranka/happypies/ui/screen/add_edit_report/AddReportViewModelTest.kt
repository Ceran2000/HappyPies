package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.error_unavailable
import happypies.sharedui.generated.resources.report_form_session_expired
import happypies.sharedui.generated.resources.report_form_title_required
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
        assertEquals(AddReportUiState.INITIAL, viewModel.uiState.value)
        assertEquals("", viewModel.titleState.text.toString())
    }

    @Test
    fun blankTitleIsRejected() {
        fill(title = "   ")

        viewModel.onSaveClick()

        assertEquals(Res.string.report_form_title_required, viewModel.uiState.value.titleFieldErrorMessage)
        assertTrue(reports.created.isEmpty())
    }

    @Test
    fun savesWithTheAuthorFromTheSession() {
        fill(title = "  Przywołanie  ", description = "  Trzy próby ")

        viewModel.onSaveClick()

        assertTrue(viewModel.uiState.value.isSaved)
        assertEquals(
            NewReport(anna.id, "Przywołanie", "Trzy próby", authorId = trainer.uid, authorRole = UserRole.TRAINER),
            reports.created.single()
        )
    }

    @Test
    fun blankDescriptionIsSavedAsNull() {
        fill(title = "Przywołanie", description = "   ")

        viewModel.onSaveClick()

        assertEquals(null, reports.created.single().description)
    }

    @Test
    fun failedSaveShowsTheErrorAndAllowsRetry() {
        reports.createError = DataException.Unavailable("offline")
        fill(title = "Przywołanie")

        viewModel.onSaveClick()

        val state = viewModel.uiState.value
        assertEquals(Res.string.error_unavailable, state.saveErrorMessage)
        assertEquals(SaveStatus.IDLE, state.saveStatus)
        assertTrue(state.saveButtonEnabled)
    }

    @Test
    fun saveButtonIsDisabledWhileSaving() {
        val gate = CompletableDeferred<Unit>()
        reports.createGate = gate
        fill(title = "Przywołanie")

        viewModel.onSaveClick()
        assertEquals(SaveStatus.SAVING, viewModel.uiState.value.saveStatus)
        assertFalse(viewModel.uiState.value.saveButtonEnabled)
        gate.complete(Unit)

        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun missingProfileAsksToSignInAgain() {
        account = accountOf(null)
        fill(title = "Przywołanie")

        viewModel.onSaveClick()

        assertEquals(Res.string.report_form_session_expired, viewModel.uiState.value.saveErrorMessage)
        assertTrue(reports.created.isEmpty())
    }

    @Test
    fun nextSaveAttemptClearsThePreviousErrors() {
        account = accountOf(null)
        viewModel.onSaveClick()
        fill(title = "Przywołanie")

        viewModel.onSaveClick()

        assertNull(viewModel.uiState.value.titleFieldErrorMessage)
        assertEquals(Res.string.report_form_session_expired, viewModel.uiState.value.saveErrorMessage)
    }
}