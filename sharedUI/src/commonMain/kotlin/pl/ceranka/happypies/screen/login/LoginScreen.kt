package pl.ceranka.happypies.screen.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.data.user.UserRepository
import pl.ceranka.happypies.data.user.UserRole

@Composable
fun LoginScreen(
    authManager: AuthManager,
    userRepository: UserRepository,
    onLoggedIn: (UserRole) -> Unit,
) {
    val viewModel = viewModel { LoginViewModel(authManager, userRepository) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        val state = uiState
        if (state is LoginUiState.Success) {
            onLoggedIn(state.role)
        }
    }

    LoginContent(
        emailState = viewModel.emailState,
        passwordState = viewModel.passwordState,
        uiState = uiState,
        onSignInClick = viewModel::signIn,
    )
}

@Composable
private fun LoginContent(
    emailState: TextFieldState,
    passwordState: TextFieldState,
    uiState: LoginUiState,
    onSignInClick: () -> Unit,
) {
    val isLoading = uiState is LoginUiState.Loading

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        OutlinedTextField(
            state = emailState,
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedSecureTextField(
            state = passwordState,
            label = { Text("Hasło") },
            modifier = Modifier.fillMaxWidth()
        )
        if (uiState is LoginUiState.Error) {
            Text(uiState.message ?: "Błąd logowania", color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onSignInClick,
            enabled = !isLoading && emailState.text.isNotEmpty() && passwordState.text.isNotEmpty()
        ) {
            Text(if (isLoading) "Logowanie..." else "Zaloguj")
        }
    }
}