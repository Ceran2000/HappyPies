package pl.ceranka.happypies.ui.screen.accept_invitation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedSecureTextField
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.accept_invitation_back_to_login
import happypies.sharedui.generated.resources.accept_invitation_create_account
import happypies.sharedui.generated.resources.accept_invitation_email_label
import happypies.sharedui.generated.resources.accept_invitation_expired_message
import happypies.sharedui.generated.resources.accept_invitation_expired_title
import happypies.sharedui.generated.resources.accept_invitation_greeting
import happypies.sharedui.generated.resources.accept_invitation_message
import happypies.sharedui.generated.resources.accept_invitation_not_current_message
import happypies.sharedui.generated.resources.accept_invitation_not_current_title
import happypies.sharedui.generated.resources.accept_invitation_password_hint
import happypies.sharedui.generated.resources.accept_invitation_password_label
import happypies.sharedui.generated.resources.accept_invitation_repeat_password_label
import happypies.sharedui.generated.resources.accept_invitation_title
import happypies.sharedui.generated.resources.accept_invitation_valid_until
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pl.ceranka.happypies.ui.common.CenteredProgress
import pl.ceranka.happypies.ui.common.ErrorWithRetry
import pl.ceranka.happypies.ui.common.HappyPiesAppBar
import pl.ceranka.happypies.ui.common.ProgressButton
import pl.ceranka.happypies.ui.common.clearFocusOnTap
import pl.ceranka.happypies.ui.common.filledFieldColors

@Composable
fun AcceptInvitationScreen(
    code: String,
    onBack: () -> Unit,
    onBackToLogin: () -> Unit
) {
    val viewModel = koinViewModel<AcceptInvitationViewModel> { parametersOf(code) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        AcceptInvitationUiState.Loading -> InvitationStatusLayout(onBack = onBack) {
            CenteredProgress()
        }
        is AcceptInvitationUiState.LoadError -> InvitationStatusLayout(onBack = onBack) {
            ErrorWithRetry(message = stringResource(state.message), onRetry = viewModel::onRetryClick)
        }
        AcceptInvitationUiState.Expired -> InvalidInvitationLayout(
            title = Res.string.accept_invitation_expired_title,
            message = Res.string.accept_invitation_expired_message,
            onBack = onBack,
            onBackToLogin = onBackToLogin
        )
        AcceptInvitationUiState.NotCurrent -> InvalidInvitationLayout(
            title = Res.string.accept_invitation_not_current_title,
            message = Res.string.accept_invitation_not_current_message,
            onBack = onBack,
            onBackToLogin = onBackToLogin
        )
        is AcceptInvitationUiState.Ready -> RegistrationLayout(
            state = state,
            passwordState = viewModel.passwordState,
            repeatPasswordState = viewModel.repeatPasswordState,
            onCreateAccountClick = viewModel::onCreateAccountClick,
            onBack = onBack
        )
    }
}

@Composable
private fun InvitationStatusLayout(onBack: () -> Unit, content: @Composable () -> Unit) {
    Scaffold(
        topBar = { HappyPiesAppBar(title = {}, onBackClick = onBack) }
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}

@Composable
private fun InvalidInvitationLayout(
    title: StringResource,
    message: StringResource,
    onBack: () -> Unit,
    onBackToLogin: () -> Unit
) {
    Scaffold(
        topBar = { HappyPiesAppBar(title = {}, onBackClick = onBack) },
        bottomBar = {
            OutlinedButton(
                onClick = onBackToLogin,
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
                    .fillMaxWidth()
            ) {
                Text(stringResource(Res.string.accept_invitation_back_to_login), style = MaterialTheme.typography.titleMedium)
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(88.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Schedule, contentDescription = null, modifier = Modifier.size(44.dp))
                }
            }
            Text(text = stringResource(title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
            Text(
                text = stringResource(message),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RegistrationLayout(
    state: AcceptInvitationUiState.Ready,
    passwordState: TextFieldState,
    repeatPasswordState: TextFieldState,
    onCreateAccountClick: () -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val onCreateClick = {
        focusManager.clearFocus()
        onCreateAccountClick()
    }
    val emailState = rememberTextFieldState(state.invitation.email)
    val colors = filledFieldColors()

    Scaffold(
        topBar = {
            HappyPiesAppBar(
                title = { Text(stringResource(Res.string.accept_invitation_title)) },
                onBackClick = onBack,
                backButtonEnabled = state.backButtonEnabled
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                ProgressButton(
                    text = stringResource(Res.string.accept_invitation_create_account),
                    isLoading = state.createButtonLoading,
                    enabled = state.createButtonEnabled,
                    onClick = onCreateClick
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .clearFocusOnTap()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ) {
                Text(
                    text = stringResource(Res.string.accept_invitation_valid_until, state.invitation.expiryDate),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
            Text(
                text = stringResource(Res.string.accept_invitation_greeting, state.invitation.greetingName),
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = stringResource(Res.string.accept_invitation_message, state.invitation.trainerName),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                state = emailState,
                label = { Text(stringResource(Res.string.accept_invitation_email_label), fontWeight = FontWeight.Bold) },
                labelPosition = TextFieldLabelPosition.Above(),
                readOnly = true,
                lineLimits = TextFieldLineLimits.SingleLine,
                colors = colors,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedSecureTextField(
                state = passwordState,
                label = { Text(stringResource(Res.string.accept_invitation_password_label), fontWeight = FontWeight.Bold) },
                labelPosition = TextFieldLabelPosition.Above(),
                supportingText = { Text(stringResource(Res.string.accept_invitation_password_hint)) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                enabled = state.passwordFieldsEnabled,
                colors = colors,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedSecureTextField(
                state = repeatPasswordState,
                label = { Text(stringResource(Res.string.accept_invitation_repeat_password_label), fontWeight = FontWeight.Bold) },
                labelPosition = TextFieldLabelPosition.Above(),
                isError = state.repeatPasswordErrorMessage != null,
                supportingText = state.repeatPasswordErrorMessage?.let { error -> { Text(stringResource(error)) } },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                onKeyboardAction = { performDefaultAction ->
                    if (state.createButtonEnabled) onCreateClick() else performDefaultAction()
                },
                enabled = state.passwordFieldsEnabled,
                colors = colors,
                modifier = Modifier.fillMaxWidth()
            )
            state.createErrorMessage?.let { error ->
                Text(text = stringResource(error), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}