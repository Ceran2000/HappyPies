package pl.ceranka.happypies.data.report

import androidx.paging.PagingData
import kotlinx.coroutines.flow.Flow
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.data.user.UserRole

interface ReportRepository {

    /** The client's reports, newest first, in pages */
    fun reports(clientId: String): Flow<PagingData<Report>>

    /** Emits when [reports] reloaded from the device cache because the server was unreachable. */
    val cachedReloads: Flow<Unit>

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