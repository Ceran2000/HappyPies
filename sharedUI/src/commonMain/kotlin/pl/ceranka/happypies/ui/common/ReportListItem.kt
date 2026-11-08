package pl.ceranka.happypies.ui.common

import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.data.report.Report
import pl.ceranka.happypies.data.report.ReportRepository
import pl.ceranka.happypies.data.trainer.Trainer

data class ReportListItem(
    val id: String,
    val title: String,
    val createdAt: String,
    val authorName: String,
    val hasComment: Boolean
)

internal fun Report.toListItem(people: ClientSpacePeople, timeZone: TimeZone) = ReportListItem(
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