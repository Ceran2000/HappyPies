package pl.ceranka.happypies.ui.screen.client_list

import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.ui.common.greetingFor

sealed interface ClientListUiState {
    data object Loading : ClientListUiState
    data class Success(val clients: List<Client>, val trainerName: String) : ClientListUiState {
        val greeting: String? = greetingFor(trainerName)
    }
    data class Error(val message: String?) : ClientListUiState
}