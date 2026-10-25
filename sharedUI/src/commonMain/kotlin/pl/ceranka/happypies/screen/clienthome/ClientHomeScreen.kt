package pl.ceranka.happypies.screen.clienthome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pl.ceranka.happypies.ui.common.ErrorWithRetry
import pl.ceranka.happypies.ui.common.ReportCard

@Composable
fun ClientHomeScreen(
    clientId: String,
    onAddReport: () -> Unit,
    onReportClick: (reportId: String) -> Unit,
    onLogout: () -> Unit
) {
    val viewModel = koinViewModel<ClientHomeViewModel> { parametersOf(clientId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ClientHomeContent(
        uiState = uiState,
        onAddReport = onAddReport,
        onReportClick = onReportClick,
        onLogout = onLogout,
        onRetry = viewModel::onRetry
    )
}

@Composable
private fun ClientHomeContent(
    uiState: ClientHomeUiState,
    onAddReport: () -> Unit,
    onReportClick: (reportId: String) -> Unit,
    onLogout: () -> Unit,
    onRetry: () -> Unit
) {
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    val greeting = uiState.greeting
    val subtitle = uiState.subtitle
    val initials = uiState.initials

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (greeting != null && subtitle != null && initials != null) {
                item { Header(greeting, subtitle, initials, onLogoutClick = { showLogoutDialog = true }) }
                item { ReportsTitle(uiState.reportCount) }
            }
            if (uiState.showLoadingProgress) {
                item { CircularProgressIndicator() }
            }
            uiState.error?.let { message ->
                item { ErrorWithRetry(message, onRetry) }
            }
            if (uiState.showEmptyState) {
                item { EmptyReports() }
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
private fun ReportsTitle(reportCount: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, top = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = "Twoje raporty", style = MaterialTheme.typography.titleLarge)
        Text(
            text = reportCount,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyReports() {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.size(72.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Description,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(34.dp)
                    )
                }
            }
            Text(text = "Nie masz jeszcze raportów", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
            Text(
                text = "Po treningu dodaj raport z opisem, zdjęciem albo filmem. Trener go zobaczy i skomentuje.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}