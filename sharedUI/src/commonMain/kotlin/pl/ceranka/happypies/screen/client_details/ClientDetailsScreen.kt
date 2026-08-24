package pl.ceranka.happypies.screen.client_details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.client.ClientRepository

@Composable
fun ClientDetailsScreen(
    clientRepository: ClientRepository,
    clientId: String,
    onBack: () -> Unit,
) {
    val viewModel = viewModel { ClientDetailsViewModel(clientRepository, clientId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    ClientDetailsContent(
        uiState = uiState,
        onBack = onBack
    )
}

@Composable
private fun ClientDetailsContent(
    uiState: ClientDetailsUiState,
    onBack: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) {
            Text("← Wstecz")
        }
        when (uiState) {
            is ClientDetailsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ClientDetailsUiState.Error -> {
                Text(
                    text = uiState.message ?: "Nie udało się wczytać danych klienta",
                    color = MaterialTheme.colorScheme.error,
                )
            }
            is ClientDetailsUiState.Success -> {
                ClientDetailsFields(client = uiState.client)
            }
        }
    }
}

@Composable
private fun ClientDetailsFields(client: Client) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
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
