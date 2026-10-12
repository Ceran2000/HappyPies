package pl.ceranka.happypies.screen.client_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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
    sessionManager: SessionManager,
    private val clientId: String,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    /** Raw reports state; names and dates are resolved when combined with the client and the session. */
    private sealed interface ReportsLoad {
        data object Loading : ReportsLoad
        data class Failed(val message: String) : ReportsLoad
        data class Loaded(
            val reports: List<Report>,
            val isRefreshing: Boolean = false,
            val actionError: String? = null,
        ) : ReportsLoad
    }

    private val log = Logger.withTag("ClientDetails")

    private val clientState = MutableStateFlow<ClientState>(ClientState.Loading)
    private val reportsLoad = MutableStateFlow<ReportsLoad>(ReportsLoad.Loading)
    private var reportsJob: Job? = null

    val uiState: StateFlow<ClientDetailsUiState> =
        combine(clientState, reportsLoad, sessionManager.state) { client, reports, session ->
            ClientDetailsUiState(client = client, reports = reports.toUiState(client, session.profileOrNull))
        }.stateIn(viewModelScope, SharingStarted.Eagerly, ClientDetailsUiState())

    init {
        loadClient()
        loadReports()
        viewModelScope.launch {
            reportRepository.changes.collect { refreshReports() }
        }
    }

    fun onRefresh() = refreshReports()

    fun onRetry() {
        if (clientState.value is ClientState.Error) loadClient()
        if (reportsLoad.value is ReportsLoad.Failed) loadReports()
    }

    /** Keeps the shown list while the reports reload. */
    private fun refreshReports() {
        val current = reportsLoad.value
        if (current is ReportsLoad.Loaded) {
            reportsLoad.value = current.copy(isRefreshing = true, actionError = null)
        }
        loadReports()
    }

    private fun loadClient() {
        clientState.value = ClientState.Loading
        viewModelScope.launch {
            clientState.value = try {
                ClientState.Success(clientRepository.getClient(clientId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Loading client $clientId failed" }
                ClientState.Error(e.toUserMessage())
            }
        }
    }

    /** Replaces the list; a running load is cancelled. */
    private fun loadReports() {
        reportsJob?.cancel()
        if (reportsLoad.value !is ReportsLoad.Loaded) reportsLoad.value = ReportsLoad.Loading
        reportsJob = viewModelScope.launch {
            try {
                reportsLoad.value = ReportsLoad.Loaded(reportRepository.getReports(clientId))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Loading reports of client $clientId failed" }
                reportsLoad.update { state ->
                    if (state is ReportsLoad.Loaded) {
                        state.copy(isRefreshing = false, actionError = e.toUserMessage())
                    } else {
                        ReportsLoad.Failed(e.toUserMessage())
                    }
                }
            }
        }
    }

    private fun ReportsLoad.toUiState(client: ClientState, profile: UserProfile?): ReportsState = when (this) {
        ReportsLoad.Loading -> ReportsState.Loading
        is ReportsLoad.Failed -> ReportsState.Error(message)
        is ReportsLoad.Loaded -> ReportsState.Loaded(
            items = reports.map { it.toListItem((client as? ClientState.Success)?.client, profile) },
            isRefreshing = isRefreshing,
            actionError = actionError,
        )
    }

    private fun Report.toListItem(client: Client?, profile: UserProfile?) = ReportListItem(
        id = id,
        title = title,
        createdAt = formatDateTime(createdAt, timeZone),
        authorName = authorName(authorId, authorRole, client, profile),
        mediaCount = media.size,
        hasComment = comment != null,
    )
}

/** Only the client and their trainer write reports, so both names are already on this screen (ADR 0007). */
internal fun authorName(authorId: String, authorRole: UserRole, client: Client?, profile: UserProfile?): String = when {
    client != null && authorId == client.id -> client.name
    profile != null && authorId == profile.uid -> profile.name
    authorRole == UserRole.TRAINER -> "Trener"
    else -> "Klient"
}
