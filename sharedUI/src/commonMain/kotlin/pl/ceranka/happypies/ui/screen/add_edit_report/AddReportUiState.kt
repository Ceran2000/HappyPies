package pl.ceranka.happypies.ui.screen.add_edit_report

import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.data.report.MediaType

data class AddReportUiState(
    val saveStatus: SaveStatus,
    val titleFieldErrorMessage: StringResource?,
    val saveErrorMessage: StringResource?,
    val media: List<ReportMedia>,
    val isPreparingMedia: Boolean,
    val mediaErrorMessage: StringResource?
) {
    val fieldsEnabled: Boolean get() = saveStatus == SaveStatus.Idle

    val saveButtonEnabled: Boolean get() = saveStatus == SaveStatus.Idle && !isPreparingMedia && !showMediaLimitError

    val showMediaLimitError: Boolean get() = media.exceedsLimits()

    val cancelButtonEnabled: Boolean get() = saveStatus == SaveStatus.Idle || saveStatus == SaveStatus.Saved

    val isSaved: Boolean get() = saveStatus == SaveStatus.Saved

    val showAddPhotosButton: Boolean get() = fieldsEnabled && media.freeSlotsOf(MediaType.PHOTO) > 0

    val showAddVideosButton: Boolean get() = fieldsEnabled && media.freeSlotsOf(MediaType.VIDEO) > 0

    val photoPickerMaxItems: Int get() = media.freeSlotsOf(MediaType.PHOTO).coerceAtLeast(1)

    val videoPickerMaxItems: Int get() = media.freeSlotsOf(MediaType.VIDEO).coerceAtLeast(1)

    val addMediaButtonEnabled: Boolean get() = !isPreparingMedia

    val showRemoveMediaButtons: Boolean get() = fieldsEnabled

    companion object {
        val INITIAL = AddReportUiState(
            saveStatus = SaveStatus.Idle,
            titleFieldErrorMessage = null,
            saveErrorMessage = null,
            media = emptyList(),
            isPreparingMedia = false,
            mediaErrorMessage = null
        )
    }
}