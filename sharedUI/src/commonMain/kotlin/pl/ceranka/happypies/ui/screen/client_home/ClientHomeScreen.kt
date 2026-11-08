package pl.ceranka.happypies.ui.screen.client_home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pl.ceranka.happypies.ui.common.EmptyReportsCard
import pl.ceranka.happypies.ui.common.ErrorWithRetry
import pl.ceranka.happypies.ui.common.ReportCard
import pl.ceranka.happypies.ui.common.ReportListItem
import pl.ceranka.happypies.ui.paging.PagingLazyColumn
import pl.ceranka.happypies.ui.paging.pagingItems

@Composable
fun ClientHomeScreen(
    clientId: String,
    onAddReport: () -> Unit,
    onReportClick: (reportId: String) -> Unit,
    onLogout: () -> Unit
) {
    val viewModel = koinViewModel<ClientHomeViewModel> { parametersOf(clientId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val showOfflineMessage by viewModel.showOfflineMessage.collectAsStateWithLifecycle()
    val reports = viewModel.reports.collectAsLazyPagingItems()

    ClientHomeContent(
        uiState = uiState,
        reports = reports,
        showOfflineMessage = showOfflineMessage,
        onOfflineMessageShown = viewModel::onOfflineMessageShown,
        onAddReport = onAddReport,
        onReportClick = onReportClick,
        onLogout = onLogout,
        onRetry = viewModel::onRetry
    )
}

@Composable
private fun ClientHomeContent(
    uiState: ClientHomeUiState,
    reports: LazyPagingItems<ReportListItem>,
    showOfflineMessage: Boolean,
    onOfflineMessageShown: () -> Unit,
    onAddReport: () -> Unit,
    onReportClick: (reportId: String) -> Unit,
    onLogout: () -> Unit,
    onRetry: () -> Unit
) {
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Wylogować się?") },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text("Wyloguj")
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
            snackbarHostState = snackbarHostState,
            showOfflineMessage = showOfflineMessage,
            onOfflineMessageShown = onOfflineMessageShown,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (uiState) {
                is ClientHomeUiState.ShowContent -> headerWithReports(
                    greeting = uiState.greeting,
                    subtitle = uiState.subtitle,
                    initials = uiState.initials,
                    reports = reports,
                    onReportClick = onReportClick,
                    onLogoutClick = { showLogoutDialog = true }
                )
                ClientHomeUiState.Loading -> loading()
                is ClientHomeUiState.Error -> error(uiState.message, onRetry)
            }
        }
    }
}

private fun LazyListScope.headerWithReports(
    greeting: String,
    subtitle: String,
    initials: String,
    reports: LazyPagingItems<ReportListItem>,
    onReportClick: (reportId: String) -> Unit,
    onLogoutClick: () -> Unit
) {
    item(key = "header", contentType = "header") { Header(greeting, subtitle, initials, onLogoutClick) }
    item(key = "reportsTitle", contentType = "reportsTitle") { ReportsTitle() }
    reportItems(reports, onReportClick, emptyContent = {
        EmptyReportsCard(
            title = "Nie masz jeszcze raportów",
            message = "Po treningu dodaj raport z opisem, zdjęciem albo filmem. Trener go zobaczy i skomentuje."
        )
    })
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
private fun Header(greeting: String, subtitle: String, initials: String, onLogoutClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = greeting, style = MaterialTheme.typography.headlineLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Surface(
            onClick = onLogoutClick,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
            modifier = Modifier.size(52.dp).clearAndSetSemantics { contentDescription = "Wyloguj się" }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = initials, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun ReportsTitle() {
    Text(
        text = "Twoje raporty",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)
    )
}