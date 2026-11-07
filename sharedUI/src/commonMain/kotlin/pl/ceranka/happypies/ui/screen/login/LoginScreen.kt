package pl.ceranka.happypies.ui.screen.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import pl.ceranka.happypies.ui.common.AppLogo
import pl.ceranka.happypies.ui.common.filledFieldColors

@Composable
fun LoginScreen() {
    val viewModel = koinViewModel<LoginViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LoginContent(
        emailState = viewModel.emailState,
        passwordState = viewModel.passwordState,
        uiState = uiState,
        onSignIn = viewModel::onSignIn
    )
}

@Composable
private fun LoginContent(
    emailState: TextFieldState,
    passwordState: TextFieldState,
    uiState: LoginUiState,
    onSignIn: () -> Unit
) {
    val isLoading = uiState is LoginUiState.Loading
    val fieldsFilled by remember(emailState, passwordState) {
        derivedStateOf { emailState.text.isNotEmpty() && passwordState.text.isNotEmpty() }
    }
    val signInButtonEnabled = !isLoading && fieldsFilled
    val showError = uiState is LoginUiState.Error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        AppLogo(modifier = Modifier.align(Alignment.CenterHorizontally))
        OutlinedTextField(
            state = emailState,
            label = { Text("E-mail") },
            labelPosition = TextFieldLabelPosition.Above(),
            lineLimits = TextFieldLineLimits.SingleLine,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            colors = filledFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedSecureTextField(
            state = passwordState,
            label = { Text("Hasło") },
            labelPosition = TextFieldLabelPosition.Above(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            onKeyboardAction = { performDefaultAction ->
                if (signInButtonEnabled) onSignIn() else performDefaultAction()
            },
            colors = filledFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
        if (showError) {
            Text(uiState.message, color = MaterialTheme.colorScheme.error)
        }
        Button(
            onClick = onSignIn,
            enabled = signInButtonEnabled,
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = LocalContentColor.current,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(24.dp)
                )
            } else {
                Text("Zaloguj się", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}