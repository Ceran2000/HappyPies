package pl.ceranka.happypies.screen.login

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.ceranka.happypies.auth.SessionManager
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.ui.common.toUserMessage

class LoginViewModel(
    private val sessionManager: SessionManager,
) : ViewModel() {

    private val log = Logger.withTag("Login")

    val emailState = TextFieldState()
    val passwordState = TextFieldState()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onSignIn() {
        if (_uiState.value == LoginUiState.Loading) return

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                sessionManager.signIn(emailState.text.toString(), passwordState.text.toString())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Sign-in failed" }
                _uiState.value = LoginUiState.Error(e.toSignInMessage())
            }
        }
    }
}

private fun Throwable.toSignInMessage(): String =
    if (this is DataException) toUserMessage()
    else "Nie udało się zalogować. Sprawdź e-mail, hasło i połączenie z internetem."