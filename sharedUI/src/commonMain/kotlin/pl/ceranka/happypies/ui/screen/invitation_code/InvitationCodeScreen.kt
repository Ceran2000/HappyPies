package pl.ceranka.happypies.ui.screen.invitation_code

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
import androidx.compose.foundation.text.input.InputTransformation
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.allCaps
import androidx.compose.foundation.text.input.maxLength
import androidx.compose.foundation.text.input.then
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.invitation_code_info
import happypies.sharedui.generated.resources.invitation_code_label
import happypies.sharedui.generated.resources.invitation_code_next
import happypies.sharedui.generated.resources.invitation_code_title
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pl.ceranka.happypies.data.invitation.InvitationCode
import pl.ceranka.happypies.ui.common.HappyPiesAppBar
import pl.ceranka.happypies.ui.common.ProgressButton
import pl.ceranka.happypies.ui.common.clearFocusOnTap
import pl.ceranka.happypies.ui.common.filledFieldColors

@Composable
fun InvitationCodeScreen(
    onFound: (String) -> Unit,
    onBack: () -> Unit
) {
    val viewModel = koinViewModel<InvitationCodeViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isFound) {
        if (uiState.isFound) {
            onFound(viewModel.code)
            viewModel.onNavigatedToRegistration()
        }
    }

    InvitationCodeContent(
        codeState = viewModel.codeState,
        uiState = uiState,
        onNextClick = viewModel::onNextClick,
        onBack = onBack
    )
}

@Composable
private fun InvitationCodeContent(
    codeState: TextFieldState,
    uiState: InvitationCodeUiState,
    onNextClick: () -> Unit,
    onBack: () -> Unit
) {
    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            HappyPiesAppBar(
                title = { Text(stringResource(Res.string.invitation_code_title)) },
                onBackClick = onBack,
                backButtonEnabled = uiState.backButtonEnabled
            )
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                ProgressButton(
                    text = stringResource(Res.string.invitation_code_next),
                    isLoading = uiState.nextButtonLoading,
                    enabled = uiState.nextButtonEnabled,
                    onClick = onNextClick
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
            Text(
                text = stringResource(Res.string.invitation_code_info),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                state = codeState,
                label = { Text(stringResource(Res.string.invitation_code_label), fontWeight = FontWeight.Bold) },
                labelPosition = TextFieldLabelPosition.Above(),
                inputTransformation = InputTransformation.maxLength(InvitationCode.LENGTH).then(InputTransformation.allCaps(Locale.current)),
                isError = uiState.codeFieldErrorMessage != null,
                supportingText = uiState.codeFieldErrorMessage?.let { error -> { Text(stringResource(error)) } },
                lineLimits = TextFieldLineLimits.SingleLine,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                onKeyboardAction = { if (uiState.nextButtonEnabled) onNextClick() else focusManager.clearFocus() },
                textStyle = MaterialTheme.typography.headlineSmall,
                enabled = uiState.codeFieldEnabled,
                colors = filledFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}