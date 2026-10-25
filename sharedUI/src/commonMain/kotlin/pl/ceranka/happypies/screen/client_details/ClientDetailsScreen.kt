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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.ui.common.ErrorWithRetry

@Composable
fun ClientDetailsScreen(
    clientId: String,
    onBack: () -> Unit,
    onAddReport: () -> Unit,
    onReportClick: (reportId: String) -> Unit,
) {
    val viewModel = koinViewModel<ClientDetailsViewModel> { parametersOf(clientId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ClientDetailsContent(
        uiState = uiState,
        onBack = onBack,
        onAddReport = onAddReport,
        onReportClick = onReportClick,
        onRetry = viewModel::onRetry,
    )
}

@Composable
private fun ClientDetailsContent(
    uiState: ClientDetailsUiState,
    onBack: () -> Unit,
    onAddReport: () -> Unit,
    onReportClick: (reportId: String) -> Unit,
    onRetry: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        IconButton(onClick = onBack, modifier = Modifier.padding(horizontal = 8.dp)) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Wstecz")
        }
        val client = uiState.client
        when {
            client != null -> ClientWithReports(client, uiState, onAddReport, onReportClick, onRetry)
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
    onReportClick: (reportId: String) -> Unit,
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
            items(uiState.reports, key = { it.id }) { report ->
                ReportCard(report, onClick = { onReportClick(report.id) })
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAddReport,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Nowy raport") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        )
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
private fun ReportCard(report: ReportListItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
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