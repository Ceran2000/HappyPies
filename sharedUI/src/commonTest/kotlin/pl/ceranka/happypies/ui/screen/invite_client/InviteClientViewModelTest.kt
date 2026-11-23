package pl.ceranka.happypies.ui.screen.invite_client

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.error_unavailable
import happypies.sharedui.generated.resources.invite_client_account_exists
import happypies.sharedui.generated.resources.invite_client_already_invited
import happypies.sharedui.generated.resources.invite_client_invalid_email
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.data.invitation.InvitationException
import pl.ceranka.happypies.fakes.FakeInvitationRepository
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class InviteClientViewModelTest {

    private val invitations = FakeInvitationRepository()
    private val viewModel = InviteClientViewModel(invitations)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun fill(name: String = " Ewa Nowak ", email: String = " ewa@przyklad.pl ") {
        viewModel.nameState.setTextAndPlaceCursorAtEnd(name)
        viewModel.emailState.setTextAndPlaceCursorAtEnd(email)
    }

    @Test
    fun sendsTheTrimmedInvitation() {
        fill()

        viewModel.onSendClick()

        assertEquals(listOf("Ewa Nowak" to "ewa@przyklad.pl"), invitations.invited)
        assertEquals(InviteClientUiState.INITIAL.copy(sendStatus = SendStatus.SENT), viewModel.uiState.value)
    }

    @Test
    fun isSendingUntilTheRepositoryAnswers() {
        invitations.pendingInvite = CompletableDeferred()
        fill()

        viewModel.onSendClick()

        assertEquals(InviteClientUiState.INITIAL.copy(sendStatus = SendStatus.SENDING), viewModel.uiState.value)
    }

    @Test
    fun rejectsAnInvalidEmailWithoutSending() {
        fill(email = "ewa@przyklad")

        viewModel.onSendClick()

        assertEquals(InviteClientUiState.INITIAL.copy(emailFieldErrorMessage = Res.string.invite_client_invalid_email), viewModel.uiState.value)
        assertEquals(emptyList(), invitations.invited)
    }

    @Test
    fun sendsAfterTheEmailIsCorrected() {
        fill(email = "ewa")
        viewModel.onSendClick()
        fill()

        viewModel.onSendClick()

        assertEquals(InviteClientUiState.INITIAL.copy(sendStatus = SendStatus.SENT), viewModel.uiState.value)
    }

    @Test
    fun explainsThatTheEmailAlreadyHasAnAccount() {
        invitations.actionError = InvitationException.AccountExists()
        fill()

        viewModel.onSendClick()

        assertEquals(InviteClientUiState.INITIAL.copy(sendErrorMessage = Res.string.invite_client_account_exists), viewModel.uiState.value)
    }

    @Test
    fun explainsThatTheEmailIsAlreadyInvited() {
        invitations.actionError = InvitationException.AlreadyInvited()
        fill()

        viewModel.onSendClick()

        assertEquals(InviteClientUiState.INITIAL.copy(sendErrorMessage = Res.string.invite_client_already_invited), viewModel.uiState.value)
    }

    @Test
    fun showsTheGenericErrorMessage() {
        invitations.actionError = DataException.Unavailable("offline")
        fill()

        viewModel.onSendClick()

        assertEquals(InviteClientUiState.INITIAL.copy(sendErrorMessage = Res.string.error_unavailable), viewModel.uiState.value)
    }
}