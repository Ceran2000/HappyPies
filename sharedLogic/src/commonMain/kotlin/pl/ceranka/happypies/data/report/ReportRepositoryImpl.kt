package pl.ceranka.happypies.data.report

import androidx.paging.InvalidatingPagingSourceFactory
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.firebase.firestore.DocumentReader
import pl.ceranka.happypies.firebase.firestore.FirestoreDocument
import pl.ceranka.happypies.firebase.firestore.FirestoreGateway
import pl.ceranka.happypies.firebase.firestore.FirestoreValue
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.ArrayValue
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.LongValue
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.MapValue
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.ServerTimestamp
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.StringValue
import pl.ceranka.happypies.firebase.firestore.documentFlow
import pl.ceranka.happypies.firebase.firestore.documentIdOrThrow
import pl.ceranka.happypies.firebase.firestore.documentOrThrow
import pl.ceranka.happypies.firebase.firestore.toFirestoreValue

private typealias Fields = ReportFirestoreFields

private val pagingConfig = PagingConfig(pageSize = 10, initialLoadSize = 20, enablePlaceholders = false)

class ReportRepositoryImpl(private val gateway: FirestoreGateway) : ReportRepository {

    private val reportsChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val cachedRefreshes = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    override val cachedReloads: Flow<Unit> = cachedRefreshes

    override fun reports(clientId: String): Flow<PagingData<Report>> = channelFlow {
        val sources = InvalidatingPagingSourceFactory {
            ReportPagingSource(gateway, clientId, onCachedRefresh = { cachedRefreshes.tryEmit(Unit) })
        }
        reportsChanged.onEach { sources.invalidate() }.launchIn(this)
        Pager(pagingConfig) { sources() }.flow.collect { send(it) }
    }

    override fun observeReport(reportId: String): Flow<Report?> =
        gateway.documentFlow(Fields.COLLECTION, reportId).map { it?.toReport() }

    override suspend fun getReport(reportId: String): Report {
        return gateway.getDocument(Fields.COLLECTION, reportId).documentOrThrow { "Report $reportId does not exist" }.toReport()
    }

    override suspend fun createReport(report: NewReport): String {
        val id = gateway.add(Fields.COLLECTION, report.toFields()).documentIdOrThrow()
        reportsChanged.tryEmit(Unit)
        return id
    }

    override suspend fun updateReport(reportId: String, title: String, description: String?, media: List<MediaItem>, editorId: String) {
        val fields = mapOf(
            Fields.TITLE to StringValue(title),
            Fields.DESCRIPTION to description.toFirestoreValue(),
            Fields.MEDIA to media.toFirestoreValue(),
            Fields.LAST_EDITED_BY to StringValue(editorId),
            Fields.UPDATED_AT to ServerTimestamp,
        )
        gateway.update(Fields.COLLECTION, reportId, fields).documentIdOrThrow()
        reportsChanged.tryEmit(Unit)
    }

    override suspend fun setComment(reportId: String, text: String, authorId: String, authorRole: UserRole) {
        val comment = MapValue(
            mapOf(
                Fields.COMMENT_AUTHOR_ID to StringValue(authorId),
                Fields.COMMENT_AUTHOR_ROLE to StringValue(authorRole.name),
                Fields.COMMENT_TEXT to StringValue(text),
                Fields.COMMENT_UPDATED_AT to ServerTimestamp,
            )
        )
        gateway.update(Fields.COLLECTION, reportId, mapOf(Fields.COMMENT to comment)).documentIdOrThrow()
    }

    override suspend fun deleteComment(reportId: String) {
        gateway.update(Fields.COLLECTION, reportId, mapOf(Fields.COMMENT to FirestoreValue.Delete)).documentIdOrThrow()
    }

    override suspend fun deleteReport(reportId: String) {
        gateway.delete(Fields.COLLECTION, reportId).documentIdOrThrow()
        reportsChanged.tryEmit(Unit)
    }
}

internal fun NewReport.toFields(): Map<String, FirestoreValue> = mapOf(
    Fields.CLIENT_ID to StringValue(clientId),
    Fields.TITLE to StringValue(title),
    Fields.DESCRIPTION to description.toFirestoreValue(),
    Fields.AUTHOR_ID to StringValue(authorId),
    Fields.AUTHOR_ROLE to StringValue(authorRole.name),
    Fields.LAST_EDITED_BY to StringValue(authorId),
    Fields.CREATED_AT to ServerTimestamp,
    Fields.UPDATED_AT to ServerTimestamp,
    Fields.MEDIA to media.toFirestoreValue()
)

private fun List<MediaItem>.toFirestoreValue() = ArrayValue(
    map { item ->
        MapValue(
            mapOf(
                Fields.MEDIA_TYPE to StringValue(item.type.name),
                Fields.MEDIA_STORAGE_PATH to StringValue(item.storagePath),
                Fields.MEDIA_DOWNLOAD_URL to StringValue(item.downloadUrl),
                Fields.MEDIA_SIZE_BYTES to LongValue(item.sizeBytes)
            )
        )
    }
)

internal fun FirestoreDocument.toReport(): Report {
    val reader = DocumentReader(Fields.COLLECTION, this)
    return Report(
        id = id,
        clientId = reader.requireString(Fields.CLIENT_ID),
        title = reader.requireString(Fields.TITLE),
        description = reader.optionalString(Fields.DESCRIPTION),
        authorId = reader.requireString(Fields.AUTHOR_ID),
        authorRole = reader.requireEnum(Fields.AUTHOR_ROLE, UserRole.entries),
        lastEditedBy = reader.requireString(Fields.LAST_EDITED_BY),
        createdAt = reader.requireMillis(Fields.CREATED_AT),
        updatedAt = reader.requireMillis(Fields.UPDATED_AT),
        comment = reader.optionalMap(Fields.COMMENT)?.toComment(),
        media = reader.requireMapArray(Fields.MEDIA).map { it.toMediaItem() },
    )
}

private fun DocumentReader.toComment() = ReportComment(
    authorId = requireString(Fields.COMMENT_AUTHOR_ID),
    authorRole = requireEnum(Fields.COMMENT_AUTHOR_ROLE, UserRole.entries),
    text = requireString(Fields.COMMENT_TEXT),
    updatedAt = requireMillis(Fields.COMMENT_UPDATED_AT),
)

private fun DocumentReader.toMediaItem() = MediaItem(
    type = requireEnum(Fields.MEDIA_TYPE, MediaType.entries),
    storagePath = requireString(Fields.MEDIA_STORAGE_PATH),
    downloadUrl = requireString(Fields.MEDIA_DOWNLOAD_URL),
    sizeBytes = requireLong(Fields.MEDIA_SIZE_BYTES)
)