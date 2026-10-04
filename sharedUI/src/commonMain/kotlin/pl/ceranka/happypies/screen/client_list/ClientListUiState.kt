package pl.ceranka.happypies.screen.client_list

import pl.ceranka.happypies.data.client.Client

sealed interface ClientListUiState {
    data object Loading : ClientListUiState
    data class Success(val clients: List<Client>) : ClientListUiState
    data class Error(val message: String?) : ClientListUiState
}