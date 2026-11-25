package pl.ceranka.happypies.ui.screen.accept_invitation

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.accept_invitation_account_exists
import happypies.sharedui.generated.resources.accept_invitation_password_too_short
import happypies.sharedui.generated.resources.accept_invitation_passwords_differ
import happypies.sharedui.generated.resources.accept_invitation_sign_in_failed
import happypies.sharedui.generated.resources.error_unavailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.data.invitation.InvitationException
import pl.ceranka.happypies.fakes.FakeAuthManager
import pl.ceranka.happypies.fakes.FakeInvitationRepository
import pl.ceranka.happypies.fakes.ewasInvitationPreview
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class AcceptInvitationViewModelTest {

    private val invitations = FakeInvitationRepository()
    private val authManager = FakeAuthManager()

    private val ready = AcceptInvitationUiState.Ready(
        invitation = InvitationDetails(
            name = ewasInvitationPreview.name,
            email = ewasInvitationPreview.email,
            trainerName = ewasInvitationPreview.trainerName,
            expiryDate = "15.01.2027"
        ),
        form = RegistrationForm.INITIAL,
        passwordsCheck = PasswordsCheck.INCOMPLETE
    )

    private val filled = ready.copy(passwordsCheck = PasswordsCheck.MATCH)

    private val creating = filled.copy(form = RegistrationForm.INITIAL.copy(isCreating = true))

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel() = AcceptInvitationViewModel(invitations, authManager, CODE, TimeZone.UTC).also { viewModel ->
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect {} }
    }

    private fun AcceptInvitationViewModel.fillPasswords(password: String = PASSWORD, repeated: String = password) {
        passwordState.setTextAndPlaceCursorAtEnd(password)
        repeatPasswordState.setTextAndPlaceCursorAtEnd(repeated)
        Snapshot.sendApplyNotifications()
    }

    private fun failedToCreate(message: StringResource) =
        filled.copy(form = RegistrationForm.INITIAL.copy(createErrorMessage = message))

    @Test
    fun loadsTheInvitation() = runTest {
        val viewModel = viewModel()

        assertEquals(ready, viewModel.uiState.value)
        assertEquals(listOf(CODE), invitations.lookedUpCodes)
    }

    @Test
    fun showsAnExpiredInvitation() = runTest {
        invitations.lookupError = InvitationException.Expired()

        assertEquals(AcceptInvitationUiState.Expired, viewModel().uiState.value)
    }

    @Test
    fun showsAnInvitationThatIsNoLongerCurrent() = runTest {
        invitations.lookupError = DataException.NotFound("no invitation")

        assertEquals(AcceptInvitationUiState.NotCurrent, viewModel().uiState.value)
    }

    @Test
    fun retriesAfterALoadError() = runTest {
        invitations.lookupError = DataException.Unavailable("offline")
        val viewModel = viewModel()
        assertEquals(AcceptInvitationUiState.LoadError(Res.string.error_unavailable), viewModel.uiState.value)
        invitations.lookupError = null

        viewModel.onRetryClick()

        assertEquals(ready, viewModel.uiState.value)
    }

    @Test
    fun disablesCreatingWhilePasswordsAreEmpty() {
        assertFalse(ready.createButtonEnabled)
        assertNull(ready.repeatPasswordErrorMessage)
    }

    @Test
    fun enablesCreatingWhenPasswordsMatch() = runTest {
        val viewModel = viewModel()

        viewModel.fillPasswords()

        assertEquals(filled, viewModel.uiState.value)
        assertTrue(filled.createButtonEnabled)
        assertNull(filled.repeatPasswordErrorMessage)
    }

    @Test
    fun showsThatPasswordsDiffer() = runTest {
        val viewModel = viewModel()
        val differ = ready.copy(passwordsCheck = PasswordsCheck.DIFFER)

        viewModel.fillPasswords(repeated = "inne-haslo")

        assertEquals(differ, viewModel.uiState.value)
        assertFalse(differ.createButtonEnabled)
        assertEquals(Res.string.accept_invitation_passwords_differ, differ.repeatPasswordErrorMessage)
    }

    @Test
    fun showsThatPasswordsDifferWhenOnlyTheRepeatedOneIsFilled() = runTest {
        val viewModel = viewModel()

        viewModel.fillPasswords(password = "", repeated = "t")

        assertEquals(ready.copy(passwordsCheck = PasswordsCheck.DIFFER), viewModel.uiState.value)
    }

    @Test
    fun waitsForTheRepeatedPasswordBeforeComparing() = runTest {
        val viewModel = viewModel()

        viewModel.fillPasswords(repeated = "")

        assertEquals(ready, viewModel.uiState.value)
    }

    @Test
    fun locksTheFormWhileCreating() {
        assertFalse(creating.createButtonEnabled)
        assertTrue(creating.createButtonLoading)
        assertFalse(creating.passwordFieldsEnabled)
        assertFalse(creating.backButtonEnabled)
    }

    @Test
    fun createsTheAccountAndSignsIn() = runTest {
        val viewModel = viewModel()
        viewModel.fillPasswords()

        viewModel.onCreateAccountClick()

        assertEquals(listOf(CODE to PASSWORD), invitations.accepted)
        assertEquals(listOf(ewasInvitationPreview.email to PASSWORD), authManager.signIns)
        assertEquals(creating, viewModel.uiState.value)
    }

    @Test
    fun rejectsAShortPassword() = runTest {
        val viewModel = viewModel()
        val tooShort = filled.copy(form = RegistrationForm.INITIAL.copy(isPasswordTooShort = true))
        viewModel.fillPasswords(password = "1234567")

        viewModel.onCreateAccountClick()

        assertEquals(tooShort, viewModel.uiState.value)
        assertEquals(Res.string.accept_invitation_password_too_short, tooShort.repeatPasswordErrorMessage)
        assertEquals(emptyList(), invitations.accepted)
    }

    @Test
    fun clearsThePasswordErrorOnTheNextAttempt() = runTest {
        val viewModel = viewModel()
        viewModel.fillPasswords(password = "123")
        viewModel.onCreateAccountClick()
        viewModel.fillPasswords()

        viewModel.onCreateAccountClick()

        assertEquals(creating, viewModel.uiState.value)
    }

    @Test
    fun showsAnInvitationThatExpiredBeforeCreating() = runTest {
        invitations.acceptError = InvitationException.Expired()
        val viewModel = viewModel()
        viewModel.fillPasswords()

        viewModel.onCreateAccountClick()

        assertEquals(AcceptInvitationUiState.Expired, viewModel.uiState.value)
    }

    @Test
    fun showsAnInvitationThatDisappearedBeforeCreating() = runTest {
        invitations.acceptError = DataException.NotFound("no invitation")
        val viewModel = viewModel()
        viewModel.fillPasswords()

        viewModel.onCreateAccountClick()

        assertEquals(AcceptInvitationUiState.NotCurrent, viewModel.uiState.value)
    }

    @Test
    fun explainsThatTheEmailAlreadyHasAnAccount() = runTest {
        invitations.acceptError = InvitationException.AccountExists()
        val viewModel = viewModel()
        viewModel.fillPasswords()

        viewModel.onCreateAccountClick()

        assertEquals(failedToCreate(Res.string.accept_invitation_account_exists), viewModel.uiState.value)
        assertEquals(emptyList(), authManager.signIns)
    }

    @Test
    fun showsTheGenericErrorMessage() = runTest {
        invitations.acceptError = DataException.Unavailable("offline")
        val viewModel = viewModel()
        viewModel.fillPasswords()

        viewModel.onCreateAccountClick()

        assertEquals(failedToCreate(Res.string.error_unavailable), viewModel.uiState.value)
    }

    @Test
    fun asksToSignInWhenSigningInFailsAfterCreating() = runTest {
        authManager.signInError = DataException.Unavailable("offline")
        val viewModel = viewModel()
        viewModel.fillPasswords()

        viewModel.onCreateAccountClick()

        assertEquals(failedToCreate(Res.string.accept_invitation_sign_in_failed), viewModel.uiState.value)
    }

    private companion object {
        const val CODE = "ABCD2345"
        const val PASSWORD = "tajne-haslo"
    }
}