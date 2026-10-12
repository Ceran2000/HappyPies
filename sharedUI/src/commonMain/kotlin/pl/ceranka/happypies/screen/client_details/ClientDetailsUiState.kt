package pl.ceranka.happypies.screen.client_details

import pl.ceranka.happypies.data.client.Client

data class ClientDetailsUiState(
    val client: Client? = null,
    val reports: List<ReportListItem> = emptyList(),
    val isRefreshing: Boolean = false,
    val error: String? = null,
)

data class ReportListItem(
    val id: String,
    val title: String,
    val createdAt: String,
    val authorName: String,
    val mediaCount: Int,
    val hasComment: Boolean,
)