package pl.ceranka.happypies.screen.client_details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.ceranka.happypies.data.client.ClientRepository

class ClientDetailsViewModel(
    clientRepository: ClientRepository,
    clientId: String,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ClientDetailsUiState>(ClientDetailsUiState.Loading)
    val uiState: StateFlow<ClientDetailsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = try {
                ClientDetailsUiState.Success(clientRepository.getClient(clientId))
            } catch (e: Exception) {
                ClientDetailsUiState.Error(e.message)
            }
        }
    }
}
