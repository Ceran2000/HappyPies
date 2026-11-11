package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.error_unavailable
import happypies.sharedui.generated.resources.report_form_session_expired
import happypies.sharedui.generated.resources.report_form_title_required
import happypies.sharedui.generated.resources.report_form_video_unreadable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.data.report.NewReport
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.fakes.FakeMediaRepository
import pl.ceranka.happypies.fakes.FakeReportRepository
import pl.ceranka.happypies.fakes.accountOf
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.trainer
import pl.ceranka.happypies.fakes.uploadedVideo
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
    private val media = FakeMediaRepository()
    private var account = accountOf(trainer)
    private val viewModel by lazy { AddReportViewModel(reports, media, account, anna.id) }

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
            NewReport(anna.id, "Przywołanie", "Trzy próby", authorId = trainer.uid, authorRole = UserRole.TRAINER, media = emptyList()),
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
        assertEquals(SaveStatus.Idle, state.saveStatus)
        assertTrue(state.saveButtonEnabled)
    }

    @Test
    fun saveButtonIsDisabledWhileSaving() {
        val gate = CompletableDeferred<Unit>()
        reports.createGate = gate
        fill(title = "Przywołanie")

        viewModel.onSaveClick()
        assertEquals(SaveStatus.Saving, viewModel.uiState.value.saveStatus)
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

    private fun picked(name: String, sizeBytes: Long = 1_000) = ReportVideo.Picked("file:///$name.mov", sizeBytes)

    @Test
    fun uploadsPickedVideosBeforeSavingTheReport() {
        fill(title = "Przywołanie")
        viewModel.onVideosPicked(listOf(picked("a"), picked("b")))

        viewModel.onSaveClick()

        assertEquals(listOf("file:///a.mov", "file:///b.mov"), media.uploads.map { it.localFileUri })
        assertEquals(listOf(uploadedVideo("file:///a.mov", 1_000), uploadedVideo("file:///b.mov", 1_000)), reports.created.single().media)
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun showsTheProgressOfAllVideosTogether() {
        val gate = CompletableDeferred<Unit>()
        media.uploadGate = gate
        fill(title = "Przywołanie")
        viewModel.onVideosPicked(listOf(picked("a", sizeBytes = 1_000), picked("b", sizeBytes = 3_000)))

        viewModel.onSaveClick()
        assertEquals(SaveStatus.Uploading(12), viewModel.uiState.value.saveStatus)
        assertFalse(viewModel.uiState.value.cancelButtonEnabled)
        gate.complete(Unit)

        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun failedUploadKeepsTheUploadedVideosForTheNextAttempt() {
        media.failingUris += "file:///b.mov"
        fill(title = "Przywołanie")
        viewModel.onVideosPicked(listOf(picked("a"), picked("b")))

        viewModel.onSaveClick()

        val failed = viewModel.uiState.value
        assertEquals(Res.string.error_unavailable, failed.saveErrorMessage)
        assertEquals(listOf(ReportVideo.Uploaded(uploadedVideo("file:///a.mov", 1_000)), picked("b")), failed.videos)
        assertTrue(reports.created.isEmpty())

        media.failingUris.clear()
        viewModel.onSaveClick()

        assertEquals(listOf("file:///a.mov", "file:///b.mov", "file:///b.mov"), media.uploads.map { it.localFileUri })
        assertEquals(2, reports.created.single().media.size)
    }

    @Test
    fun keepsAtMostThreeVideos() {
        viewModel.onVideosPicked(listOf(picked("a"), picked("b")))
        viewModel.onVideosPicked(listOf(picked("c"), picked("d")))

        assertEquals(listOf(picked("a"), picked("b"), picked("c")), viewModel.uiState.value.videos)
        assertFalse(viewModel.uiState.value.addVideoButtonEnabled)
    }

    @Test
    fun videoWithUnknownSizeIsRejected() {
        viewModel.onVideosPicked(listOf(picked("a", sizeBytes = -1), picked("b")))

        assertEquals(listOf(picked("b")), viewModel.uiState.value.videos)
        assertEquals(Res.string.report_form_video_unreadable, viewModel.uiState.value.videoErrorMessage)
    }

    @Test
    fun removedVideoIsNotUploaded() {
        fill(title = "Przywołanie")
        viewModel.onVideosPicked(listOf(picked("a"), picked("b")))
        viewModel.onRemoveVideoClick(picked("a"))

        viewModel.onSaveClick()

        assertEquals(listOf("file:///b.mov"), media.uploads.map { it.localFileUri })
    }
}