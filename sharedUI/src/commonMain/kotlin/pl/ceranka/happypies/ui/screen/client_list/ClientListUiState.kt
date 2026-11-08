package pl.ceranka.happypies.ui.screen.client_list

import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.ui.common.firstNameOf

sealed interface ClientListUiState {
    data object Loading : ClientListUiState
    data class Success(val clients: List<Client>, val trainerName: String) : ClientListUiState {
        val trainerFirstName: String? = firstNameOf(trainerName)
    }
    data class Error(val message: StringResource) : ClientListUiState
}