package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.report_form_session_expired
import happypies.sharedui.generated.resources.report_form_title_required
import happypies.sharedui.generated.resources.report_form_video_unreadable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.accountOrNull
import pl.ceranka.happypies.data.media.MediaRepository
import pl.ceranka.happypies.data.report.MediaItem
import pl.ceranka.happypies.data.report.MediaType
import pl.ceranka.happypies.data.report.Report
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.ui.common.toUserMessage

class EditReportViewModel(
    private val reportRepository: ReportRepository,
    private val mediaRepository: MediaRepository,
    private val accountRepository: AccountRepository,
    private val reportId: String
) : ViewModel() {

    private val log = Logger.withTag("EditReport")

    private val reportFormFields = ReportFormFields()
    val titleState: TextFieldState get() = reportFormFields.titleState
    val descriptionState: TextFieldState get() = reportFormFields.descriptionState

    private val _uiState = MutableStateFlow<EditReportUiState>(EditReportUiState.Loading)
    val uiState: StateFlow<EditReportUiState> = _uiState.asStateFlow()

    fun onVideosPicked(videos: List<ReportVideo.Picked>) {
        val readable = videos.filter { it.sizeBytes >= 0 }
        val videoErrorMessage = if (readable.size < videos.size) Res.string.report_form_video_unreadable else null
        updateEditing { it.copy(videos = it.videos.withPicked(readable), videoErrorMessage = videoErrorMessage) }
    }

    fun onRemoveVideoClick(video: ReportVideo) {
        updateEditing { it.copy(videos = it.videos - video, videoErrorMessage = null) }
    }

    fun onSaveClick() {
        val state = uiState.value.editingOrNull ?: return
        clearErrors()
        if (reportFormFields.title.isEmpty()) return showTitleRequiredError()
        val viewer = accountRepository.state.value.accountOrNull ?: return showSessionExpiredError()

        if (!fieldsDifferFrom(state.report, state.videos)) {
            return updateEditing { it.copy(saveStatus = SaveStatus.Saved) }
        }
        updateReport(state.report, viewer)
    }

    private fun updateReport(report: Report, viewer: Account) {
        updateEditing { it.copy(saveStatus = it.videos.firstSaveStatus()) }
        viewModelScope.launch {
            try {
                uploadPickedVideos(report.clientId)
                updateEditing { it.copy(saveStatus = SaveStatus.Saving) }
                val videos = uiState.value.editingOrNull?.videos.orEmpty().uploadedItems()
                val photos = report.media.filter { it.type == MediaType.PHOTO }
                reportRepository.updateReport(reportId, reportFormFields.title, reportFormFields.description, photos + videos, editorId = viewer.uid)
                deleteRemovedVideos(report.media.filter { it.type == MediaType.VIDEO } - videos.toSet())
                updateEditing { it.copy(saveStatus = SaveStatus.Saved) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Saving report $reportId failed" }
                updateEditing { it.copy(saveStatus = SaveStatus.Idle, saveErrorMessage = e.toUserMessage()) }
            }
        }
    }

    private suspend fun uploadPickedVideos(clientId: String) {
        mediaRepository.uploadPickedVideos(
            clientId = clientId,
            videos = uiState.value.editingOrNull?.videos.orEmpty(),
            onProgress = { percent -> updateEditing { it.copy(saveStatus = SaveStatus.Uploading(percent)) } },
            onUploaded = { picked, uploaded -> updateEditing { it.copy(videos = it.videos.replace(picked, uploaded)) } }
        )
    }

    private suspend fun deleteRemovedVideos(removed: List<MediaItem>) {
        for (item in removed) {
            try {
                mediaRepository.delete(item)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.w(e) { "Deleting removed video ${item.storagePath} of report $reportId failed" }
            }
        }
    }

    fun onRetryClick() {
        loadReport()
    }

    private fun loadReport() {
        _uiState.update { EditReportUiState.Loading }

        viewModelScope.launch {
            try {
                val report = reportRepository.getReport(reportId)
                fillFields(report)
                _uiState.update {
                    EditReportUiState.Editing(
                        report = report,
                        saveStatus = SaveStatus.Idle,
                        titleFieldErrorMessage = null,
                        saveErrorMessage = null,
                        videos = report.media.toReportVideos(),
                        videoErrorMessage = null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Loading report $reportId for editing failed" }
                _uiState.update { EditReportUiState.LoadError(e.toUserMessage()) }
            }
        }
    }

    private fun fillFields(report: Report) {
        reportFormFields.titleState.setTextAndPlaceCursorAtEnd(report.title)
        reportFormFields.descriptionState.setTextAndPlaceCursorAtEnd(report.description.orEmpty())
    }

    private fun fieldsDifferFrom(report: Report, videos: List<ReportVideo>): Boolean =
        reportFormFields.title != report.title ||
            reportFormFields.description != report.description ||
            videos != report.media.toReportVideos()

    private fun clearErrors() {
        updateEditing { it.copy(titleFieldErrorMessage = null, saveErrorMessage = null, videoErrorMessage = null) }
    }

    private fun showTitleRequiredError() {
        updateEditing { it.copy(titleFieldErrorMessage = Res.string.report_form_title_required) }
    }

    private fun showSessionExpiredError() {
        updateEditing { it.copy(saveErrorMessage = Res.string.report_form_session_expired) }
    }

    private fun updateEditing(transform: (EditReportUiState.Editing) -> EditReportUiState.Editing) {
        _uiState.update { state ->
            when (state) {
                is EditReportUiState.Editing -> transform(state)
                EditReportUiState.Loading, is EditReportUiState.LoadError -> state
            }
        }
    }

    init {
        loadReport()
    }
}

private val EditReportUiState.editingOrNull: EditReportUiState.Editing?
    get() = when (this) {
        is EditReportUiState.Editing -> this
        EditReportUiState.Loading, is EditReportUiState.LoadError -> null
    }