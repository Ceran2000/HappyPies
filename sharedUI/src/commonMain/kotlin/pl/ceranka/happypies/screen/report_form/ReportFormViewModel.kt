package pl.ceranka.happypies.screen.report_form

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.accountOrNull
import pl.ceranka.happypies.data.report.NewReport
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.ui.common.toUserMessage

class ReportFormViewModel(
    private val reportRepository: ReportRepository,
    private val accountRepository: AccountRepository,
    private val clientId: String,
) : ViewModel() {

    private val log = Logger.withTag("ReportForm")

    val titleState = TextFieldState()
    val descriptionState = TextFieldState()

    private val _uiState = MutableStateFlow<ReportFormUiState>(ReportFormUiState.Editing())
    val uiState: StateFlow<ReportFormUiState> = _uiState.asStateFlow()

    fun onSave() {
        // Ignore taps while saving or after saving (no double submit).
        if (_uiState.value !is ReportFormUiState.Editing) return
        val title = titleState.text.toString().trim()
        if (title.isEmpty()) {
            _uiState.value = ReportFormUiState.Editing(titleError = "Podaj tytuł raportu")
            return
        }
        val author = accountRepository.state.value.accountOrNull
        if (author == null) {
            _uiState.value = ReportFormUiState.Editing(saveError = "Sesja wygasła. Zaloguj się ponownie.")
            return
        }
        val description = descriptionState.text.toString().trim().ifEmpty { null }

        _uiState.value = ReportFormUiState.Saving
        viewModelScope.launch {
            _uiState.value = try {
                reportRepository.createReport(
                    NewReport(
                        clientId = clientId,
                        title = title,
                        description = description,
                        authorId = author.uid,
                        authorRole = author.role,
                    )
                )
                ReportFormUiState.Saved
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Creating a report for client $clientId failed" }
                ReportFormUiState.Editing(saveError = e.toUserMessage())
            }
        }
    }
}