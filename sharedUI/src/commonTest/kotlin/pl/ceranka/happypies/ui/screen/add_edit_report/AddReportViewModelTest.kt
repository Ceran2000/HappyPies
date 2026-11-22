package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.error_unavailable
import happypies.sharedui.generated.resources.report_form_session_expired
import happypies.sharedui.generated.resources.report_form_title_required
import happypies.sharedui.generated.resources.report_form_media_too_large
import happypies.sharedui.generated.resources.report_form_media_unreadable
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.data.report.MediaType
import pl.ceranka.happypies.data.report.NewReport
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.fakes.FakeMediaRepository
import pl.ceranka.happypies.fakes.FakePhotoCompressor
import pl.ceranka.happypies.fakes.FakeVideoCompressor
import pl.ceranka.happypies.fakes.FakeReportRepository
import pl.ceranka.happypies.fakes.accountOf
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.trainer
import pl.ceranka.happypies.fakes.uploadedMedia
import pl.ceranka.happypies.fakes.uploadedVideo
import pl.ceranka.happypies.ui.media.PickedFile
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
    private val photos = FakePhotoCompressor()
    private val videos = FakeVideoCompressor()
    private var account = accountOf(trainer)
    private val viewModel by lazy { AddReportViewModel(reports, media, account, ReportMediaPreparer(photos, videos), anna.id) }

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

    private fun videoFile(name: String, sizeBytes: Long = 1_000) = PickedFile("file:///$name.mov", "video/quicktime", sizeBytes)

    private fun photoFile(name: String) = PickedFile("file:///$name.heic", "image/heic", 3_000_000)

    private fun compressedVideo(name: String, sizeBytes: Long = 1_000) = ReportMedia.Compressed("file:///$name.mov", "file:///$name.mov", sizeBytes, MediaType.VIDEO)

    private fun compressedPhoto(name: String) = ReportMedia.Compressed("file:///$name.heic", "file:///$name.heic.jpg", 300_000, MediaType.PHOTO)

    @Test
    fun uploadsPickedMediaBeforeSavingTheReport() {
        fill(title = "Przywołanie")
        viewModel.onMediaPicked(listOf(photoFile("p"), videoFile("a")))

        viewModel.onSaveClick()

        assertEquals(
            listOf(
                FakeMediaRepository.Upload(anna.id, MediaType.PHOTO, "file:///p.heic.jpg", 300_000),
                FakeMediaRepository.Upload(anna.id, MediaType.VIDEO, "file:///a.mov", 1_000)
            ),
            media.uploads
        )
        assertEquals(
            listOf(uploadedMedia(MediaType.PHOTO, "file:///p.heic.jpg", 300_000), uploadedVideo("file:///a.mov", 1_000)),
            reports.created.single().media
        )
        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun photosAreCompressedWhenPicked() {
        viewModel.onMediaPicked(listOf(photoFile("p"), videoFile("a")))

        assertEquals(listOf("file:///p.heic"), photos.compressed)
        assertEquals(listOf(compressedPhoto("p"), compressedVideo("a")), viewModel.uiState.value.media)
    }

    @Test
    fun saveAndAddAreDisabledWhilePhotosArePrepared() {
        val gate = CompletableDeferred<Unit>()
        photos.gate = gate

        viewModel.onMediaPicked(listOf(photoFile("p")))
        assertEquals(listOf(ReportMedia.Compressing("file:///p.heic", MediaType.PHOTO, percent = 0)), viewModel.uiState.value.media)
        assertFalse(viewModel.uiState.value.saveButtonEnabled)
        assertFalse(viewModel.uiState.value.addMediaButtonEnabled)
        gate.complete(Unit)

        assertEquals(listOf(compressedPhoto("p")), viewModel.uiState.value.media)
        assertTrue(viewModel.uiState.value.saveButtonEnabled)
    }

    @Test
    fun showsTheProgressOfAllFilesTogether() {
        val gate = CompletableDeferred<Unit>()
        media.uploadGate = gate
        fill(title = "Przywołanie")
        videos.compressedSizes["file:///b.mov"] = 3_000
        viewModel.onMediaPicked(listOf(videoFile("a"), videoFile("b")))

        viewModel.onSaveClick()
        assertEquals(SaveStatus.Uploading(totalPercent = 12), viewModel.uiState.value.saveStatus)
        assertFalse(viewModel.uiState.value.cancelButtonEnabled)
        assertFalse(viewModel.uiState.value.showRemoveMediaButtons)
        assertFalse(viewModel.uiState.value.showAddPhotosButton)
        assertFalse(viewModel.uiState.value.showAddVideosButton)
        gate.complete(Unit)

        assertTrue(viewModel.uiState.value.isSaved)
    }

    @Test
    fun failedUploadKeepsTheUploadedFilesForTheNextAttempt() {
        media.failingUris += "file:///b.mov"
        fill(title = "Przywołanie")
        viewModel.onMediaPicked(listOf(videoFile("a"), videoFile("b")))

        viewModel.onSaveClick()

        val failed = viewModel.uiState.value
        assertEquals(Res.string.error_unavailable, failed.saveErrorMessage)
        assertEquals(listOf(ReportMedia.Uploaded(uploadedVideo("file:///a.mov", 1_000), previewUri = "file:///a.mov"), compressedVideo("b")), failed.media)
        assertTrue(reports.created.isEmpty())

        media.failingUris.clear()
        viewModel.onSaveClick()

        assertEquals(listOf("file:///a.mov", "file:///b.mov", "file:///b.mov"), media.uploads.map { it.localFileUri })
        assertEquals(2, reports.created.single().media.size)
    }

    @Test
    fun keepsAllPickedFilesButBlocksSavingOverTheLimit() {
        fill(title = "Przywołanie")
        viewModel.onMediaPicked(listOf("a", "b", "c", "d").map { videoFile(it) } + listOf("p1", "p2").map { photoFile(it) })

        val state = viewModel.uiState.value
        assertEquals(listOf("a", "b", "c", "d").map { compressedVideo(it) } + listOf("p1", "p2").map { compressedPhoto(it) }, state.media)
        assertTrue(state.showMediaLimitError)
        assertFalse(state.saveButtonEnabled)
        assertFalse(state.showAddVideosButton)
        assertTrue(state.showAddPhotosButton)
        assertEquals(3, state.photoPickerMaxItems)
    }

    @Test
    fun pickersAllowOnlyTheFreeSlotsOfTheirType() {
        viewModel.onMediaPicked(listOf(photoFile("p1"), photoFile("p2"), videoFile("a")))

        val state = viewModel.uiState.value
        assertEquals(3, state.photoPickerMaxItems)
        assertEquals(2, state.videoPickerMaxItems)
        assertTrue(state.showAddPhotosButton)
        assertTrue(state.showAddVideosButton)
    }

    @Test
    fun addPhotosButtonDisappearsWhenThePhotoLimitIsReached() {
        viewModel.onMediaPicked(listOf("p1", "p2", "p3", "p4", "p5").map { photoFile(it) })

        assertFalse(viewModel.uiState.value.showAddPhotosButton)
        assertTrue(viewModel.uiState.value.showAddVideosButton)
        assertEquals(3, viewModel.uiState.value.videoPickerMaxItems)
    }

    @Test
    fun removingTheExtraFileAllowsSaving() {
        fill(title = "Przywołanie")
        viewModel.onMediaPicked(listOf("a", "b", "c", "d").map { videoFile(it) })

        viewModel.onRemoveMediaClick(compressedVideo("d"))

        assertFalse(viewModel.uiState.value.showMediaLimitError)
        assertTrue(viewModel.uiState.value.saveButtonEnabled)
    }

    @Test
    fun uploadedPhotoKeepsShowingTheLocalFile() {
        media.failingUris += "file:///a.mov"
        fill(title = "Przywołanie")
        viewModel.onMediaPicked(listOf(photoFile("p"), videoFile("a")))

        viewModel.onSaveClick()

        assertEquals(
            ReportMedia.Uploaded(uploadedMedia(MediaType.PHOTO, "file:///p.heic.jpg", 300_000), previewUri = "file:///p.heic.jpg"),
            viewModel.uiState.value.media.first()
        )
    }

    @Test
    fun theSameVideoPickedTwiceIsAddedOnce() {
        viewModel.onMediaPicked(listOf(videoFile("a")))
        viewModel.onMediaPicked(listOf(videoFile("a"), videoFile("a"), videoFile("b")))

        assertEquals(listOf(compressedVideo("a"), compressedVideo("b")), viewModel.uiState.value.media)
    }

    @Test
    fun videosAreCompressedWithinTheReportLimitWhenPicked() {
        videos.compressedSizes["file:///a.mov"] = 20_000_000
        viewModel.onMediaPicked(listOf(videoFile("a", sizeBytes = 80_000_000)))

        assertEquals(listOf("file:///a.mov" to MediaType.VIDEO.maxReportSizeBytes()), videos.compressed)
        assertEquals(listOf(compressedVideo("a", sizeBytes = 20_000_000)), viewModel.uiState.value.media)
    }

    @Test
    fun everyPickedFileGetsATileWithItsOwnPreparationProgress() {
        val gate = CompletableDeferred<Unit>()
        videos.gate = gate

        viewModel.onMediaPicked(listOf(photoFile("p"), videoFile("a"), videoFile("b")))
        assertEquals(
            listOf(
                compressedPhoto("p"),
                ReportMedia.Compressing("file:///a.mov", MediaType.VIDEO, percent = 50),
                ReportMedia.Compressing("file:///b.mov", MediaType.VIDEO, percent = 0)
            ),
            viewModel.uiState.value.media
        )
        assertEquals(2, viewModel.uiState.value.media.countOf(MediaType.VIDEO))
        gate.complete(Unit)

        assertEquals(listOf(compressedPhoto("p"), compressedVideo("a"), compressedVideo("b")), viewModel.uiState.value.media)
    }

    @Test
    fun filesOverTheSizeLimitAreRejected() {
        photos.compressedSizes["file:///big.heic"] = MediaType.PHOTO.maxReportSizeBytes() + 1
        videos.compressedSizes["file:///long.mov"] = MediaType.VIDEO.maxReportSizeBytes() + 1
        viewModel.onMediaPicked(listOf(photoFile("big"), videoFile("long"), videoFile("b")))

        assertEquals(listOf(compressedVideo("b")), viewModel.uiState.value.media)
        assertEquals(Res.string.report_form_media_too_large, viewModel.uiState.value.mediaErrorMessage)
    }

    @Test
    fun failedVideoCompressionIsReportedAsUnreadable() {
        videos.failingUris += "file:///a.mov"
        viewModel.onMediaPicked(listOf(videoFile("a")))

        assertTrue(viewModel.uiState.value.media.isEmpty())
        assertEquals(Res.string.report_form_media_unreadable, viewModel.uiState.value.mediaErrorMessage)
    }

    @Test
    fun unreadableFilesAreRejected() {
        photos.failingUris += "file:///broken.heic"
        viewModel.onMediaPicked(
            listOf(
                videoFile("unknown-size", sizeBytes = -1),
                PickedFile("file:///doc.pdf", "application/pdf", 1_000),
                PickedFile("file:///no-type", null, 1_000),
                photoFile("broken"),
                videoFile("b")
            )
        )

        assertEquals(listOf(compressedVideo("b")), viewModel.uiState.value.media)
        assertEquals(Res.string.report_form_media_unreadable, viewModel.uiState.value.mediaErrorMessage)
    }

    @Test
    fun removedMediaIsNotUploaded() {
        fill(title = "Przywołanie")
        viewModel.onMediaPicked(listOf(videoFile("a"), photoFile("p")))
        viewModel.onRemoveMediaClick(compressedVideo("a"))

        viewModel.onSaveClick()

        assertEquals(listOf("file:///p.heic.jpg"), media.uploads.map { it.localFileUri })
    }
}