package pl.ceranka.happypies.ui.screen.add_edit_report

sealed interface MediaUploadStatus {
    data object NotStarted : MediaUploadStatus
    data object Waiting : MediaUploadStatus
    data class InProgress(val percent: Int) : MediaUploadStatus
}