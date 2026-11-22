package pl.ceranka.happypies.ui.common

import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.report.Report
import pl.ceranka.happypies.data.report.ReportEntry
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.data.trainer.Trainer

sealed interface ReportListItem {
    val id: String

    data class Readable(
        override val id: String,
        val title: String,
        val createdAt: String,
        val authorName: String,
        val hasComment: Boolean
    ) : ReportListItem

    data class Unreadable(override val id: String) : ReportListItem
}

private fun ReportEntry.toListItem(people: ClientSpacePeople, timeZone: TimeZone): ReportListItem = when (this) {
    is ReportEntry.Readable -> report.toListItem(people, timeZone)
    is ReportEntry.Unreadable -> ReportListItem.Unreadable(id)
}

private fun Report.toListItem(people: ClientSpacePeople, timeZone: TimeZone) = ReportListItem.Readable(
    id = id,
    title = title,
    createdAt = formatDate(createdAt, timeZone),
    authorName = people.nameOf(authorId),
    hasComment = comment != null
)

fun ReportRepository.reportListItems(client: Client, trainer: Trainer, timeZone: TimeZone): Flow<PagingData<ReportListItem>> {
    val people = ClientSpacePeople(client, trainer)
    return reports(client.id).map { page -> page.map { it.toListItem(people, timeZone) } }
}