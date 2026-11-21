package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.report_form_session_expired
import happypies.sharedui.generated.resources.report_form_title_required
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
import pl.ceranka.happypies.data.report.Report
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.infrastructure.media.PhotoCompressor
import pl.ceranka.happypies.ui.common.toUserMessage
import pl.ceranka.happypies.ui.media.PickedFile

class EditReportViewModel(
    private val reportRepository: ReportRepository,
    private val mediaRepository: MediaRepository,
    private val accountRepository: AccountRepository,
    private val photoCompressor: PhotoCompressor,
    private val reportId: String
) : ViewModel() {

    private val log = Logger.withTag("EditReport")
    private val mediaUploader by lazy { ReportMediaUploader(mediaRepository) }

    private val reportFormFields = ReportFormFields()
    val titleState: TextFieldState get() = reportFormFields.titleState
    val descriptionState: TextFieldState get() = reportFormFields.descriptionState

    private val _uiState = MutableStateFlow<EditReportUiState>(EditReportUiState.Loading)
    val uiState: StateFlow<EditReportUiState> = _uiState.asStateFlow()

    fun onMediaPicked(files: List<PickedFile>) {
        updateEditing { it.copy(isPreparingMedia = true, mediaErrorMessage = null) }
        viewModelScope.launch {
            val prepared = files.map { photoCompressor.preparedMediaOrNull(it) }
            updateEditing {
                it.copy(
                    media = it.media.withPicked(prepared.filterNotNull()),
                    isPreparingMedia = false,
                    mediaErrorMessage = pickErrorMessage(prepared)
                )
            }
        }
    }

    fun onRemoveMediaClick(media: ReportMedia) {
        updateEditing { it.copy(media = it.media - media, mediaErrorMessage = null) }
    }

    fun onSaveClick() {
        val state = uiState.value.editingOrNull ?: return
        clearErrors()
        if (reportFormFields.title.isEmpty()) return showTitleRequiredError()
        val viewer = accountRepository.state.value.accountOrNull ?: return showSessionExpiredError()

        if (!fieldsDifferFrom(state.report, state.media)) {
            return updateEditing { it.copy(saveStatus = SaveStatus.Saved) }
        }
        updateReport(state.report, viewer)
    }

    private fun updateReport(report: Report, viewer: Account) {
        updateEditing { it.copy(saveStatus = it.media.firstSaveStatus(), media = it.media.withUploadsWaiting()) }
        viewModelScope.launch {
            try {
                uploadPickedMedia(report.clientId)
                updateEditing { it.copy(saveStatus = SaveStatus.Saving) }
                val media = uiState.value.editingOrNull?.media.orEmpty().uploadedItems()
                reportRepository.updateReport(reportId, reportFormFields.title, reportFormFields.description, media, editorId = viewer.uid)
                deleteRemovedMedia(report.media - media.toSet())
                updateEditing { it.copy(saveStatus = SaveStatus.Saved) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Saving report $reportId failed" }
                updateEditing { it.copy(saveStatus = SaveStatus.Idle, saveErrorMessage = e.toUserMessage(), media = it.media.withUploadsNotStarted()) }
            }
        }
    }

    private suspend fun uploadPickedMedia(clientId: String) {
        mediaUploader.uploadPicked(
            clientId = clientId,
            media = uiState.value.editingOrNull?.media.orEmpty(),
            onProgress = { file, filePercent, totalPercent ->
                updateEditing {
                    it.copy(saveStatus = SaveStatus.Uploading(totalPercent), media = it.media.withUploadProgress(file.localFileUri, filePercent))
                }
            },
            onUploaded = { file, item -> updateEditing { it.copy(media = it.media.withUploaded(file.localFileUri, item)) } }
        )
    }

    private suspend fun deleteRemovedMedia(removed: List<MediaItem>) {
        for (item in removed) {
            try {
                mediaRepository.delete(item)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.w(e) { "Deleting removed file ${item.storagePath} of report $reportId failed" }
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
                        media = report.media.toReportMedia(),
                        isPreparingMedia = false,
                        mediaErrorMessage = null
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

    private fun fieldsDifferFrom(report: Report, media: List<ReportMedia>): Boolean =
        reportFormFields.title != report.title ||
            reportFormFields.description != report.description ||
            media != report.media.toReportMedia()

    private fun clearErrors() {
        updateEditing { it.copy(titleFieldErrorMessage = null, saveErrorMessage = null, mediaErrorMessage = null) }
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