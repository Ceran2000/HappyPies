package pl.ceranka.happypies.screen.clienthome

import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.trainer.Trainer
import pl.ceranka.happypies.ui.common.ReportListItem

data class ClientHomeUiState(
    val client: Client? = null,
    val trainer: Trainer? = null,
    val reports: List<ReportListItem> = emptyList(),
    val error: String? = null
) {
    val showLoadingProgress: Boolean get() = client == null && error == null
    val showEmptyState: Boolean get() = client != null && reports.isEmpty()
    val reportCount: String get() = reports.size.toString()

    val greeting: String? get() = nameWords?.let { "Cześć, ${it.first()}" }
    val initials: String? get() = nameWords?.take(2)?.joinToString("") { it.take(1).uppercase() }
    val subtitle: String?
        get() {
            val client = client ?: return null
            val trainer = trainer ?: return null
            return (client.dogs.map { it.name } + "trener ${trainer.name}").joinToString(" · ")
        }

    /** names and surname */
    private val nameWords: List<String>? get() = client?.name?.trim()?.split(Regex("\\s+"))
}