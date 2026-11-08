package pl.ceranka.happypies.ui.screen.client_home

import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.trainer.Trainer
import pl.ceranka.happypies.ui.common.firstNameOf
import pl.ceranka.happypies.ui.common.initialsOf

sealed interface ClientHomeUiState {

    data object Loading : ClientHomeUiState

    data class Error(val message: StringResource) : ClientHomeUiState

    data class ShowContent(val client: Client, val trainer: Trainer) : ClientHomeUiState {
        val firstName: String? = firstNameOf(client.name)
        val initials: String = initialsOf(client.name)
        val dogNames: List<String> = client.dogs.map { it.name }
    }
}