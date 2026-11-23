package pl.ceranka.happypies.ui.screen.invite_client

import org.jetbrains.compose.resources.StringResource

data class InviteClientUiState(
    val sendStatus: SendStatus,
    val emailFieldErrorMessage: StringResource?,
    val sendErrorMessage: StringResource?
) {
    val fieldsEnabled: Boolean get() = sendStatus == SendStatus.IDLE

    val isSending: Boolean get() = sendStatus == SendStatus.SENDING

    val isSent: Boolean get() = sendStatus == SendStatus.SENT

    companion object {
        val INITIAL = InviteClientUiState(
            sendStatus = SendStatus.IDLE,
            emailFieldErrorMessage = null,
            sendErrorMessage = null
        )
    }
}