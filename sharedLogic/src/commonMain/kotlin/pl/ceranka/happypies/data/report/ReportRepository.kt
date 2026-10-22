package pl.ceranka.happypies.data.report

import kotlinx.coroutines.flow.Flow
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.user.UserRole

interface ReportRepository {

    /** The client's reports, newest first, updated live. Ends with a [DataException] on a read error. */
    fun observeReports(clientId: String): Flow<List<Report>>

    /** The report, updated live; `null` once it no longer exists. Ends with a [DataException] on a read error. */
    fun observeReport(reportId: String): Flow<Report?>

    @Throws(Exception::class)
    suspend fun getReport(reportId: String): Report

    @Throws(Exception::class)
    suspend fun createReport(report: NewReport): String

    @Throws(Exception::class)
    suspend fun updateReport(reportId: String, title: String, description: String?, editorId: String)

    @Throws(Exception::class)
    suspend fun setComment(reportId: String, text: String, authorId: String, authorRole: UserRole)

    @Throws(Exception::class)
    suspend fun deleteComment(reportId: String)

    @Throws(Exception::class)
    suspend fun deleteReport(reportId: String)
}