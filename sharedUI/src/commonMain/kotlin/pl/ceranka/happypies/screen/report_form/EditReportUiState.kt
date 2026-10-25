package pl.ceranka.happypies.screen.report_form

data class EditReportUiState(
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val form: ReportFormUiState = ReportFormUiState()
) {
    val showLoadingProgress: Boolean get() = isLoading
    val showForm: Boolean get() = !isLoading && loadError == null
}