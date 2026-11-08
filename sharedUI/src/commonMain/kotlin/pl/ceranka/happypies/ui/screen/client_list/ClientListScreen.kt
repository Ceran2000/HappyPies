package pl.ceranka.happypies.ui.screen.client_list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.ui.common.CenteredProgress
import pl.ceranka.happypies.ui.common.HappyPiesAppBar
import pl.ceranka.happypies.ui.common.InitialsAvatar

@Composable
fun ClientListScreen(
    onClientClick: (String) -> Unit,
    onLogout: () -> Unit
) {
    val viewModel = koinViewModel<ClientListViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ClientListContent(
        uiState = uiState,
        onClientClick = onClientClick,
        onLogout = onLogout
    )
}

@Composable
private fun ClientListContent(
    uiState: ClientListUiState,
    onClientClick: (String) -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            HappyPiesAppBar(
                title = {},
                navigationIcon = null,
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = "Wyloguj się")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (uiState) {
                is ClientListUiState.Loading -> item { CenteredProgress() }
                is ClientListUiState.Error -> item {
                    Text(
                        text = uiState.message ?: "Nie udało się wczytać listy klientów",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                is ClientListUiState.Success -> {
                    uiState.greeting?.let { greeting -> item(key = "greeting", contentType = "greeting") { Greeting(greeting) } }
                    item(key = "clientsTitle", contentType = "clientsTitle") { ClientsTitle() }
                    if (uiState.clients.isEmpty()) {
                        item { EmptyClients() }
                    }
                    items(uiState.clients, key = { it.id }, contentType = { "client" }) { client ->
                        ClientCard(client = client, onClick = { onClientClick(client.id) })
                    }
                }
            }
        }
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
private fun Greeting(greeting: String) {
    Text(text = greeting, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 8.dp))
}

@Composable
private fun ClientsTitle() {
    Text(
        text = "Klienci",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)
    )
}

@Composable
private fun ClientCard(client: Client, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            InitialsAvatar(name = client.name, colorKey = client.id)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = client.name, style = MaterialTheme.typography.titleMedium)
                if (client.dogs.isNotEmpty()) {
                    Text(
                        text = client.dogs.joinToString(" · ") { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyClients() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Nie masz jeszcze żadnych klientów",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp)
        )
    }
}