package pl.ceranka.happypies.ui.screen.login

import org.jetbrains.compose.resources.StringResource

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data class Error(val message: StringResource) : LoginUiState
}