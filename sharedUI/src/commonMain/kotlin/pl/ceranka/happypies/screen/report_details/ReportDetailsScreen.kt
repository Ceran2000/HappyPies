package pl.ceranka.happypies.screen.report_details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
    )
}

@Composable
private fun ReportDetailsContent(
    uiState: ReportDetailsUiState,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onRetry: () -> Unit,
) {
    var confirmingDelete by rememberSaveable { mutableStateOf(false) }
    val report = uiState.report

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onBack) {
                Text("← Wstecz")
            }
            if (report != null && !uiState.isDeleted) {
                TextButton(onClick = { confirmingDelete = true }, enabled = !uiState.isDeleting) {
                    Text("Usuń")
                }
            }
        }

        when {
            uiState.isDeleted -> DeletedElsewhere(onBack)
            report != null -> {
                ReportBody(report)
                if (uiState.isDeleting) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        Text("Usuwanie…")
                    }
                }
                uiState.deleteError?.let { Text(text = it, color = MaterialTheme.colorScheme.error) }
                uiState.error?.let { ErrorWithRetry(it, onRetry) }
            }
            uiState.error != null -> ErrorWithRetry(uiState.error, onRetry)
            else -> CircularProgressIndicator()
        }
    }

    if (confirmingDelete) {
        AlertDialog(
            onDismissRequest = { confirmingDelete = false },
            title = { Text("Usunąć raport?") },
            text = { Text("Raport zniknie u trenera i u klienta. Tego nie można cofnąć.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmingDelete = false
                    onDelete()
                }) {
                    Text("Usuń")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDelete = false }) {
                    Text("Anuluj")
                }
            },
        )
    }
}

@Composable
private fun ReportBody(report: ReportDetailsItem) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(text = report.title, style = MaterialTheme.typography.headlineSmall)
        Text(text = "Dodany przez: ${report.addedBy}", style = MaterialTheme.typography.bodySmall)
        report.editedBy?.let { Text(text = "Edytowany przez: $it", style = MaterialTheme.typography.bodySmall) }
    }
    report.description?.let { Text(text = it, style = MaterialTheme.typography.bodyLarge) }
    report.comment?.let { CommentCard(it) }
}

@Composable
private fun CommentCard(comment: CommentItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(text = comment.label.uppercase(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            Text(text = comment.text, style = MaterialTheme.typography.bodyLarge)
            Text(text = comment.date, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DeletedElsewhere(onBack: () -> Unit) {
    Text("Ten raport został usunięty.")
    Button(onClick = onBack) {
        Text("Wróć")
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