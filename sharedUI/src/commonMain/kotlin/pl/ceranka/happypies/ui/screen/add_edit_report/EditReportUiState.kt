package pl.ceranka.happypies.ui.screen.add_edit_report

import pl.ceranka.happypies.data.report.Report

sealed interface EditReportUiState {

    data object Loading : EditReportUiState

    data class LoadError(val message: String) : EditReportUiState

    data class Editing(
        val report: Report,
        val saveStatus: SaveStatus,
        val titleFieldErrorMessage: String?,
        val saveErrorMessage: String?
    ) : EditReportUiState {
        val fieldsEnabled: Boolean get() = saveStatus == SaveStatus.IDLE
        val showSavingProgress: Boolean get() = saveStatus == SaveStatus.SAVING
        val cancelButtonEnabled: Boolean get() = saveStatus != SaveStatus.SAVING
        val saveButtonEnabled: Boolean get() = saveStatus == SaveStatus.IDLE
        val isSaved: Boolean get() = saveStatus == SaveStatus.SAVED
    }
}