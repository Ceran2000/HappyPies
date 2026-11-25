package pl.ceranka.happypies.ui.screen.invitation_code

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.compose.runtime.snapshots.Snapshot
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.error_unavailable
import happypies.sharedui.generated.resources.invitation_code_expired
import happypies.sharedui.generated.resources.invitation_code_not_found
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InvitationCodeViewModelTest {

    private val invitations = FakeInvitationRepository()
    private lateinit var viewModel: InvitationCodeViewModel

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = InvitationCodeViewModel(invitations)
    }

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun InvitationCodeViewModel.type(code: String) {
        codeState.setTextAndPlaceCursorAtEnd(code)
        Snapshot.sendApplyNotifications()
    }

    @Test
    fun disablesNextUntilTheCodeIsComplete() {
        viewModel.type("ABCD234")

        assertFalse(viewModel.uiState.value.nextButtonEnabled)
    }

    @Test
    fun countsTheCodeWithoutSurroundingSpaces() {
        viewModel.type(" ABCD234")

        assertFalse(viewModel.uiState.value.nextButtonEnabled)
    }

    @Test
    fun enablesNextForACompleteCode() {
        viewModel.type("ABCD2345")

        assertTrue(viewModel.uiState.value.nextButtonEnabled)
    }

    @Test
    fun locksTheScreenWhileChecking() {
        val checking = InvitationCodeUiState.INITIAL.copy(checkStatus = CheckStatus.CHECKING, codeComplete = true)

        assertFalse(checking.nextButtonEnabled)
        assertTrue(checking.nextButtonLoading)
        assertFalse(checking.codeFieldEnabled)
        assertFalse(checking.backButtonEnabled)
    }

    @Test
    fun findsTheInvitationByTheNormalizedCode() {
        viewModel.codeState.setTextAndPlaceCursorAtEnd(" abcd2345 ")

        viewModel.onNextClick()

        assertEquals(listOf("ABCD2345"), invitations.lookedUpCodes)
        assertEquals("ABCD2345", viewModel.code)
        assertEquals(InvitationCodeUiState.INITIAL.copy(checkStatus = CheckStatus.FOUND), viewModel.uiState.value)
    }

    @Test
    fun explainsAnUnknownCode() {
        invitations.lookupError = DataException.NotFound("no invitation")

        viewModel.onNextClick()

        assertEquals(InvitationCodeUiState.INITIAL.copy(codeFieldErrorMessage = Res.string.invitation_code_not_found), viewModel.uiState.value)
    }

    @Test
    fun explainsAnExpiredInvitation() {
        invitations.lookupError = InvitationException.Expired()

        viewModel.onNextClick()

        assertEquals(InvitationCodeUiState.INITIAL.copy(codeFieldErrorMessage = Res.string.invitation_code_expired), viewModel.uiState.value)
    }

    @Test
    fun showsTheGenericErrorMessage() {
        invitations.lookupError = DataException.Unavailable("offline")

        viewModel.onNextClick()

        assertEquals(InvitationCodeUiState.INITIAL.copy(codeFieldErrorMessage = Res.string.error_unavailable), viewModel.uiState.value)
    }

    @Test
    fun clearsTheErrorWhenTheNextCodeIsFound() {
        invitations.lookupError = DataException.NotFound("no invitation")
        viewModel.onNextClick()
        invitations.lookupError = null

        viewModel.onNextClick()

        assertEquals(InvitationCodeUiState.INITIAL.copy(checkStatus = CheckStatus.FOUND), viewModel.uiState.value)
    }

    @Test
    fun becomesIdleAfterNavigating() {
        viewModel.onNextClick()

        viewModel.onNavigatedToRegistration()

        assertEquals(InvitationCodeUiState.INITIAL, viewModel.uiState.value)
    }
}