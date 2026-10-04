package pl.ceranka.happypies.screen.login

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.data.user.UserRepository

class LoginViewModel(
    private val authManager: AuthManager,
    private val userRepository: UserRepository,
) : ViewModel() {

    val emailState = TextFieldState()
    val passwordState = TextFieldState()

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun signIn() {
        viewModelScope.launch {
            _uiState.value = LoginUiState.Loading
            _uiState.value = try {
                val user = authManager.signIn(emailState.text.toString(), passwordState.text.toString())
                val role = userRepository.getRole(user.uid)
                LoginUiState.Success(role)
            } catch (e: Exception) {
                LoginUiState.Error(e.message)
            }
        }
    }
}
