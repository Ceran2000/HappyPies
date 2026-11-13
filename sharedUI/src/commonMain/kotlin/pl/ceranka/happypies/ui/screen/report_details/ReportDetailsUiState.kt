package pl.ceranka.happypies.ui.screen.report_details

import org.jetbrains.compose.resources.StringResource

data class ReportDetailsUiState(
    val report: ReportDetailsItem? = null,
    /** The report no longer exists, e.g. deleted on another device. */
    val isDeleted: Boolean = false,
    val isDeleting: Boolean = false,
    /** This screen deleted the report; the screen closes. */
    val isDeleteFinished: Boolean = false,
    val deleteError: StringResource? = null,
    val error: StringResource? = null,
    val commentEditor: CommentEditor = CommentEditor()
) {
    val showDeleteReportButton: Boolean get() = report != null && !isDeleted
    val deleteReportButtonEnabled: Boolean get() = !isDeleting
    val showReportDeletionProgress: Boolean get() = isDeleting
    val editReportButtonEnabled: Boolean get() = !isDeleting
}

data class ReportDetailsItem(
    val title: String,
    val description: String?,
    val videos: List<ReportDetailsVideo>,
    val addedBy: ChangeStamp,
    val editedBy: ChangeStamp?,
    val comment: CommentItem?,
    val viewerCanAddComment: Boolean
) {
    val showAddCommentButton: Boolean get() = viewerCanAddComment
    val showDeleteCommentButton: Boolean get() = comment != null
}

data class ReportDetailsVideo(val downloadUrl: String, val sizeBytes: Long)

data class ChangeStamp(val personName: String, val date: String)

data class CommentItem(
    val authorName: String,
    val text: String,
    val date: String,
    val viewerCanEdit: Boolean
) {
    val showEditButton: Boolean get() = viewerCanEdit
}

/** The comment card in edit mode. The text itself lives in the ViewModel's TextFieldState. */
data class CommentEditor(
    val isOpen: Boolean = false,
    val isSaving: Boolean = false,
    /** Validation or write error shown in the card. */
    val error: StringResource? = null
) {
    val textFieldEnabled: Boolean get() = !isSaving
    val deleteButtonEnabled: Boolean get() = !isSaving
    val showSavingProgress: Boolean get() = isSaving
    val cancelButtonEnabled: Boolean get() = !isSaving
    val saveButtonEnabled: Boolean get() = !isSaving
}