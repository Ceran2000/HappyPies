package pl.ceranka.happypies.ui.screen.client_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import co.touchlab.kermit.Logger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.datetime.TimeZone
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.data.trainer.TrainerRepository
import pl.ceranka.happypies.ui.common.ReportListItem
import pl.ceranka.happypies.ui.common.reportListItems
import pl.ceranka.happypies.ui.common.toUserMessage

@OptIn(ExperimentalCoroutinesApi::class)
class ClientDetailsViewModel(
    private val clientRepository: ClientRepository,
    private val trainerRepository: TrainerRepository,
    private val reportRepository: ReportRepository,
    private val clientId: String,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) : ViewModel() {

    private val log = Logger.withTag("ClientDetails")

    private val retries = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val uiState: StateFlow<ClientDetailsUiState> = retries
        .onStart { emit(Unit) }
        .flatMapLatest { load() }
        .stateIn(viewModelScope, SharingStarted.Lazily, ClientDetailsUiState.Loading)

    val reports: Flow<PagingData<ReportListItem>> = uiState
        .filterIsInstance<ClientDetailsUiState.ShowContent>()
        .distinctUntilChanged()
        .flatMapLatest { content -> reportRepository.reportListItems(content.client, content.trainer, timeZone) }
        .cachedIn(viewModelScope)

    private val _showOfflineMessage = MutableStateFlow(false)
    val showOfflineMessage: StateFlow<Boolean> = _showOfflineMessage.asStateFlow()

    fun onRetry() {
        retries.tryEmit(Unit)
    }

    fun onOfflineMessageShown() {
        _showOfflineMessage.update { false }
    }

    private fun load(): Flow<ClientDetailsUiState> =
        flow<ClientDetailsUiState> {
            val client = clientRepository.getClient(clientId)
            emit(ClientDetailsUiState.ShowContent(client, trainerRepository.getTrainer(client.trainerId)))
        }
            .onStart { emit(ClientDetailsUiState.Loading) }
            .catch { e ->
                log.e(e) { "Loading client $clientId failed" }
                emit(ClientDetailsUiState.Error(e.toUserMessage()))
            }

    init {
        reportRepository.cachedReloads
            .onEach { _showOfflineMessage.update { true } }
            .launchIn(viewModelScope)
    }
}