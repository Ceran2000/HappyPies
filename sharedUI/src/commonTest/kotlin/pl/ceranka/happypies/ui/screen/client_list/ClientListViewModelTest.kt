package pl.ceranka.happypies.ui.screen.client_list

import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.client_list_invitation_cancelled
import happypies.sharedui.generated.resources.client_list_invitation_resent
import happypies.sharedui.generated.resources.error_unavailable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.fakes.FakeClientRepository
import pl.ceranka.happypies.fakes.FakeInvitationRepository
import pl.ceranka.happypies.fakes.accountOf
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.ewasInvitation
import pl.ceranka.happypies.fakes.trainer
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ClientListViewModelTest {

    private val clients = FakeClientRepository()
    private val invitations = FakeInvitationRepository()
    private var now = ewasInvitation.expiresAt - 1
    private val clock = object : Clock {
        override fun now() = Instant.fromEpochMilliseconds(now)
    }

    private val ewasItem = InvitationItem(ewasInvitation.id, ewasInvitation.name, ewasInvitation.email, "15.01.2027", isExpired = false)

    @BeforeTest
    fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(signedIn: Boolean = true) =
        ClientListViewModel(clients, invitations, accountOf(if (signedIn) trainer else null), clock, TimeZone.UTC)

    @Test
    fun loadsTheSignedInTrainersClientsAndInvitations() {
        val viewModel = viewModel()

        assertEquals(
            ClientListUiState.Success(listOf(anna), listOf(ewasItem), trainer.name, isRefreshing = false),
            viewModel.uiState.value
        )
        assertEquals(listOf(trainer.uid), clients.requestedTrainers)
        assertEquals(listOf(trainer.uid), invitations.requestedTrainers)
    }

    @Test
    fun marksAnInvitationPastItsExpiryAsExpired() {
        now = ewasInvitation.expiresAt

        val viewModel = viewModel()

        assertEquals(listOf(ewasItem.copy(isExpired = true)), (viewModel.uiState.value as ClientListUiState.Success).invitations)
    }

    @Test
    fun showsTheErrorMessage() {
        invitations.error = DataException.Unavailable("offline")

        val viewModel = viewModel()

        assertEquals(ClientListUiState.Error(Res.string.error_unavailable), viewModel.uiState.value)
    }

    @Test
    fun waitsWhileNobodyIsSignedIn() {
        val viewModel = viewModel(signedIn = false)

        assertEquals(ClientListUiState.Loading, viewModel.uiState.value)
        assertEquals(emptyList(), clients.requestedTrainers)
    }

    @Test
    fun reloadsOnRefresh() {
        val viewModel = viewModel()
        invitations.invitations = emptyList()

        viewModel.onRefresh()

        assertEquals(emptyList(), (viewModel.uiState.value as ClientListUiState.Success).invitations)
        assertEquals(2, clients.requestedTrainers.size)
    }

    @Test
    fun reloadsWhenInvitationsChange() {
        val viewModel = viewModel()
        invitations.invitations = emptyList()

        invitations.changes.tryEmit(Unit)

        assertEquals(emptyList(), (viewModel.uiState.value as ClientListUiState.Success).invitations)
    }

    @Test
    fun keepsTheListAndShowsASnackbarWhenARefreshFails() {
        val viewModel = viewModel()
        clients.error = DataException.Unavailable("offline")

        viewModel.onRefresh()

        assertEquals(
            ClientListUiState.Success(listOf(anna), listOf(ewasItem), trainer.name, isRefreshing = false),
            viewModel.uiState.value
        )
        assertEquals(Res.string.error_unavailable, viewModel.snackbarMessage.value)
    }

    @Test
    fun resendsTheInvitation() {
        val viewModel = viewModel()

        viewModel.onResendInvitationClick(ewasInvitation.id)

        assertEquals(listOf(ewasInvitation.id), invitations.resent)
        assertEquals(Res.string.client_list_invitation_resent, viewModel.snackbarMessage.value)
    }

    @Test
    fun cancelsTheInvitation() {
        val viewModel = viewModel()

        viewModel.onCancelInvitationClick(ewasInvitation.id)

        assertEquals(listOf(ewasInvitation.id), invitations.cancelled)
        assertEquals(Res.string.client_list_invitation_cancelled, viewModel.snackbarMessage.value)
    }

    @Test
    fun showsTheErrorWhenAnInvitationActionFails() {
        val viewModel = viewModel()
        invitations.actionError = DataException.Unavailable("offline")

        viewModel.onCancelInvitationClick(ewasInvitation.id)

        assertEquals(Res.string.error_unavailable, viewModel.snackbarMessage.value)
    }

    @Test
    fun clearsTheSnackbarMessageOnceShown() {
        val viewModel = viewModel()
        viewModel.onResendInvitationClick(ewasInvitation.id)

        viewModel.onSnackbarMessageShown()

        assertEquals(null, viewModel.snackbarMessage.value)
    }
}