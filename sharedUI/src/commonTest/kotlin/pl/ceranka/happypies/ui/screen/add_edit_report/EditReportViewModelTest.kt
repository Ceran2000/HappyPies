package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.error_unavailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.fakes.FakeReportRepository
import pl.ceranka.happypies.fakes.accountOf
import pl.ceranka.happypies.fakes.report
import pl.ceranka.happypies.fakes.trainer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EditReportViewModelTest {

    private val existing = report("r1").copy(title = "Przywołanie", description = "Trzy próby")
    private val reports = FakeReportRepository().apply { stored[existing.id] = existing }

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // The state is shared WhileSubscribed, so a test collects it like the screen does.
    private fun TestScope.createViewModel() =
        EditReportViewModel(reports, accountOf(trainer), existing.id).also { viewModel ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
        }

    private val EditReportViewModel.editing: EditReportUiState.Editing
        get() = assertIs<EditReportUiState.Editing>(uiState.value)

    private fun EditReportViewModel.fill(title: String, description: String = "") {
        titleState.setTextAndPlaceCursorAtEnd(title)
        descriptionState.setTextAndPlaceCursorAtEnd(description)
    }

    @Test
    fun fillsTheFormWithTheReport() = runTest {
        val viewModel = createViewModel()

        assertIs<EditReportUiState.Editing>(viewModel.uiState.value)
        assertEquals("Przywołanie", viewModel.titleState.text.toString())
        assertEquals("Trzy próby", viewModel.descriptionState.text.toString())
    }

    @Test
    fun savesTheChangesAsTheViewer() = runTest {
        val viewModel = createViewModel()
        viewModel.fill(title = " Przywołanie z gwizdkiem ", description = "")

        viewModel.onSaveClick()

        assertTrue(viewModel.editing.isSaved)
        assertEquals(
            FakeReportRepository.Update(existing.id, "Przywołanie z gwizdkiem", null, editorId = trainer.uid),
            reports.updated.single()
        )
    }

    @Test
    fun savingWithoutChangesClosesTheFormWithoutWriting() = runTest {
        val viewModel = createViewModel()
        viewModel.fill(title = "Przywołanie  ", description = " Trzy próby")

        viewModel.onSaveClick()

        assertTrue(viewModel.editing.isSaved)
        assertTrue(reports.updated.isEmpty())
    }

    @Test
    fun changedTitleIsSaved() = runTest {
        val viewModel = createViewModel()
        viewModel.fill(title = "Przywołanie z gwizdkiem", description = "Trzy próby")

        viewModel.onSaveClick()

        assertEquals("Przywołanie z gwizdkiem", reports.updated.single().title)
    }

    @Test
    fun clearedDescriptionIsSaved() = runTest {
        val viewModel = createViewModel()
        viewModel.fill(title = "Przywołanie", description = "")

        viewModel.onSaveClick()

        assertEquals(null, reports.updated.single().description)
    }

    @Test
    fun reportWithoutDescriptionFillsAnEmptyField() = runTest {
        reports.stored[existing.id] = existing.copy(description = null)
        val viewModel = createViewModel()

        assertEquals("", viewModel.descriptionState.text.toString())
    }

    @Test
    fun failedLoadShowsTheErrorAndRetryLoadsTheReport() = runTest {
        reports.getError = DataException.Unavailable("offline")
        val viewModel = createViewModel()

        assertEquals(EditReportUiState.LoadError(Res.string.error_unavailable), viewModel.uiState.value)

        reports.getError = null
        viewModel.onRetryClick()

        assertIs<EditReportUiState.Editing>(viewModel.uiState.value)
        assertEquals("Przywołanie", viewModel.titleState.text.toString())
    }

    @Test
    fun saveIsIgnoredUntilTheReportIsLoaded() = runTest {
        reports.getError = DataException.Unavailable("offline")
        val viewModel = createViewModel()
        viewModel.fill(title = "Nowy tytuł")

        viewModel.onSaveClick()

        assertIs<EditReportUiState.LoadError>(viewModel.uiState.value)
        assertTrue(reports.updated.isEmpty())
    }

    @Test
    fun failedUpdateKeepsTheFormOpenWithTheError() = runTest {
        reports.updateError = DataException.Unavailable("offline")
        val viewModel = createViewModel()
        viewModel.fill(title = "Nowy tytuł")

        viewModel.onSaveClick()

        val state = viewModel.editing
        assertFalse(state.isSaved)
        assertEquals(Res.string.error_unavailable, state.saveErrorMessage)
        assertEquals("Nowy tytuł", viewModel.titleState.text.toString())
    }
}