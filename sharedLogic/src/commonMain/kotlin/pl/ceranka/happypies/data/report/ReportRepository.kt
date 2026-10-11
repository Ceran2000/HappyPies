package pl.ceranka.happypies.data.report

import pl.ceranka.happypies.data.user.UserRole

interface ReportRepository {

    /** Reports of one client, newest first. */
    @Throws(Exception::class)
    suspend fun getReports(clientId: String): List<Report>

    @Throws(Exception::class)
    suspend fun getReport(reportId: String): Report

    /** Creates the report and returns its id. Timestamps are set by the server. */
    @Throws(Exception::class)
    suspend fun createReport(report: NewReport): String

    @Throws(Exception::class)
    suspend fun updateReport(
        reportId: String,
        title: String,
        description: String?,
        editorId: String,
        editorName: String,
    )

    /** Sets the report's single comment, replacing the previous one. */
    @Throws(Exception::class)
    suspend fun setComment(
        reportId: String,
        text: String,
        authorId: String,
        authorName: String,
        authorRole: UserRole,
    )

    @Throws(Exception::class)
    suspend fun deleteComment(reportId: String)

    @Throws(Exception::class)
    suspend fun deleteReport(reportId: String)
}