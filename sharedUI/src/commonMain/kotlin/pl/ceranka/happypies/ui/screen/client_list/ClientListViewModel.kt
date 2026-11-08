package pl.ceranka.happypies.ui.screen.client_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.accountOrNull
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.ui.common.toUserMessage

class ClientListViewModel(
    private val clientRepository: ClientRepository,
    accountRepository: AccountRepository,
) : ViewModel() {

    private val log = Logger.withTag("ClientList")

    private val _uiState = MutableStateFlow<ClientListUiState>(ClientListUiState.Loading)
    val uiState: StateFlow<ClientListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            accountRepository.state
                .map { it.accountOrNull }
                .filterNotNull()
                .distinctUntilChangedBy { it.uid }
                .collectLatest { trainer ->
                    _uiState.value = ClientListUiState.Loading
                    _uiState.value = try {
                        ClientListUiState.Success(clientRepository.getClients(trainer.uid), trainer.name)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        log.e(e) { "Loading clients of trainer ${trainer.uid} failed" }
                        ClientListUiState.Error(e.toUserMessage())
                    }
                }
        }
    }
}