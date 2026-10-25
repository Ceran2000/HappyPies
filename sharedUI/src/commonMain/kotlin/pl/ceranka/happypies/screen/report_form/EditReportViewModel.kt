package pl.ceranka.happypies.screen.report_form

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.report.Report
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.ui.common.toUserMessage

class EditReportViewModel(
    private val reportRepository: ReportRepository,
    accountRepository: AccountRepository,
    private val reportId: String
) : ReportFormViewModel(accountRepository) {

    private val loadState = MutableStateFlow(LoadState())

    /** The report as loaded; saving the same values writes nothing. */
    private var loadedReport: Report? = null

    val uiState: StateFlow<EditReportUiState> = combine(loadState, formState) { load, form ->
        EditReportUiState(isLoading = load.inProgress, loadError = load.error, form = form)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), EditReportUiState(isLoading = true))

    override val reportForLog: String get() = "report $reportId"

    fun onRetry() {
        if (loadState.value.inProgress) return

        loadReport()
    }

    private fun loadReport() {
        loadState.update { LoadState(inProgress = true) }

        viewModelScope.launch {
            try {
                val report = reportRepository.getReport(reportId)
                loadedReport = report
                titleState.setTextAndPlaceCursorAtEnd(report.title)
                descriptionState.setTextAndPlaceCursorAtEnd(report.description.orEmpty())
                loadState.update { LoadState() }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Loading report $reportId for editing failed" }
                loadState.update { LoadState(error = e.toUserMessage()) }
            }
        }
    }

    override fun canSave(): Boolean = loadedReport != null

    // Saving without changes would only mark the report as edited.
    override fun hasChanges(title: String, description: String?): Boolean =
        title != loadedReport?.title || description != loadedReport?.description

    override suspend fun save(title: String, description: String?, viewer: Account) {
        reportRepository.updateReport(reportId, title, description, editorId = viewer.uid)
    }

    init {
        loadReport()
    }
}

private data class LoadState(val inProgress: Boolean = false, val error: String? = null)