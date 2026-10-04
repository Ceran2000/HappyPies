package pl.ceranka.happypies.screen.client_list

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pl.ceranka.happypies.data.client.Client

@Composable
fun ClientListScreen(
    onClientClick: (String) -> Unit,
) {
    val viewModel = koinViewModel<ClientListViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ClientListContent(
        uiState = uiState,
        onClientClick = onClientClick
    )
}

@Composable
private fun ClientListContent(
    uiState: ClientListUiState,
    onClientClick: (String) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Witaj, trenerze",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        when (uiState) {
            is ClientListUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ClientListUiState.Error -> {
                Text(
                    text = uiState.message ?: "Nie udało się wczytać listy klientów",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            is ClientListUiState.Success -> {
                if (uiState.clients.isEmpty()) {
                    Text("Nie masz jeszcze żadnych klientów")
                } else {
                    LazyColumn {
                        items(uiState.clients, key = { it.id }) { client ->
                            ClientRow(client = client, onClick = { onClientClick(client.id) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClientRow(client: Client, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = client.name, style = MaterialTheme.typography.titleMedium)
            Text(text = client.email, style = MaterialTheme.typography.bodyMedium)
        }
    }
}