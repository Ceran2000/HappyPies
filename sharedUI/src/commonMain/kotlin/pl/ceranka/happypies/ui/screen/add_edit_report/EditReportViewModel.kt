package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.accountOrNull
import pl.ceranka.happypies.data.report.Report
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.ui.common.toUserMessage

class EditReportViewModel(
    private val reportRepository: ReportRepository,
    private val accountRepository: AccountRepository,
    private val reportId: String
) : ViewModel() {

    private val log = Logger.withTag("EditReport")

    private val reportFormFields = ReportFormFields()
    val titleState: TextFieldState get() = reportFormFields.titleState
    val descriptionState: TextFieldState get() = reportFormFields.descriptionState

    private val _uiState = MutableStateFlow<EditReportUiState>(EditReportUiState.Loading)
    val uiState: StateFlow<EditReportUiState> = _uiState.asStateFlow()

    fun onSaveClick() {
        val report = uiState.value.reportOrNull ?: return
        clearErrors()
        if (reportFormFields.title.isEmpty()) return showTitleRequiredError()
        val viewer = accountRepository.state.value.accountOrNull ?: return showSessionExpiredError()

        if (!fieldsDifferFrom(report)) {
            return updateEditing { it.copy(saveStatus = SaveStatus.SAVED) }
        }
        updateReport(viewer)
    }

    private fun updateReport(viewer: Account) {
        updateEditing { it.copy(saveStatus = SaveStatus.SAVING) }
        viewModelScope.launch {
            try {
                reportRepository.updateReport(reportId, reportFormFields.title, reportFormFields.description, editorId = viewer.uid)
                updateEditing { it.copy(saveStatus = SaveStatus.SAVED) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Saving report $reportId failed" }
                updateEditing { it.copy(saveStatus = SaveStatus.IDLE, saveErrorMessage = e.toUserMessage()) }
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
                    EditReportUiState.Editing(report = report, saveStatus = SaveStatus.IDLE, titleFieldErrorMessage = null, saveErrorMessage = null)
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

    private fun fieldsDifferFrom(report: Report): Boolean =
        reportFormFields.title != report.title || reportFormFields.description != report.description

    private fun clearErrors() {
        updateEditing { it.copy(titleFieldErrorMessage = null, saveErrorMessage = null) }
    }

    private fun showTitleRequiredError() {
        updateEditing { it.copy(titleFieldErrorMessage = TITLE_REQUIRED_MESSAGE) }
    }

    private fun showSessionExpiredError() {
        updateEditing { it.copy(saveErrorMessage = SESSION_EXPIRED_MESSAGE) }
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

private val EditReportUiState.reportOrNull: Report?
    get() = when (this) {
        is EditReportUiState.Editing -> report
        EditReportUiState.Loading, is EditReportUiState.LoadError -> null
    }