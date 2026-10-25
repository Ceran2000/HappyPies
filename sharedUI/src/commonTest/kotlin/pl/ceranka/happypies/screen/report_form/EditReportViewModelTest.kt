package pl.ceranka.happypies.screen.report_form

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.fakes.FakeReportRepository
import pl.ceranka.happypies.fakes.accountOf
import pl.ceranka.happypies.fakes.report
import pl.ceranka.happypies.fakes.trainer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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

    private fun EditReportViewModel.fill(title: String, description: String = "") {
        titleState.setTextAndPlaceCursorAtEnd(title)
        descriptionState.setTextAndPlaceCursorAtEnd(description)
    }

    @Test
    fun fillsTheFormWithTheReport() = runTest {
        val viewModel = createViewModel()

        assertTrue(viewModel.uiState.value.showForm)
        assertEquals("Przywołanie", viewModel.titleState.text.toString())
        assertEquals("Trzy próby", viewModel.descriptionState.text.toString())
    }

    @Test
    fun savesTheChangesAsTheViewer() = runTest {
        val viewModel = createViewModel()
        viewModel.fill(title = " Przywołanie z gwizdkiem ", description = "")

        viewModel.onSave()

        assertTrue(viewModel.uiState.value.form.isSaved)
        assertEquals(
            FakeReportRepository.Update(existing.id, "Przywołanie z gwizdkiem", null, editorId = trainer.uid),
            reports.updated.single()
        )
    }

    @Test
    fun savingWithoutChangesClosesTheFormWithoutWriting() = runTest {
        val viewModel = createViewModel()
        viewModel.fill(title = "Przywołanie  ", description = " Trzy próby")

        viewModel.onSave()

        assertTrue(viewModel.uiState.value.form.isSaved)
        assertTrue(reports.updated.isEmpty())
    }

    @Test
    fun failedLoadShowsTheErrorAndRetryLoadsTheReport() = runTest {
        reports.getError = DataException.Unavailable("offline")
        val viewModel = createViewModel()

        assertEquals("Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie.", viewModel.uiState.value.loadError)
        assertFalse(viewModel.uiState.value.showForm)

        reports.getError = null
        viewModel.onRetry()

        assertEquals(null, viewModel.uiState.value.loadError)
        assertTrue(viewModel.uiState.value.showForm)
        assertEquals("Przywołanie", viewModel.titleState.text.toString())
    }

    @Test
    fun saveIsIgnoredUntilTheReportIsLoaded() = runTest {
        reports.getError = DataException.Unavailable("offline")
        val viewModel = createViewModel()
        viewModel.fill(title = "Nowy tytuł")

        viewModel.onSave()

        assertFalse(viewModel.uiState.value.form.isSaving)
        assertTrue(reports.updated.isEmpty())
    }

    @Test
    fun failedUpdateKeepsTheFormOpenWithTheError() = runTest {
        reports.updateError = DataException.Unavailable("offline")
        val viewModel = createViewModel()
        viewModel.fill(title = "Nowy tytuł")

        viewModel.onSave()

        val form = viewModel.uiState.value.form
        assertFalse(form.isSaved)
        assertEquals("Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie.", form.saveError)
        assertEquals("Nowy tytuł", viewModel.titleState.text.toString())
    }
}