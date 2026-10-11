package pl.ceranka.happypies.data.report

import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.CollectionQuery
import pl.ceranka.happypies.data.firestore.DocumentReader
import pl.ceranka.happypies.data.firestore.FirestoreDocument
import pl.ceranka.happypies.data.firestore.FirestoreGateway
import pl.ceranka.happypies.data.firestore.FirestoreValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.ArrayValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.MapValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.ServerTimestamp
import pl.ceranka.happypies.data.firestore.FirestoreValue.StringValue
import pl.ceranka.happypies.data.firestore.documentIdOrThrow
import pl.ceranka.happypies.data.firestore.documentsOrThrow
import pl.ceranka.happypies.data.firestore.toFirestoreValue
import pl.ceranka.happypies.data.user.UserRole

private typealias Fields = ReportFirestoreFields

class ReportRepositoryImpl(private val gateway: FirestoreGateway) : ReportRepository {

    override suspend fun getReports(clientId: String): List<Report> {
        val query = CollectionQuery(
            collection = Fields.COLLECTION,
            whereEqualTo = mapOf(Fields.CLIENT_ID to StringValue(clientId)),
            orderBy = Fields.CREATED_AT,
            descending = true,
        )
        return gateway.query(query).documentsOrThrow().map { it.toReport() }
    }

    override suspend fun getReport(reportId: String): Report {
        val document = gateway.getDocument(Fields.COLLECTION, reportId).documentsOrThrow().firstOrNull()
            ?: throw DataException.NotFound("Report $reportId does not exist")
        return document.toReport()
    }

    override suspend fun createReport(report: NewReport): String =
        gateway.add(Fields.COLLECTION, report.toFields()).documentIdOrThrow()

    override suspend fun updateReport(
        reportId: String,
        title: String,
        description: String?,
        editorId: String,
        editorName: String,
    ) {
        val fields = mapOf(
            Fields.TITLE to StringValue(title),
            Fields.DESCRIPTION to description.toFirestoreValue(),
            Fields.LAST_EDITED_BY to StringValue(editorId),
            Fields.LAST_EDITED_BY_NAME to StringValue(editorName),
            Fields.UPDATED_AT to ServerTimestamp,
        )
        gateway.update(Fields.COLLECTION, reportId, fields).documentIdOrThrow()
    }

    override suspend fun setComment(
        reportId: String,
        text: String,
        authorId: String,
        authorName: String,
        authorRole: UserRole,
    ) {
        val comment = MapValue(
            mapOf(
                Fields.COMMENT_AUTHOR_ID to StringValue(authorId),
                Fields.COMMENT_AUTHOR_NAME to StringValue(authorName),
                Fields.COMMENT_AUTHOR_ROLE to StringValue(authorRole.name),
                Fields.COMMENT_TEXT to StringValue(text),
                Fields.COMMENT_UPDATED_AT to ServerTimestamp,
            )
        )
        gateway.update(Fields.COLLECTION, reportId, mapOf(Fields.COMMENT to comment)).documentIdOrThrow()
    }

    override suspend fun deleteComment(reportId: String) {
        gateway.update(Fields.COLLECTION, reportId, mapOf(Fields.COMMENT to FirestoreValue.Delete))
            .documentIdOrThrow()
    }

    override suspend fun deleteReport(reportId: String) {
        gateway.delete(Fields.COLLECTION, reportId).documentIdOrThrow()
    }
}

internal fun NewReport.toFields(): Map<String, FirestoreValue> = mapOf(
    Fields.CLIENT_ID to StringValue(clientId),
    Fields.TITLE to StringValue(title),
    Fields.DESCRIPTION to description.toFirestoreValue(),
    Fields.AUTHOR_ID to StringValue(authorId),
    Fields.AUTHOR_NAME to StringValue(authorName),
    Fields.AUTHOR_ROLE to StringValue(authorRole.name),
    Fields.LAST_EDITED_BY to StringValue(authorId),
    Fields.LAST_EDITED_BY_NAME to StringValue(authorName),
    Fields.CREATED_AT to ServerTimestamp,
    Fields.UPDATED_AT to ServerTimestamp,
    Fields.MEDIA to ArrayValue(emptyList()),
)

internal fun FirestoreDocument.toReport(): Report {
    val reader = DocumentReader(Fields.COLLECTION, this)
    return Report(
        id = id,
        clientId = reader.requireString(Fields.CLIENT_ID),
        title = reader.requireString(Fields.TITLE),
        description = reader.optionalString(Fields.DESCRIPTION),
        authorId = reader.requireString(Fields.AUTHOR_ID),
        authorName = reader.requireString(Fields.AUTHOR_NAME),
        authorRole = reader.requireEnum(Fields.AUTHOR_ROLE, UserRole.entries),
        lastEditedBy = reader.requireString(Fields.LAST_EDITED_BY),
        lastEditedByName = reader.requireString(Fields.LAST_EDITED_BY_NAME),
        createdAt = reader.requireMillis(Fields.CREATED_AT),
        updatedAt = reader.requireMillis(Fields.UPDATED_AT),
        comment = reader.optionalMap(Fields.COMMENT)?.toComment(),
        media = reader.requireMapArray(Fields.MEDIA).map { it.toMediaItem() },
    )
}

private fun DocumentReader.toComment() = ReportComment(
    authorId = requireString(Fields.COMMENT_AUTHOR_ID),
    authorName = requireString(Fields.COMMENT_AUTHOR_NAME),
    authorRole = requireEnum(Fields.COMMENT_AUTHOR_ROLE, UserRole.entries),
    text = requireString(Fields.COMMENT_TEXT),
    updatedAt = requireMillis(Fields.COMMENT_UPDATED_AT),
)

private fun DocumentReader.toMediaItem() = MediaItem(
    type = requireEnum(Fields.MEDIA_TYPE, MediaType.entries),
    storagePath = requireString(Fields.MEDIA_STORAGE_PATH),
    sizeBytes = requireLong(Fields.MEDIA_SIZE_BYTES),
)
