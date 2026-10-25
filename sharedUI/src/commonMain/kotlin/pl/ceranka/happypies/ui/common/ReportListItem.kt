package pl.ceranka.happypies.ui.common

import kotlinx.datetime.TimeZone
import pl.ceranka.happypies.data.report.Report

data class ReportListItem(
    val id: String,
    val title: String,
    val createdAt: String,
    val authorName: String,
    val mediaCount: Int,
    val hasComment: Boolean
)

internal fun Report.toListItem(people: ClientSpacePeople, timeZone: TimeZone) = ReportListItem(
    id = id,
    title = title,
    createdAt = formatDateTime(createdAt, timeZone),
    authorName = people.nameOf(authorId),
    mediaCount = media.size,
    hasComment = comment != null
)