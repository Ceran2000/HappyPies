package pl.ceranka.happypies.screen.client_details

import pl.ceranka.happypies.data.client.Client

data class ClientDetailsUiState(
    val client: ClientState = ClientState.Loading,
    val reports: ReportsState = ReportsState.Loading,
)

sealed interface ClientState {
    data object Loading : ClientState
    data class Success(val client: Client) : ClientState
    data class Error(val message: String) : ClientState
}

sealed interface ReportsState {
    data object Loading : ReportsState
    data class Error(val message: String) : ReportsState
    data class Loaded(
        val items: List<ReportListItem>,
        val isRefreshing: Boolean,
        /** A failed refresh; the list already shown stays. */
        val actionError: String?,
    ) : ReportsState
}

data class ReportListItem(
    val id: String,
    val title: String,
    val createdAt: String,
    val authorName: String,
    val mediaCount: Int,
    val hasComment: Boolean,
)