package pl.ceranka.happypies.data.report

import androidx.paging.PagingSource
import androidx.paging.PagingState
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.firebase.firestore.CollectionQuery
import pl.ceranka.happypies.firebase.firestore.FirestoreDocument
import pl.ceranka.happypies.firebase.firestore.FirestoreGateway
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.StringValue
import pl.ceranka.happypies.firebase.firestore.OrderBy
import pl.ceranka.happypies.firebase.firestore.PageCursor
import pl.ceranka.happypies.firebase.firestore.pageOrThrow

class ReportPagingSource(
    private val gateway: FirestoreGateway,
    private val clientId: String,
    private val onCachedRefresh: () -> Unit
) : PagingSource<PageCursor, ReportEntry>() {

    private val log = Logger.withTag("ReportPagingSource")

    override suspend fun load(params: LoadParams<PageCursor>): LoadResult<PageCursor, ReportEntry> =
        try {
            val page = (params.key?.nextPage(params.loadSize)
                ?: gateway.queryPage(query = firstPageQuery(params.loadSize))).pageOrThrow()

            if (params is LoadParams.Refresh && page.isFromCache) onCachedRefresh()

            LoadResult.Page(
                data = page.documents.map { it.toEntry() },
                prevKey = null,
                nextKey = page.next.takeIf { page.documents.size == params.loadSize }
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Loading reports of client $clientId failed" }
            LoadResult.Error(e)
        }

    private fun FirestoreDocument.toEntry(): ReportEntry = try {
        ReportEntry.Readable(toReport())
    } catch (e: DataException.MalformedData) {
        log.e(e) { "Report $id is unreadable" }
        ReportEntry.Unreadable(id)
    }

    private fun firstPageQuery(limit: Int) = CollectionQuery(
        collection = ReportFirestoreFields.COLLECTION,
        whereEqualTo = mapOf(ReportFirestoreFields.CLIENT_ID to StringValue(clientId)),
        orderBy = listOf(OrderBy(ReportFirestoreFields.CREATED_AT, descending = true)),
        limit = limit
    )

    override fun getRefreshKey(state: PagingState<PageCursor, ReportEntry>): PageCursor? = null
}