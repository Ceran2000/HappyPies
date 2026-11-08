package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.foundation.text.input.TextFieldState
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
import pl.ceranka.happypies.data.report.NewReport
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.ui.common.toUserMessage

class AddReportViewModel(
    private val reportRepository: ReportRepository,
    private val accountRepository: AccountRepository,
    private val clientId: String
) : ViewModel() {

    private val log = Logger.withTag("AddReport")

    private val reportFormFields = ReportFormFields()
    val titleState: TextFieldState get() = reportFormFields.titleState
    val descriptionState: TextFieldState get() = reportFormFields.descriptionState

    private val _uiState = MutableStateFlow(AddReportUiState.INITIAL)
    val uiState: StateFlow<AddReportUiState> = _uiState.asStateFlow()

    fun onSaveClick() {
        clearErrors()
        if (reportFormFields.title.isEmpty()) return showTitleRequiredError()
        val viewer = accountRepository.state.value.accountOrNull ?: return showSessionExpiredError()
        createReport(viewer)
    }

    private fun showTitleRequiredError() {
        _uiState.update { it.copy(titleFieldErrorMessage = TITLE_REQUIRED_MESSAGE) }
    }

    private fun showSessionExpiredError() {
        _uiState.update { it.copy(saveErrorMessage = SESSION_EXPIRED_MESSAGE) }
    }

    private fun clearErrors() {
        _uiState.update { it.copy(titleFieldErrorMessage = null, saveErrorMessage = null) }
    }

    private fun createReport(viewer: Account) {
        _uiState.update { it.copy(saveStatus = SaveStatus.SAVING) }
        viewModelScope.launch {
            try {
                reportRepository.createReport(
                    NewReport(clientId, reportFormFields.title, reportFormFields.description, authorId = viewer.uid, authorRole = viewer.role)
                )
                _uiState.update { it.copy(saveStatus = SaveStatus.SAVED) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Saving a new report for client $clientId failed" }
                _uiState.update { it.copy(saveStatus = SaveStatus.IDLE, saveErrorMessage = e.toUserMessage()) }
            }
        }
    }
}