package pl.ceranka.happypies.screen.report_form

data class ReportFormUiState(
    val isSaving: Boolean = false,
    /** The report is saved (or nothing changed); the form closes. */
    val isSaved: Boolean = false,
    val titleError: String? = null,
    val saveError: String? = null
) {
    val fieldsEnabled: Boolean get() = !isSaving
    val cancelButtonEnabled: Boolean get() = !isSaving
    val saveButtonEnabled: Boolean get() = !isSaving
    val showSavingProgress: Boolean get() = isSaving
    val saveButtonText: String get() = if (isSaving) "Zapisywanie…" else "Zapisz raport"
}