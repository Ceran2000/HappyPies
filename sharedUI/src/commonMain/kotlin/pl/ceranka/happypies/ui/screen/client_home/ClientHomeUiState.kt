package pl.ceranka.happypies.ui.screen.client_home

import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.trainer.Trainer
import pl.ceranka.happypies.ui.common.greetingFor
import pl.ceranka.happypies.ui.common.initialsOf

sealed interface ClientHomeUiState {

    data object Loading : ClientHomeUiState

    data class Error(val message: String) : ClientHomeUiState

    data class ShowContent(val client: Client, val trainer: Trainer) : ClientHomeUiState {
        val greeting: String? = greetingFor(client.name)
        val initials: String = initialsOf(client.name)
        val subtitle: String = (client.dogs.map { it.name } + "trener: ${trainer.name}").joinToString(" · ")
    }
}