package pl.ceranka.happypies.ui.screen.client_list

import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.ui.common.firstNameOf

sealed interface ClientListUiState {
    data object Loading : ClientListUiState

    data class Success(
        val clients: List<Client>,
        val invitations: List<InvitationItem>,
        val trainerName: String,
        val isRefreshing: Boolean
    ) : ClientListUiState {
        val trainerFirstName: String? = firstNameOf(trainerName)
        val showInvitations: Boolean get() = invitations.isNotEmpty()
    }

    data class Error(val message: StringResource) : ClientListUiState
}

data class InvitationItem(val id: String, val name: String, val email: String, val expiryDate: String, val isExpired: Boolean)