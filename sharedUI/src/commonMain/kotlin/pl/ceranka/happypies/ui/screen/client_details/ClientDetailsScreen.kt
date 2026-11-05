package pl.ceranka.happypies.ui.screen.client_details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.ui.common.ErrorWithRetry
import pl.ceranka.happypies.ui.common.ReportCard
import pl.ceranka.happypies.ui.common.ReportListItem
import pl.ceranka.happypies.ui.paging.PagingLazyColumn
import pl.ceranka.happypies.ui.paging.pagingItems

@Composable
fun ClientDetailsScreen(
    clientId: String,
    onBack: () -> Unit,
    onAddReport: () -> Unit,
    onReportClick: (reportId: String) -> Unit
) {
    val viewModel = koinViewModel<ClientDetailsViewModel> { parametersOf(clientId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reports = viewModel.reports.collectAsLazyPagingItems()

    ClientDetailsContent(
        uiState = uiState,
        reports = reports,
        onBack = onBack,
        onAddReport = onAddReport,
        onReportClick = onReportClick,
        onRetry = viewModel::onRetry
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ClientDetailsContent(
    uiState: ClientDetailsUiState,
    reports: LazyPagingItems<ReportListItem>,
    onBack: () -> Unit,
    onAddReport: () -> Unit,
    onReportClick: (reportId: String) -> Unit,
    onRetry: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddReport,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("Nowy raport") }
            )
        }
    ) { padding ->
        PagingLazyColumn(
            items = reports,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (uiState) {
                is ClientDetailsUiState.ShowContent -> clientWithReports(uiState.client, reports, onReportClick)
                ClientDetailsUiState.Loading -> loading()
                is ClientDetailsUiState.Error -> error(uiState.message, onRetry)
            }
        }
    }
}

private fun LazyListScope.clientWithReports(
    client: Client,
    reports: LazyPagingItems<ReportListItem>,
    onReportClick: (reportId: String) -> Unit
) {
    item(key = "clientDetails", contentType = "clientDetails") { ClientDetailsFields(client) }
    item(key = "reportsTitle", contentType = "reportsTitle") { ReportsTitle() }
    reportItems(reports, onReportClick, emptyContent = { Text("Brak raportów. Dodaj pierwszy przyciskiem „Nowy raport”.") })
}

private fun LazyListScope.reportItems(
    reports: LazyPagingItems<ReportListItem>,
    onReportClick: (reportId: String) -> Unit,
    emptyContent: @Composable () -> Unit
) {
    pagingItems(
        items = reports,
        key = { it.id },
        contentType = "report",
        emptyContent = emptyContent
    ) { report ->
        ReportCard(report, onClick = { onReportClick(report.id) })
    }
}

private fun LazyListScope.loading() {
    item(key = "progress", contentType = "progress") {
        CircularProgressIndicator()
    }
}

private fun LazyListScope.error(message: String, onRetry: () -> Unit) {
    item(key = "error", contentType = "error") {
        ErrorWithRetry(message, onRetry)
    }
}

@Composable
private fun ReportsTitle() {
    Text(
        text = "Raporty",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(top = 16.dp)
    )
}

@Composable
private fun ClientDetailsFields(client: Client) {
    Column {
        Text(text = client.name, style = MaterialTheme.typography.headlineSmall)
        Text(text = client.email, style = MaterialTheme.typography.bodyMedium)

        Text(
            text = "Psy",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
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