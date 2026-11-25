package pl.ceranka.happypies.ui.screen.accept_invitation

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.accept_invitation_account_exists
import happypies.sharedui.generated.resources.accept_invitation_sign_in_failed
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.data.invitation.InvitationException
import pl.ceranka.happypies.data.invitation.InvitationPreview
import pl.ceranka.happypies.data.invitation.InvitationRepository
import pl.ceranka.happypies.firebase.auth.AuthManager
import pl.ceranka.happypies.ui.common.formatDate
import pl.ceranka.happypies.ui.common.toUserMessage

private const val MIN_PASSWORD_LENGTH = 8

class AcceptInvitationViewModel(
    private val invitationRepository: InvitationRepository,
    private val authManager: AuthManager,
    private val code: String,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) : ViewModel() {

    private val log = Logger.withTag("AcceptInvitation")

    val passwordState = TextFieldState()
    val repeatPasswordState = TextFieldState()

    private val password: String get() = passwordState.text.toString()

    private val invitation = MutableStateFlow<InvitationStatus>(InvitationStatus.Loading)
    private val form = MutableStateFlow(RegistrationForm.INITIAL)
    private val passwordsCheck = snapshotFlow { PasswordsCheck.of(passwordState.text, repeatPasswordState.text) }

    val uiState: StateFlow<AcceptInvitationUiState> =
        combine(invitation, form, passwordsCheck) { invitation, form, passwordsCheck ->
            invitation.toUiState(form, passwordsCheck)
        }
            .stateIn(viewModelScope, SharingStarted.Lazily, AcceptInvitationUiState.Loading)

    fun onRetryClick() {
        loadInvitation()
    }

    private fun loadInvitation() {
        invitation.update { InvitationStatus.Loading }
        viewModelScope.launch {
            val status = try {
                InvitationStatus.Valid(invitationRepository.getInvitation(code).toDetails())
            } catch (e: CancellationException) {
                throw e
            } catch (_: InvitationException.Expired) {
                InvitationStatus.Expired
            } catch (_: DataException.NotFound) {
                InvitationStatus.NotCurrent
            } catch (e: Exception) {
                log.e(e) { "Loading the invitation failed" }
                InvitationStatus.LoadError(e.toUserMessage())
            }
            invitation.update { status }
        }
    }

    fun onCreateAccountClick() {
        if (password.length < MIN_PASSWORD_LENGTH) {
            form.update { it.copy(isPasswordTooShort = true, createErrorMessage = null) }
            return
        }
        form.update { it.copy(isCreating = true, isPasswordTooShort = false, createErrorMessage = null) }
        viewModelScope.launch { createAccount() }
    }

    private suspend fun createAccount() {
        val email = try {
            invitationRepository.accept(code, password)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Accepting the invitation failed" }
            showAcceptError(e)
            return
        }
        try {
            authManager.signIn(email, password)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Signing in after accepting the invitation failed" }
            showCreateError(Res.string.accept_invitation_sign_in_failed)
        }
    }

    private fun showAcceptError(e: Exception) {
        when (e) {
            is InvitationException.Expired -> invitation.update { InvitationStatus.Expired }
            is DataException.NotFound -> invitation.update { InvitationStatus.NotCurrent }
            is InvitationException.AccountExists -> showCreateError(Res.string.accept_invitation_account_exists)
            else -> showCreateError(e.toUserMessage())
        }
    }

    private fun showCreateError(message: StringResource) {
        form.update { it.copy(isCreating = false, createErrorMessage = message) }
    }

    private fun InvitationPreview.toDetails() = InvitationDetails(
        name = name,
        email = email,
        trainerName = trainerName,
        expiryDate = formatDate(expiresAt, timeZone)
    )

    init {
        loadInvitation()
    }
}

private sealed interface InvitationStatus {
    data object Loading : InvitationStatus

    data class LoadError(val message: StringResource) : InvitationStatus

    data object Expired : InvitationStatus

    data object NotCurrent : InvitationStatus

    data class Valid(val details: InvitationDetails) : InvitationStatus
}

private fun InvitationStatus.toUiState(form: RegistrationForm, passwordsCheck: PasswordsCheck) = when (this) {
    InvitationStatus.Loading -> AcceptInvitationUiState.Loading
    is InvitationStatus.LoadError -> AcceptInvitationUiState.LoadError(message)
    InvitationStatus.Expired -> AcceptInvitationUiState.Expired
    InvitationStatus.NotCurrent -> AcceptInvitationUiState.NotCurrent
    is InvitationStatus.Valid -> AcceptInvitationUiState.Ready(details, form, passwordsCheck)
}