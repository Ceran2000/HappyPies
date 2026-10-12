package pl.ceranka.happypies.screen.login

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.launch
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.AccountState

class LoginViewModel(
    private val authManager: AuthManager,
    private val accountRepository: AccountRepository,
) : ViewModel() {

    private val log = Logger.withTag("Login")

    val emailState = TextFieldState()
    val passwordState = TextFieldState()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.state
                .filterIsInstance<AccountState.SignedOut>()
                .filter { it.rejected }
                .collect {
                    _uiState.value = LoginUiState.Error("Konto nie jest skonfigurowane. Skontaktuj się z trenerem.")
                }
        }
    }

    fun onSignIn() {
        if (_uiState.value == LoginUiState.Loading) return

        _uiState.value = LoginUiState.Loading
        viewModelScope.launch {
            try {
                authManager.signIn(emailState.text.toString(), passwordState.text.toString())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Sign-in failed" }
                _uiState.value = LoginUiState.Error("Nie udało się zalogować. Sprawdź e-mail, hasło i połączenie z internetem.")
            }
        }
    }
}