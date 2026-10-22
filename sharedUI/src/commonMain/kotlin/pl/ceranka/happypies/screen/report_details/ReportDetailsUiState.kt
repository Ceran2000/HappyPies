package pl.ceranka.happypies.screen.report_details

data class ReportDetailsUiState(
    val report: ReportDetailsItem? = null,
    /** The report no longer exists, e.g. deleted on another device. */
    val isDeleted: Boolean = false,
    val isDeleting: Boolean = false,
    /** This screen deleted the report; the screen closes. */
    val isDeleteFinished: Boolean = false,
    val deleteError: String? = null,
    val error: String? = null,
)

data class ReportDetailsItem(
    val title: String,
    val description: String?,
    val addedBy: String,
    /** Only when the report was edited after it was added. */
    val editedBy: String?,
    val comment: CommentItem?,
)

data class CommentItem(
    val label: String,
    val text: String,
    val date: String,
)