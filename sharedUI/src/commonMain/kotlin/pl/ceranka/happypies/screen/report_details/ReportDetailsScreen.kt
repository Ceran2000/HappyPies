package pl.ceranka.happypies.screen.report_details

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ReportDetailsScreen(
    clientId: String,
    reportId: String,
    onBack: () -> Unit,
    onDeleted: () -> Unit,
) {
    val viewModel = koinViewModel<ReportDetailsViewModel> { parametersOf(clientId, reportId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isDeleteFinished) {
        if (uiState.isDeleteFinished) onDeleted()
    }

    ReportDetailsContent(
        uiState = uiState,
        onBack = onBack,
        onDelete = viewModel::onDelete,
        onRetry = viewModel::onRetry,
        commentText = viewModel.commentText,
        commentActions = CommentActions(
            onAdd = viewModel::onAddComment,
            onEdit = viewModel::onEditComment,
            onSave = viewModel::onSaveComment,
            onCancel = viewModel::onCancelComment,
            onDelete = viewModel::onDeleteComment,
        ),
    )
}

private class CommentActions(
    val onAdd: () -> Unit,
    val onEdit: () -> Unit,
    val onSave: () -> Unit,
    val onCancel: () -> Unit,
    val onDelete: () -> Unit,
)

@Composable
private fun ReportDetailsContent(
    uiState: ReportDetailsUiState,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
    commentText: TextFieldState,
    commentActions: CommentActions,
) {
    var showDeleteReportDialog by rememberSaveable { mutableStateOf(false) }
    val report = uiState.report

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack) {
                Text("← Wstecz")
            }
            if (uiState.showDeleteReportButton) {
                TextButton(onClick = { showDeleteReportDialog = true }, enabled = uiState.deleteReportButtonEnabled) {
                    Text("Usuń")
                }
            }
        }

        when {
            uiState.isDeleted -> ReportDeletedNotice(onBack)
            report != null -> LoadedReport(report, uiState, commentText, commentActions, onRetry)
            uiState.error != null -> ErrorWithRetry(uiState.error, onRetry)
            else -> CircularProgressIndicator()
        }
    }

    if (showDeleteReportDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteReportDialog = false },
            title = { Text("Usunąć raport?") },
            text = { Text("Raport zniknie u trenera i u klienta. Tego nie można cofnąć.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteReportDialog = false
                    onDelete()
                }) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteReportDialog = false }) {
                    Text("Anuluj")
                }
            },
        )
    }
}

@Composable
private fun ReportDeletedNotice(onBack: () -> Unit) {
    Text("Ten raport został usunięty.")
    Button(onClick = onBack) {
        Text("Wróć")
    }
}

@Composable
private fun LoadedReport(
    report: ReportDetailsItem,
    uiState: ReportDetailsUiState,
    commentText: TextFieldState,
    commentActions: CommentActions,
    onRetry: () -> Unit,
) {
    ReportBody(report)
    CommentSection(report, uiState.commentEditor, commentText, commentActions)
    if (uiState.showReportDeletionProgress) ReportDeletionProgress()
    uiState.deleteError?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
    uiState.error?.let { ErrorWithRetry(it, onRetry) }
}

@Composable
private fun ReportBody(report: ReportDetailsItem) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = report.title, style = MaterialTheme.typography.headlineSmall)
        Text(text = "Dodany przez: ${report.addedBy}", style = MaterialTheme.typography.bodySmall)
        report.editedBy?.let { Text(text = "Edytowany przez: $it", style = MaterialTheme.typography.bodySmall) }
    }
    report.description?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
}

@Composable
private fun CommentSection(report: ReportDetailsItem, editor: CommentEditor, commentText: TextFieldState, actions: CommentActions) {
    val comment = report.comment
    when {
        editor.isOpen -> CommentEditorCard(editor, report.showDeleteCommentButton, commentText, actions)
        comment != null -> CommentCard(comment, onEdit = actions.onEdit)
        report.showAddCommentButton -> AddCommentCard(onAdd = actions.onAdd)
    }
}

@Composable
private fun CommentEditorCard(editor: CommentEditor, showDeleteButton: Boolean, commentText: TextFieldState, actions: CommentActions) {
    var showDeleteCommentDialog by rememberSaveable { mutableStateOf(false) }

    OutlinedCard(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                state = commentText,
                modifier = Modifier.fillMaxWidth(),
                enabled = editor.textFieldEnabled,
                label = { Text("Komentarz") },
                isError = editor.error != null,
                supportingText = editor.error?.let { { Text(it) } },
                lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 3),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // A trash icon once theme A brings an icon set.
                if (showDeleteButton) {
                    TextButton(onClick = { showDeleteCommentDialog = true }, enabled = editor.deleteButtonEnabled) {
                        Text("Usuń", color = MaterialTheme.colorScheme.error)
                    }
                }
                Spacer(Modifier.weight(1f))
                if (editor.showSavingProgress) {
                    CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                }
                TextButton(onClick = actions.onCancel, enabled = editor.cancelButtonEnabled) {
                    Text("Anuluj")
                }
                Button(onClick = actions.onSave, enabled = editor.saveButtonEnabled) {
                    Text("Zapisz")
                }
            }
        }
    }

    if (showDeleteCommentDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteCommentDialog = false },
            title = { Text("Usunąć komentarz?") },
            text = { Text("Komentarz zniknie u trenera i u klienta. Tego nie można cofnąć.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteCommentDialog = false
                    actions.onDelete()
                }) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteCommentDialog = false }) {
                    Text("Anuluj")
                }
            },
        )
    }
}

@Composable
private fun CommentCard(comment: CommentItem, onEdit: () -> Unit) {
    OutlinedCard(modifier = Modifier.fillMaxWidth(), border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = comment.label.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                // A pencil icon once theme A brings an icon set.
                if (comment.showEditButton) {
                    TextButton(onClick = onEdit) {
                        Text("Edytuj")
                    }
                }
            }
            Text(text = comment.text, style = MaterialTheme.typography.bodyLarge)
            Text(text = comment.date, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun AddCommentCard(onAdd: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Button(onClick = onAdd) {
                Text("Dodaj komentarz")
            }
        }
    }
}

@Composable
private fun ReportDeletionProgress() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        Text("Usuwanie…")
    }
}

@Composable
private fun ErrorWithRetry(message: String, onRetry: () -> Unit) {
    Column {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) {
            Text("Spróbuj ponownie")
        }
    }
}