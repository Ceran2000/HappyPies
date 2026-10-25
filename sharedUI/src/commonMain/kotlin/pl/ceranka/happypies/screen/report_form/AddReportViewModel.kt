package pl.ceranka.happypies.screen.report_form

import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.report.NewReport
import pl.ceranka.happypies.data.report.ReportRepository

class AddReportViewModel(
    private val reportRepository: ReportRepository,
    accountRepository: AccountRepository,
    private val clientId: String
) : ReportFormViewModel(accountRepository) {

    val uiState: StateFlow<ReportFormUiState> = formState.asStateFlow()

    override val reportForLog: String get() = "a new report for client $clientId"

    override suspend fun save(title: String, description: String?, viewer: Account) {
        reportRepository.createReport(NewReport(clientId, title, description, authorId = viewer.uid, authorRole = viewer.role))
    }
}