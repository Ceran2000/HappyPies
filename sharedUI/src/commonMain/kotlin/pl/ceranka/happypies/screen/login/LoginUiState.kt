package pl.ceranka.happypies.screen.login

import pl.ceranka.happypies.data.user.UserRole

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: String?) : LoginUiState
    data class Success(val role: UserRole) : LoginUiState
}