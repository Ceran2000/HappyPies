package pl.ceranka.happypies.screen.client_details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pl.ceranka.happypies.data.client.Client

@Composable
fun ClientDetailsScreen(
    clientId: String,
    onBack: () -> Unit,
    onAddReport: () -> Unit,
) {
    val viewModel = koinViewModel<ClientDetailsViewModel> { parametersOf(clientId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ClientDetailsContent(
        uiState = uiState,
        onBack = onBack,
        onAddReport = onAddReport,
        onRetry = viewModel::onRetry,
    )
}

@Composable
private fun ClientDetailsContent(
    uiState: ClientDetailsUiState,
    onBack: () -> Unit,
    onAddReport: () -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        TextButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
            Text("← Wstecz")
        }
        val client = uiState.client
        when {
            client != null -> ClientWithReports(client, uiState, onAddReport, onRetry)
            uiState.error != null -> ErrorWithRetry(uiState.error, onRetry, Modifier.padding(16.dp))
            else -> CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
private fun ClientWithReports(
    client: Client,
    uiState: ClientDetailsUiState,
    onAddReport: () -> Unit,
    onRetry: () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // Bottom padding keeps the last card above the "Nowy raport" button.
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { ClientDetailsFields(client) }
            item {
                Text(
                    text = "Raporty",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            uiState.error?.let { message ->
                item { ErrorWithRetry(message, onRetry) }
            }
            if (uiState.reports.isEmpty()) {
                item { Text("Brak raportów. Dodaj pierwszy przyciskiem „Nowy raport”.") }
            }
            items(uiState.reports, key = { it.id }) { report -> ReportCard(report) }
        }
        ExtendedFloatingActionButton(
            onClick = onAddReport,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) {
            Text("+ Nowy raport")
        }
    }
}

@Composable
private fun ClientDetailsFields(client: Client) {
    Column {
        Text(text = client.name, style = MaterialTheme.typography.headlineSmall)
        Text(text = client.email, style = MaterialTheme.typography.bodyMedium)

        Text(
            text = "Psy",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
        )
        if (client.dogs.isEmpty()) {
            Text("Brak psów")
        } else {
            client.dogs.forEach { dog ->
                Text("${dog.name} (${dog.breed})", modifier = Modifier.padding(vertical = 4.dp))
            }
        }
    }
}

@Composable
private fun ReportCard(report: ReportListItem) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = report.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${report.createdAt} · Dodany przez: ${report.authorName}",
                style = MaterialTheme.typography.bodySmall,
            )
            val details = buildList {
                if (report.mediaCount > 0) add("Media: ${report.mediaCount}")
                add(if (report.hasComment) "Jest komentarz" else "Bez komentarza")
            }
            Text(text = details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ErrorWithRetry(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) {
            Text("Spróbuj ponownie")
        }
    }
}