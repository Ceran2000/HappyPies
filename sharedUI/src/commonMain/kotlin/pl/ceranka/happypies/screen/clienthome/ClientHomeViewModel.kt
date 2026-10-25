package pl.ceranka.happypies.screen.clienthome

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
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.data.trainer.TrainerRepository
import pl.ceranka.happypies.ui.common.ClientSpacePeople
import pl.ceranka.happypies.ui.common.toListItem
import pl.ceranka.happypies.ui.common.toUserMessage

@OptIn(ExperimentalCoroutinesApi::class)
class ClientHomeViewModel(
    private val clientRepository: ClientRepository,
    private val trainerRepository: TrainerRepository,
    private val reportRepository: ReportRepository,
    private val clientId: String,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) : ViewModel() {

    private val log = Logger.withTag("ClientHome")

    private val retries = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    val uiState: StateFlow<ClientHomeUiState> = retries
        .onStart { emit(Unit) }
        .flatMapLatest { load() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ClientHomeUiState())

    private fun load(): Flow<ClientHomeUiState> =
        flow {
            val client = clientRepository.getClient(clientId)
            val trainer = trainerRepository.getTrainer(client.trainerId)
            val people = ClientSpacePeople(client, trainer)
            val reports = reportRepository.observeReports(clientId).map { reports ->
                ClientHomeUiState(client = client, trainer = trainer, reports = reports.map { it.toListItem(people, timeZone) })
            }
            emitAll(reports)
        }
            .onStart { emit(uiState.value.copy(error = null)) }
            .catch { e ->
                log.e(e) { "Loading the home of client $clientId failed" }
                emit(uiState.value.copy(error = e.toUserMessage()))
            }

    fun onRetry() {
        retries.tryEmit(Unit)
    }
}