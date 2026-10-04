package pl.ceranka.happypies.screen.client_details

import pl.ceranka.happypies.data.client.Client

sealed interface ClientDetailsUiState {
    data object Loading : ClientDetailsUiState
    data class Success(val client: Client) : ClientDetailsUiState
    data class Error(val message: String?) : ClientDetailsUiState
}