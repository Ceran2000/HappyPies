package pl.ceranka.happypies.ui.screen.add_edit_report

sealed interface SaveStatus {
    data object Idle : SaveStatus
    data class Uploading(val percent: Int) : SaveStatus
    data object Saving : SaveStatus
    data object Saved : SaveStatus
}