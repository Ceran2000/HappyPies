package pl.ceranka.happypies.screen.client_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.TimeZone
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.accountOrNull
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.report.Report
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.ui.common.formatDateTime
import pl.ceranka.happypies.ui.common.personName
import pl.ceranka.happypies.ui.common.toUserMessage

@OptIn(ExperimentalCoroutinesApi::class)
class ClientDetailsViewModel(
    private val clientRepository: ClientRepository,
    private val reportRepository: ReportRepository,
    private val accountRepository: AccountRepository,
    private val clientId: String,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault(),
) : ViewModel() {

    private val log = Logger.withTag("ClientDetails")

    private val retries = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val uiState: StateFlow<ClientDetailsUiState> = retries
        .onStart { emit(Unit) }
        .flatMapLatest { load() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClientDetailsUiState())

    fun onRetry() {
        retries.tryEmit(Unit)
    }

    private fun load(): Flow<ClientDetailsUiState> =
        flow {
            val client = clientRepository.getClient(clientId)
            emitAll(reportRepository.observeReports(clientId).map { reports ->
                ClientDetailsUiState(client = client, reports = reports.map { it.toListItem(client) })
            })
        }
            .onStart { emit(uiState.value.copy(error = null)) }
            .catch { e ->
                log.e(e) { "Loading client $clientId failed" }
                emit(uiState.value.copy(error = e.toUserMessage()))
            }

    private fun Report.toListItem(client: Client) = ReportListItem(
        id = id,
        title = title,
        createdAt = formatDateTime(createdAt, timeZone),
        authorName = personName(authorId, authorRole, client, accountRepository.state.value.accountOrNull),
        mediaCount = media.size,
        hasComment = comment != null,
    )
}