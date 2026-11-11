package pl.ceranka.happypies.ui.screen.add_edit_report

import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.data.report.Report

sealed interface EditReportUiState {

    data object Loading : EditReportUiState

    data class LoadError(val message: StringResource) : EditReportUiState

    data class Editing(
        val report: Report,
        val saveStatus: SaveStatus,
        val titleFieldErrorMessage: StringResource?,
        val saveErrorMessage: StringResource?,
        val videos: List<ReportVideo>,
        val videoErrorMessage: StringResource?
    ) : EditReportUiState {
        val fieldsEnabled: Boolean get() = saveStatus == SaveStatus.Idle
        val cancelButtonEnabled: Boolean get() = saveStatus == SaveStatus.Idle || saveStatus == SaveStatus.Saved
        val saveButtonEnabled: Boolean get() = saveStatus == SaveStatus.Idle
        val isSaved: Boolean get() = saveStatus == SaveStatus.Saved
        val addVideoButtonEnabled: Boolean get() = fieldsEnabled && videos.size < MAX_REPORT_VIDEOS
        val videoPickerMaxItems: Int get() = (MAX_REPORT_VIDEOS - videos.size).coerceAtLeast(1)
    }
}