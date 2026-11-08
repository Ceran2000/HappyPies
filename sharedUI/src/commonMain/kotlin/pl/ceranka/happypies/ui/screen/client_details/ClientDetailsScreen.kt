package pl.ceranka.happypies.ui.screen.client_details

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.client.Dog
import pl.ceranka.happypies.ui.common.EmptyReportsCard
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
    val showOfflineMessage by viewModel.showOfflineMessage.collectAsStateWithLifecycle()
    val reports = viewModel.reports.collectAsLazyPagingItems()

    ClientDetailsContent(
        uiState = uiState,
        reports = reports,
        showOfflineMessage = showOfflineMessage,
        onOfflineMessageShown = viewModel::onOfflineMessageShown,
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
    showOfflineMessage: Boolean,
    onOfflineMessageShown: () -> Unit,
    onBack: () -> Unit,
    onAddReport: () -> Unit,
    onReportClick: (reportId: String) -> Unit,
    onRetry: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    uiState.clientNameOrNull?.let {
                        Text(text = it, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(start = 8.dp))
                    }
                },
                navigationIcon = {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface,
                        shadowElevation = 2.dp,
                        modifier = Modifier.padding(horizontal = 4.dp).size(40.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Wstecz")
                        }
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
            snackbarHostState = snackbarHostState,
            showOfflineMessage = showOfflineMessage,
            onOfflineMessageShown = onOfflineMessageShown,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
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
    item(key = "dogs", contentType = "dogs") { DogsCard(client.dogs) }
    item(key = "reportsTitle", contentType = "reportsTitle") { ReportsTitle() }
    reportItems(reports, onReportClick, emptyContent = {
        EmptyReportsCard(title = "Brak raportów", message = "Dodaj pierwszy raport przyciskiem „Nowy raport”.")
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
private fun ReportsTitle() {
    Text(
        text = "Raporty",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun DogsCard(dogs: List<Dog>) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "PSY",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.ExtraBold),
                color = MaterialTheme.colorScheme.secondary,
                letterSpacing = 0.5.sp
            )
            if (dogs.isEmpty()) {
                Text(text = "Brak psów", style = MaterialTheme.typography.bodyLarge)
            } else {
                dogs.forEach { dog -> DogRow(dog) }
            }
        }
    }
}

@Composable
private fun DogRow(dog: Dog) {
    val breedColor = MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(dog.name) }
            withStyle(SpanStyle(color = breedColor)) { append(" · ${dog.breed}") }
        },
        style = MaterialTheme.typography.bodyLarge
    )
}

private val ClientDetailsUiState.clientNameOrNull: String?
    get() = when (this) {
        is ClientDetailsUiState.ShowContent -> client.name
        ClientDetailsUiState.Loading, is ClientDetailsUiState.Error -> null
    }