package pl.ceranka.happypies.ui.screen.accept_invitation

import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.accept_invitation_password_too_short
import happypies.sharedui.generated.resources.accept_invitation_passwords_differ
import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.ui.common.firstNameOf

sealed interface AcceptInvitationUiState {
    data object Loading : AcceptInvitationUiState

    data class LoadError(val message: StringResource) : AcceptInvitationUiState

    data object Expired : AcceptInvitationUiState

    data object NotCurrent : AcceptInvitationUiState

    data class Ready(
        val invitation: InvitationDetails,
        val form: RegistrationForm,
        val passwordsCheck: PasswordsCheck
    ) : AcceptInvitationUiState {
        val passwordFieldsEnabled: Boolean get() = !form.isCreating

        val backButtonEnabled: Boolean get() = !form.isCreating

        val createButtonEnabled: Boolean get() = !form.isCreating && passwordsCheck == PasswordsCheck.MATCH

        val createButtonLoading: Boolean get() = form.isCreating

        val repeatPasswordErrorMessage: StringResource? get() = when {
            passwordsCheck == PasswordsCheck.DIFFER -> Res.string.accept_invitation_passwords_differ
            form.isPasswordTooShort -> Res.string.accept_invitation_password_too_short
            else -> null
        }

        val createErrorMessage: StringResource? get() = form.createErrorMessage
    }
}

data class InvitationDetails(
    val name: String,
    val email: String,
    val trainerName: String,
    val expiryDate: String
) {
    val greetingName: String get() = firstNameOf(name) ?: name
}

data class RegistrationForm(
    val isCreating: Boolean,
    val isPasswordTooShort: Boolean,
    val createErrorMessage: StringResource?
) {
    companion object {
        val INITIAL = RegistrationForm(
            isCreating = false,
            isPasswordTooShort = false,
            createErrorMessage = null
        )
    }
}

enum class PasswordsCheck {
    INCOMPLETE,
    DIFFER,
    MATCH;

    companion object {
        fun of(password: CharSequence, repeated: CharSequence): PasswordsCheck = when {
            password.isNotEmpty() && password.contentEquals(repeated) -> MATCH
            repeated.isNotEmpty() && !password.contentEquals(repeated) -> DIFFER
            else -> INCOMPLETE
        }
    }
}