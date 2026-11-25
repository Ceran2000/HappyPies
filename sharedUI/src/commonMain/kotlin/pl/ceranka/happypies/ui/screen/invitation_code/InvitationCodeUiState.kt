package pl.ceranka.happypies.ui.screen.invitation_code

import org.jetbrains.compose.resources.StringResource

data class InvitationCodeUiState(
    val checkStatus: CheckStatus,
    val codeComplete: Boolean,
    val codeFieldErrorMessage: StringResource?
) {
    val codeFieldEnabled: Boolean get() = checkStatus == CheckStatus.IDLE

    val backButtonEnabled: Boolean get() = checkStatus != CheckStatus.CHECKING

    val nextButtonEnabled: Boolean get() = checkStatus == CheckStatus.IDLE && codeComplete

    val nextButtonLoading: Boolean get() = checkStatus == CheckStatus.CHECKING

    val isFound: Boolean get() = checkStatus == CheckStatus.FOUND

    companion object {
        val INITIAL = InvitationCodeUiState(
            checkStatus = CheckStatus.IDLE,
            codeComplete = false,
            codeFieldErrorMessage = null
        )
    }
}