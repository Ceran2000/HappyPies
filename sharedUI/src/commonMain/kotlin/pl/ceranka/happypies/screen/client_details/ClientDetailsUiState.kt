package pl.ceranka.happypies.screen.client_details

import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.ui.common.ReportListItem

data class ClientDetailsUiState(
    val client: Client? = null,
    val reports: List<ReportListItem> = emptyList(),
    val error: String? = null,
)