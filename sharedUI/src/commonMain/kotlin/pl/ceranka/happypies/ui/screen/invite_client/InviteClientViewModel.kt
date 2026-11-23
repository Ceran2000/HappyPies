package pl.ceranka.happypies.ui.screen.invite_client

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.invite_client_account_exists
import happypies.sharedui.generated.resources.invite_client_already_invited
import happypies.sharedui.generated.resources.invite_client_invalid_email
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.data.invitation.InvitationException
import pl.ceranka.happypies.data.invitation.InvitationRepository
import pl.ceranka.happypies.ui.common.toUserMessage

private val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

class InviteClientViewModel(private val invitationRepository: InvitationRepository) : ViewModel() {

    private val log = Logger.withTag("InviteClient")

    val nameState = TextFieldState()
    val emailState = TextFieldState()

    private val name: String get() = nameState.text.toString().trim()
    private val email: String get() = emailState.text.toString().trim()

    private val _uiState = MutableStateFlow(InviteClientUiState.INITIAL)
    val uiState: StateFlow<InviteClientUiState> = _uiState.asStateFlow()

    fun onSendClick() {
        clearErrors()
        if (!EMAIL.matches(email)) return showInvalidEmailError()
        sendInvitation()
    }

    private fun clearErrors() {
        _uiState.update { it.copy(emailFieldErrorMessage = null, sendErrorMessage = null) }
    }

    private fun showInvalidEmailError() {
        _uiState.update { it.copy(emailFieldErrorMessage = Res.string.invite_client_invalid_email) }
    }

    private fun sendInvitation() {
        _uiState.update { it.copy(sendStatus = SendStatus.SENDING) }
        viewModelScope.launch {
            try {
                invitationRepository.invite(name, email)
                _uiState.update { it.copy(sendStatus = SendStatus.SENT) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Sending an invitation failed" }
                _uiState.update { it.copy(sendStatus = SendStatus.IDLE, sendErrorMessage = e.toSendErrorMessage()) }
            }
        }
    }
}

private fun Exception.toSendErrorMessage(): StringResource = when (this) {
    is InvitationException.AccountExists -> Res.string.invite_client_account_exists
    is InvitationException.AlreadyInvited -> Res.string.invite_client_already_invited
    else -> toUserMessage()
}