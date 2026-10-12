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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginScreen() {
    val viewModel = koinViewModel<LoginViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginContent(
        emailState = viewModel.emailState,
        passwordState = viewModel.passwordState,
        uiState = uiState,
        onSignIn = viewModel::onSignIn,
    )
}

@Composable
private fun LoginContent(
    emailState: TextFieldState,
    passwordState: TextFieldState,
    uiState: LoginUiState,
    onSignIn: () -> Unit,
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
            Text(uiState.message, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onSignIn,
            enabled = !isLoading && emailState.text.isNotEmpty() && passwordState.text.isNotEmpty()
        ) {
            Text(if (isLoading) "Logowanie..." else "Zaloguj")
        }
    }
}