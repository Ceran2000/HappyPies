package pl.ceranka.happypies.ui.screen.client_details

import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.trainer.Trainer

sealed interface ClientDetailsUiState {

    data object Loading : ClientDetailsUiState

    data class Error(val message: StringResource) : ClientDetailsUiState

    data class ShowContent(val client: Client, val trainer: Trainer) : ClientDetailsUiState
}