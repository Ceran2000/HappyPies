package pl.ceranka.happypies.screen.client_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.accountOrNull
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.report.Report
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.ui.common.formatDateTime
import pl.ceranka.happypies.ui.common.toUserMessage

class ClientDetailsViewModel(
    private val clientRepository: ClientRepository,
    private val reportRepository: ReportRepository,
    private val accountRepository: AccountRepository,
    private val clientId: String,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val log = Logger.withTag("ClientDetails")

    private val _uiState = MutableStateFlow(ClientDetailsUiState())
    val uiState: StateFlow<ClientDetailsUiState> = _uiState.asStateFlow()

    private val retries = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        viewModelScope.launch {
            retries
                .onStart { emit(Unit) }
                .collectLatest { load() }
        }
    }

    fun onRetry() {
        _uiState.update { it.copy(error = null) }
        retries.tryEmit(Unit)
    }

    // Runs until a read fails: the reports are live, so a failure ends it and only a retry starts it again.
    private suspend fun load() {
        try {
            val client = clientRepository.getClient(clientId)
            reportRepository.observeReports(clientId).collect { reports ->
                _uiState.value = ClientDetailsUiState(client = client, reports = reports.map { it.toListItem(client) })
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Loading client $clientId failed" }
            _uiState.update { it.copy(error = e.toUserMessage()) }
        }
    }

    private fun Report.toListItem(client: Client) = ReportListItem(
        id = id,
        title = title,
        createdAt = formatDateTime(createdAt, timeZone),
        authorName = authorName(authorId, authorRole, client, accountRepository.state.value.accountOrNull),
        mediaCount = media.size,
        hasComment = comment != null,
    )
}

internal fun authorName(authorId: String, authorRole: UserRole, client: Client, account: Account?): String = when {
    authorId == client.id -> client.name
    authorId == account?.uid -> account.name
    authorRole == UserRole.TRAINER -> "Trener"
    else -> "Klient"
}