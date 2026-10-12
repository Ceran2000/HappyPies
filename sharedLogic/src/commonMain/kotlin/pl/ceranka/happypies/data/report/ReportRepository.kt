package pl.ceranka.happypies.data.report

import kotlinx.coroutines.flow.Flow
import pl.ceranka.happypies.data.user.UserRole

interface ReportRepository {

    val changes: Flow<Unit>

    @Throws(Exception::class)
    suspend fun getReports(clientId: String): List<Report>

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