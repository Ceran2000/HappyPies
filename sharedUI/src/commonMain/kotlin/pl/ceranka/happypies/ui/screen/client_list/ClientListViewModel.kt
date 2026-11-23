package pl.ceranka.happypies.ui.screen.client_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.client_list_invitation_cancelled
import happypies.sharedui.generated.resources.client_list_invitation_resent
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.accountOrNull
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.invitation.Invitation
import pl.ceranka.happypies.data.invitation.InvitationRepository
import pl.ceranka.happypies.ui.common.formatDate
import pl.ceranka.happypies.ui.common.toUserMessage
import kotlin.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class ClientListViewModel(
    private val clientRepository: ClientRepository,
    private val invitationRepository: InvitationRepository,
    accountRepository: AccountRepository,
    private val clock: Clock = Clock.System,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) : ViewModel() {

    private val log = Logger.withTag("ClientList")

    private val _uiState = MutableStateFlow<ClientListUiState>(ClientListUiState.Loading)
    val uiState: StateFlow<ClientListUiState> = _uiState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<StringResource?>(null)
    val snackbarMessage: StateFlow<StringResource?> = _snackbarMessage.asStateFlow()

    private val reloadTrainer = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    fun onRefresh() {
        reloadTrainer.tryEmit(Unit)
    }

    fun onRetryClick() {
        reloadTrainer.tryEmit(Unit)
    }

    fun onResendInvitationClick(invitationId: String) {
        runInvitationAction("Resending invitation $invitationId", Res.string.client_list_invitation_resent) {
            invitationRepository.resend(invitationId)
        }
    }

    fun onCancelInvitationClick(invitationId: String) {
        runInvitationAction("Cancelling invitation $invitationId", Res.string.client_list_invitation_cancelled) {
            invitationRepository.cancel(invitationId)
        }
    }

    fun onSnackbarMessageShown() {
        _snackbarMessage.update { null }
    }

    private fun runInvitationAction(description: String, successMessage: StringResource, action: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                action()
                _snackbarMessage.update { successMessage }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "$description failed" }
                _snackbarMessage.update { e.toUserMessage() }
            }
        }
    }

    private suspend fun load(trainer: Account) {
        _uiState.update { state ->
            when (state) {
                is ClientListUiState.Success -> state.copy(isRefreshing = true)
                ClientListUiState.Loading, is ClientListUiState.Error -> ClientListUiState.Loading
            }
        }

        try {
            val content = coroutineScope {
                val clients = async { clientRepository.getClients(trainer.uid) }
                val invitations = async { invitationRepository.getInvitations(trainer.uid) }
                ClientListUiState.Success(
                    clients = clients.await(),
                    invitations = invitations.await().map { it.toItem() },
                    trainerName = trainer.name,
                    isRefreshing = false
                )
            }
            _uiState.update { content }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Loading clients and invitations of trainer ${trainer.uid} failed" }
            showLoadError(e.toUserMessage())
        }
    }

    private fun showLoadError(message: StringResource) {
        _uiState.update { state ->
            when (state) {
                is ClientListUiState.Success -> state.copy(isRefreshing = false)
                ClientListUiState.Loading, is ClientListUiState.Error -> ClientListUiState.Error(message)
            }
        }
        if (_uiState.value is ClientListUiState.Success) {
            _snackbarMessage.update { message }
        }
    }

    private fun Invitation.toItem(): InvitationItem = InvitationItem(
        id = id,
        name = name,
        email = email,
        expiryDate = formatDate(expiresAt, timeZone),
        isExpired = isExpired(clock.now().toEpochMilliseconds())
    )

    init {
        accountRepository.state
            .mapNotNull { it.accountOrNull }
            .distinctUntilChangedBy { it.uid }
            .flatMapLatest { trainer -> merge(flowOf(Unit), reloadTrainer, invitationRepository.changes).map { trainer } }
            .mapLatest { trainer -> load(trainer) }
            .launchIn(viewModelScope)
    }
}