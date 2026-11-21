package pl.ceranka.happypies.ui.common

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.paging.compose.LazyPagingItems
import pl.ceranka.happypies.ui.paging.pagingItems

fun LazyListScope.reportItems(
    reports: LazyPagingItems<ReportListItem>,
    onReportClick: (reportId: String) -> Unit,
    emptyContent: @Composable () -> Unit
) {
    pagingItems(
        items = reports,
        key = { it.id },
        contentType = { it.contentType },
        emptyContent = emptyContent
    ) { report -> report.Card(onReportClick) }
}

private val ReportListItem.contentType: String get() = when (this) {
    is ReportListItem.Readable -> "report"
    is ReportListItem.Unreadable -> "unreadableReport"
}

@Composable
private fun ReportListItem.Card(onReportClick: (String) -> Unit) {
    when (this) {
        is ReportListItem.Readable -> ReportCard(this, onClick = { onReportClick(id) })
        is ReportListItem.Unreadable -> UnreadableReportCard()
    }
}