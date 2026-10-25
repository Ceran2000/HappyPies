package pl.ceranka.happypies.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun ReportCard(report: ReportListItem, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = report.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${report.createdAt} · Dodany przez: ${report.authorName}",
                style = MaterialTheme.typography.bodySmall
            )
            val details = buildList {
                if (report.mediaCount > 0) add("Media: ${report.mediaCount}")
                add(if (report.hasComment) "Jest komentarz" else "Bez komentarza")
            }
            Text(text = details.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
        }
    }
}