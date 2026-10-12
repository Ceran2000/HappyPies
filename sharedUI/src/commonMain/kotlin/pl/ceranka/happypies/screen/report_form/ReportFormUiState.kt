package pl.ceranka.happypies.screen.report_form

sealed interface ReportFormUiState {
    data class Editing(val titleError: String? = null, val saveError: String? = null) : ReportFormUiState
    data object Saving : ReportFormUiState
    data object Saved : ReportFormUiState
}