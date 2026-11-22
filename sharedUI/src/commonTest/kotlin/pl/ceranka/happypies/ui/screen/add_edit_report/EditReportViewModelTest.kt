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
import pl.ceranka.happypies.data.report.MediaItem
import pl.ceranka.happypies.data.report.MediaType
import pl.ceranka.happypies.fakes.FakeMediaRepository
import pl.ceranka.happypies.fakes.FakePhotoCompressor
import pl.ceranka.happypies.fakes.FakeVideoCompressor
import pl.ceranka.happypies.fakes.FakeReportRepository
import pl.ceranka.happypies.fakes.accountOf
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.report
import pl.ceranka.happypies.fakes.trainer
import pl.ceranka.happypies.fakes.uploadedMedia
import pl.ceranka.happypies.fakes.uploadedVideo
import pl.ceranka.happypies.fakes.video
import pl.ceranka.happypies.ui.media.PickedFile
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
    private val media = FakeMediaRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    // The state is shared WhileSubscribed, so a test collects it like the screen does.
    private fun TestScope.createViewModel() =
        EditReportViewModel(reports, media, accountOf(trainer), ReportMediaPreparer(FakePhotoCompressor(), FakeVideoCompressor()), existing.id).also { viewModel ->
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
            FakeReportRepository.Update(existing.id, "Przywołanie z gwizdkiem", null, media = emptyList(), editorId = trainer.uid),
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

    private val photo = MediaItem(MediaType.PHOTO, "clients/client-1/media/p1", "https://example.com/p1", 500_000)

    private fun uploaded(item: MediaItem) = ReportMedia.Uploaded(item, previewUri = item.downloadUrl)

    private fun storeWithMedia() {
        reports.stored[existing.id] = existing.copy(media = listOf(photo, video))
    }

    @Test
    fun showsTheMediaOfTheReport() = runTest {
        storeWithMedia()
        val viewModel = createViewModel()

        assertEquals(listOf(uploaded(photo), uploaded(video)), viewModel.editing.media)
    }

    @Test
    fun removedFilesAreLeftOutOfTheSavedReport() = runTest {
        storeWithMedia()
        val viewModel = createViewModel()
        viewModel.onRemoveMediaClick(uploaded(video))
        viewModel.onRemoveMediaClick(uploaded(photo))

        viewModel.onSaveClick()

        assertEquals(emptyList(), reports.updated.single().media)
        assertTrue(viewModel.editing.isSaved)
    }

    @Test
    fun replacedVideoIsUploadedInPlaceOfTheOldOne() = runTest {
        storeWithMedia()
        val viewModel = createViewModel()
        viewModel.onRemoveMediaClick(uploaded(video))
        viewModel.onMediaPicked(listOf(PickedFile("file:///new.mov", "video/mp4", 2_000)))

        viewModel.onSaveClick()

        assertEquals(listOf(photo, uploadedVideo("file:///new.mov", 1_000)), reports.updated.single().media)
        assertEquals(anna.id, media.uploads.single().clientId)
    }

    @Test
    fun addedPhotoIsCompressedAndUploaded() = runTest {
        storeWithMedia()
        val viewModel = createViewModel()
        viewModel.onMediaPicked(listOf(PickedFile("file:///p2.heic", "image/heic", 3_000_000)))

        viewModel.onSaveClick()

        assertEquals(
            listOf(photo, video, uploadedMedia(MediaType.PHOTO, "file:///p2.heic.jpg", 300_000)),
            reports.updated.single().media
        )
    }
}