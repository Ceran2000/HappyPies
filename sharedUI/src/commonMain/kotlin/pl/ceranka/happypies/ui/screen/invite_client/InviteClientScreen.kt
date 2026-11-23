package pl.ceranka.happypies.ui.screen.invite_client

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.invite_client_email_label
import happypies.sharedui.generated.resources.invite_client_info
import happypies.sharedui.generated.resources.invite_client_name_label
import happypies.sharedui.generated.resources.invite_client_send
import happypies.sharedui.generated.resources.invite_client_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pl.ceranka.happypies.ui.common.HappyPiesAppBar
import pl.ceranka.happypies.ui.common.ProgressButton
import pl.ceranka.happypies.ui.common.clearFocusOnTap
import pl.ceranka.happypies.ui.common.filledFieldColors

@Composable
fun InviteClientScreen(
    onSent: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel = koinViewModel<InviteClientViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSent) {
        if (uiState.isSent) onSent()
    }

    InviteClientContent(
        nameState = viewModel.nameState,
        emailState = viewModel.emailState,
        uiState = uiState,
        onSendClick = viewModel::onSendClick,
        onBack = onBack
    )
}

@Composable
private fun InviteClientContent(
    nameState: TextFieldState,
    emailState: TextFieldState,
    uiState: InviteClientUiState,
    onSendClick: () -> Unit,
    onBack: () -> Unit
) {
    val fieldsFilled by remember(nameState, emailState) {
        derivedStateOf { nameState.text.isNotBlank() && emailState.text.isNotBlank() }
    }
    val sendButtonEnabled = uiState.fieldsEnabled && fieldsFilled
    val focusManager = LocalFocusManager.current
    val colors = filledFieldColors()

    Scaffold(
        topBar = {
            HappyPiesAppBar(
                title = { Text(stringResource(Res.string.invite_client_title)) },
                onBackClick = onBack,
                backButtonEnabled = !uiState.isSending
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                ProgressButton(
                    text = stringResource(Res.string.invite_client_send),
                    isLoading = uiState.isSending,
                    enabled = sendButtonEnabled,
                    onClick = onSendClick
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
            OutlinedTextField(
                state = nameState,
                label = { Text(stringResource(Res.string.invite_client_name_label), fontWeight = FontWeight.Bold) },
                labelPosition = TextFieldLabelPosition.Above(),
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words, imeAction = ImeAction.Next),
                enabled = uiState.fieldsEnabled,
                colors = colors,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                state = emailState,
                label = { Text(stringResource(Res.string.invite_client_email_label), fontWeight = FontWeight.Bold) },
                labelPosition = TextFieldLabelPosition.Above(),
                isError = uiState.emailFieldErrorMessage != null,
                supportingText = uiState.emailFieldErrorMessage?.let { error -> { Text(stringResource(error)) } },
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Done),
                onKeyboardAction = { if (sendButtonEnabled) onSendClick() else focusManager.clearFocus() },
                enabled = uiState.fieldsEnabled,
                colors = colors,
                modifier = Modifier.fillMaxWidth()
            )
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(Res.string.invite_client_info),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                )
            }
            uiState.sendErrorMessage?.let { error ->
                Text(text = stringResource(error), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}