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
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import pl.ceranka.happypies.auth.SessionManager
import pl.ceranka.happypies.auth.profileOrNull
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.report.Report
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.data.user.UserProfile
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.ui.common.formatDateTime
import pl.ceranka.happypies.ui.common.toUserMessage

class ClientDetailsViewModel(
    private val clientRepository: ClientRepository,
    private val reportRepository: ReportRepository,
    private val sessionManager: SessionManager,
    private val clientId: String,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val log = Logger.withTag("ClientDetails")

    private val _uiState = MutableStateFlow(ClientDetailsUiState())
    val uiState: StateFlow<ClientDetailsUiState> = _uiState.asStateFlow()

    private val reloadRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    init {
        viewModelScope.launch {
            merge(reloadRequests, reportRepository.changes)
                .onStart { emit(Unit) }
                .collectLatest { load() }
        }
    }

    fun onRefresh() {
        _uiState.update { it.copy(isRefreshing = true, error = null) }
        reloadRequests.tryEmit(Unit)
    }

    private suspend fun load() {
        _uiState.value = try {
            val client = clientRepository.getClient(clientId)
            val reports = reportRepository.getReports(clientId).map { it.toListItem(client) }
            ClientDetailsUiState(client = client, reports = reports)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Loading client $clientId failed" }
            _uiState.value.copy(isRefreshing = false, error = e.toUserMessage())
        }
    }

    private fun Report.toListItem(client: Client) = ReportListItem(
        id = id,
        title = title,
        createdAt = formatDateTime(createdAt, timeZone),
        authorName = authorName(authorId, authorRole, client, sessionManager.state.value.profileOrNull),
        mediaCount = media.size,
        hasComment = comment != null,
    )
}

internal fun authorName(authorId: String, authorRole: UserRole, client: Client, profile: UserProfile?): String = when {
    authorId == client.id -> client.name
    authorId == profile?.uid -> profile.name
    authorRole == UserRole.TRAINER -> "Trener"
    else -> "Klient"
}