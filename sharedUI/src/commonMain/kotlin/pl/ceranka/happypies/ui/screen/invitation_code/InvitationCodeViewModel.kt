package pl.ceranka.happypies.ui.screen.invitation_code

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.invitation_code_expired
import happypies.sharedui.generated.resources.invitation_code_not_found
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.data.invitation.InvitationCode
import pl.ceranka.happypies.data.invitation.InvitationException
import pl.ceranka.happypies.data.invitation.InvitationRepository
import pl.ceranka.happypies.ui.common.toUserMessage

class InvitationCodeViewModel(private val invitationRepository: InvitationRepository) : ViewModel() {

    private val log = Logger.withTag("InvitationCode")

    val codeState = TextFieldState()

    val code: String get() = InvitationCode.normalize(codeState.text)

    private val _uiState = MutableStateFlow(InvitationCodeUiState.INITIAL)
    val uiState: StateFlow<InvitationCodeUiState> = _uiState.asStateFlow()

    fun onNextClick() {
        _uiState.update {
            it.copy(checkStatus = CheckStatus.CHECKING, codeFieldErrorMessage = null)
        }

        viewModelScope.launch {
            try {
                invitationRepository.getInvitation(code)
                _uiState.update { it.copy(checkStatus = CheckStatus.FOUND) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Checking an invitation code failed" }
                _uiState.update { it.copy(checkStatus = CheckStatus.IDLE, codeFieldErrorMessage = e.toCheckErrorMessage()) }
            }
        }
    }

    fun onNavigatedToRegistration() {
        _uiState.update { it.copy(checkStatus = CheckStatus.IDLE) }
    }

    init {
        snapshotFlow { code.length == InvitationCode.LENGTH }
            .onEach { complete -> _uiState.update { it.copy(codeComplete = complete) } }
            .launchIn(viewModelScope)
    }
}

private fun Exception.toCheckErrorMessage(): StringResource = when (this) {
    is DataException.NotFound -> Res.string.invitation_code_not_found
    is InvitationException.Expired -> Res.string.invitation_code_expired
    else -> toUserMessage()
}