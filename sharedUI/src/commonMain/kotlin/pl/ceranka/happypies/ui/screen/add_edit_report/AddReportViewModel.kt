package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.foundation.text.input.TextFieldState
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
import pl.ceranka.happypies.data.report.NewReport
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.ui.common.toUserMessage
import pl.ceranka.happypies.ui.media.PickedFile

class AddReportViewModel(
    private val reportRepository: ReportRepository,
    mediaRepository: MediaRepository,
    private val accountRepository: AccountRepository,
    private val mediaPreparer: ReportMediaPreparer,
    private val clientId: String
) : ViewModel() {

    private val log = Logger.withTag("AddReport")
    private val mediaUploader by lazy { ReportMediaUploader(mediaRepository) }

    private val reportFormFields = ReportFormFields()
    val titleState: TextFieldState get() = reportFormFields.titleState
    val descriptionState: TextFieldState get() = reportFormFields.descriptionState

    private val _uiState = MutableStateFlow(AddReportUiState.INITIAL)
    val uiState: StateFlow<AddReportUiState> = _uiState.asStateFlow()

    fun onMediaPicked(files: List<PickedFile>) {
        val newFiles = files.distinctFrom(uiState.value.media)
        val compressing = newFiles.mapNotNull { ReportMedia.Compressing.fromPickedFileOrNull(it) }
        val rejected = List(newFiles.size - compressing.size) { PreparedMedia.Unreadable }

        _uiState.update { it.copy(media = it.media + compressing, mediaErrorMessage = null) }
        viewModelScope.launch {
            val prepared = mediaPreparer.prepare(
                media = compressing,
                onProgress = { file, percent -> _uiState.update { it.copy(media = it.media.withCompressionProgress(file.sourceUri, percent)) } },
                onPrepared = { file, result -> _uiState.update { it.copy(media = it.media.withPreparationResult(file.sourceUri, result)) } }
            )
            _uiState.update { it.copy(mediaErrorMessage = pickErrorMessage(rejected + prepared)) }
        }
    }

    fun onRemoveMediaClick(media: ReportMedia) {
        _uiState.update { it.copy(media = it.media - media, mediaErrorMessage = null) }
    }

    fun onSaveClick() {
        clearErrors()
        if (reportFormFields.title.isEmpty()) return showTitleRequiredError()
        val viewer = accountRepository.state.value.accountOrNull ?: return showSessionExpiredError()
        createReport(viewer)
    }

    private fun clearErrors() {
        _uiState.update { it.copy(titleFieldErrorMessage = null, saveErrorMessage = null, mediaErrorMessage = null) }
    }

    private fun showTitleRequiredError() {
        _uiState.update { it.copy(titleFieldErrorMessage = Res.string.report_form_title_required) }
    }

    private fun showSessionExpiredError() {
        _uiState.update { it.copy(saveErrorMessage = Res.string.report_form_session_expired) }
    }

    private fun createReport(viewer: Account) {
        _uiState.update { it.copy(saveStatus = it.media.saveStartStatus()) }

        viewModelScope.launch {
            try {
                uploadPickedMedia()
                _uiState.update { it.copy(saveStatus = SaveStatus.Saving) }
                reportRepository.createReport(
                    NewReport(
                        clientId,
                        reportFormFields.title,
                        reportFormFields.description,
                        authorId = viewer.uid,
                        authorRole = viewer.role,
                        media = uiState.value.media.uploadedItems()
                    )
                )
                _uiState.update { it.copy(saveStatus = SaveStatus.Saved) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Saving a new report for client $clientId failed" }
                _uiState.update { it.copy(saveStatus = SaveStatus.Idle, saveErrorMessage = e.toUserMessage()) }
            }
        }
    }

    private suspend fun uploadPickedMedia() {
        mediaUploader.uploadCompressed(
            clientId = clientId,
            media = uiState.value.media,
            onProgress = { totalPercent ->
                _uiState.update {
                    it.copy(saveStatus = SaveStatus.Uploading(totalPercent))
                }
            },
            onUploaded = { file, item ->
                _uiState.update {
                    it.copy(media = it.media.withUploaded(file.sourceUri, item))
                }
            }
        )
    }
}