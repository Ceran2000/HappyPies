package pl.ceranka.happypies.ui.screen.add_edit_report

import org.jetbrains.compose.resources.StringResource

data class AddReportUiState(
    val saveStatus: SaveStatus,
    val titleFieldErrorMessage: StringResource?,
    val saveErrorMessage: StringResource?,
    val videos: List<ReportVideo>,
    val videoErrorMessage: StringResource?
) {
    val fieldsEnabled: Boolean get() = saveStatus == SaveStatus.Idle
    val saveButtonEnabled: Boolean get() = saveStatus == SaveStatus.Idle
    val cancelButtonEnabled: Boolean get() = saveStatus == SaveStatus.Idle || saveStatus == SaveStatus.Saved
    val isSaved: Boolean get() = saveStatus == SaveStatus.Saved
    val addVideoButtonEnabled: Boolean get() = fieldsEnabled && videos.size < MAX_REPORT_VIDEOS
    val videoPickerMaxItems: Int get() = (MAX_REPORT_VIDEOS - videos.size).coerceAtLeast(1)

    companion object {
        val INITIAL = AddReportUiState(
            saveStatus = SaveStatus.Idle,
            titleFieldErrorMessage = null,
            saveErrorMessage = null,
            videos = emptyList(),
            videoErrorMessage = null
        )
    }
}