package pl.ceranka.happypies.ui.screen.add_edit_report

data class AddReportUiState(
    val saveStatus: SaveStatus,
    val titleFieldErrorMessage: String?,
    val saveErrorMessage: String?
) {
    val fieldsEnabled: Boolean get() = saveStatus == SaveStatus.IDLE
    val saveButtonEnabled: Boolean get() = saveStatus == SaveStatus.IDLE
    val showSavingProgress: Boolean get() = saveStatus == SaveStatus.SAVING
    val cancelButtonEnabled: Boolean get() = saveStatus != SaveStatus.SAVING
    val isSaved: Boolean get() = saveStatus == SaveStatus.SAVED

    companion object {
        val INITIAL = AddReportUiState(saveStatus = SaveStatus.IDLE, titleFieldErrorMessage = null, saveErrorMessage = null)
    }
}