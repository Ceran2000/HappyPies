package pl.ceranka.happypies.screen.client_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import pl.ceranka.happypies.auth.AuthSessionStore
import pl.ceranka.happypies.data.client.ClientRepository

class ClientListViewModel(
    private val clientRepository: ClientRepository,
    sessionStore: AuthSessionStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ClientListUiState>(ClientListUiState.Loading)
    val uiState: StateFlow<ClientListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            sessionStore.currentUser
                .filterNotNull()
                .collectLatest { user ->
                    _uiState.value = ClientListUiState.Loading
                    _uiState.value = try {
                        ClientListUiState.Success(clientRepository.getClients(user.uid))
                    } catch (e: Exception) {
                        ClientListUiState.Error(e.message)
                    }
                }
        }
    }
}