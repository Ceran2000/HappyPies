package pl.ceranka.happypies.data.report

sealed interface ReportEntry {
    val id: String

    data class Readable(val report: Report) : ReportEntry {
        override val id: String get() = report.id
    }

    data class Unreadable(override val id: String) : ReportEntry
}