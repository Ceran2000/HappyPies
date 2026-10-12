package pl.ceranka.happypies.data.report

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.CollectionQuery
import pl.ceranka.happypies.data.firestore.DataErrorKind
import pl.ceranka.happypies.data.firestore.FakeFirestoreGateway
import pl.ceranka.happypies.data.firestore.FirestoreDocument
import pl.ceranka.happypies.data.firestore.FirestoreValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.ArrayValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.Delete
import pl.ceranka.happypies.data.firestore.FirestoreValue.LongValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.MapValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.NullValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.ServerTimestamp
import pl.ceranka.happypies.data.firestore.FirestoreValue.StringValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.TimestampValue
import pl.ceranka.happypies.data.firestore.GatewayError
import pl.ceranka.happypies.data.firestore.OrderBy
import pl.ceranka.happypies.data.firestore.ReadResult
import pl.ceranka.happypies.data.firestore.WriteResult
import pl.ceranka.happypies.data.user.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

private typealias F = ReportFirestoreFields

class ReportRepositoryImplTest {

    private val gateway = FakeFirestoreGateway()
    private val repository = ReportRepositoryImpl(gateway)

    private val comment = MapValue(
        mapOf(
            F.COMMENT_AUTHOR_ID to StringValue("trainer-1"),
            F.COMMENT_AUTHOR_ROLE to StringValue("TRAINER"),
            F.COMMENT_TEXT to StringValue("Dobra robota"),
            F.COMMENT_UPDATED_AT to TimestampValue(3_000),
        )
    )

    private val video = MapValue(
        mapOf(
            F.MEDIA_TYPE to StringValue("VIDEO"),
            F.MEDIA_STORAGE_PATH to StringValue("reports/r1/video.mp4"),
            F.MEDIA_SIZE_BYTES to LongValue(24_000_000),
        )
    )

    private val validFields: Map<String, FirestoreValue> = mapOf(
        F.CLIENT_ID to StringValue("client-1"),
        F.TITLE to StringValue("Przywołanie w parku"),
        F.DESCRIPTION to StringValue("Trzy próby"),
        F.AUTHOR_ID to StringValue("client-1"),
        F.AUTHOR_ROLE to StringValue("CLIENT"),
        F.LAST_EDITED_BY to StringValue("client-1"),
        F.CREATED_AT to TimestampValue(1_000),
        F.UPDATED_AT to TimestampValue(2_000),
        F.COMMENT to comment,
        F.MEDIA to ArrayValue(listOf(video)),
    )

    private fun reportDocument(id: String, createdAt: Long) =
        FirestoreDocument(id, validFields + (F.CREATED_AT to TimestampValue(createdAt)))

    private suspend fun readReport(fields: Map<String, FirestoreValue>): Report {
        gateway.returnDocuments(FirestoreDocument("r1", fields))
        return repository.getReport("r1")
    }

    @Test
    fun mapsACompleteDocument() = runTest {
        assertEquals(
            Report(
                id = "r1",
                clientId = "client-1",
                title = "Przywołanie w parku",
                description = "Trzy próby",
                authorId = "client-1",
                authorRole = UserRole.CLIENT,
                lastEditedBy = "client-1",
                createdAt = 1_000,
                updatedAt = 2_000,
                comment = ReportComment("trainer-1", UserRole.TRAINER, "Dobra robota", 3_000),
                media = listOf(MediaItem(MediaType.VIDEO, "reports/r1/video.mp4", 24_000_000)),
            ),
            readReport(validFields),
        )
    }

    @Test
    fun queriesTheClientsReportsNewestFirst() = runTest {
        repository.getReports("client-1")

        assertEquals(
            CollectionQuery(
                collection = "reports",
                whereEqualTo = mapOf(F.CLIENT_ID to StringValue("client-1")),
                orderBy = listOf(OrderBy(F.CREATED_AT, descending = true)),
            ),
            gateway.queries.single(),
        )
    }

    @Test
    fun returnsTheReportsInTheQueryOrder() = runTest {
        gateway.returnDocuments(reportDocument("r2", 2_000), reportDocument("r1", 1_000))

        assertEquals(listOf("r2", "r1"), repository.getReports("client-1").map { it.id })
    }

    @Test
    fun missingRequiredFieldIsMalformed() = runTest {
        val error = assertFailsWith<DataException.MalformedData> { readReport(validFields - F.TITLE) }
        assertTrue(error.message!!.contains("'title'"))
        assertTrue(error.message!!.contains("reports/r1"))
    }

    @Test
    fun wrongTypeIsMalformed() = runTest {
        assertFailsWith<DataException.MalformedData> { readReport(validFields + (F.TITLE to LongValue(1))) }
    }

    @Test
    fun invalidRoleIsMalformed() = runTest {
        assertFailsWith<DataException.MalformedData> { readReport(validFields + (F.AUTHOR_ROLE to StringValue("ADMIN"))) }
    }

    @Test
    fun missingOrNullDescriptionIsNull() = runTest {
        assertNull(readReport(validFields - F.DESCRIPTION).description)
        assertNull(readReport(validFields + (F.DESCRIPTION to NullValue)).description)
    }

    @Test
    fun missingCommentIsNull() = runTest {
        assertNull(readReport(validFields - F.COMMENT).comment)
    }

    @Test
    fun commentOfWrongTypeIsMalformed() = runTest {
        assertFailsWith<DataException.MalformedData> { readReport(validFields + (F.COMMENT to StringValue("x"))) }
    }

    @Test
    fun incompleteCommentIsMalformed() = runTest {
        val incomplete = MapValue(comment.fields - F.COMMENT_TEXT)
        val error = assertFailsWith<DataException.MalformedData> { readReport(validFields + (F.COMMENT to incomplete)) }
        assertTrue(error.message!!.contains("reports/r1.comment"))
    }

    @Test
    fun emptyMediaIsAllowedButMissingMediaIsMalformed() = runTest {
        assertEquals(emptyList(), readReport(validFields + (F.MEDIA to ArrayValue(emptyList()))).media)
        assertFailsWith<DataException.MalformedData> { readReport(validFields - F.MEDIA) }
    }

    @Test
    fun invalidMediaEntryIsMalformed() = runTest {
        assertFailsWith<DataException.MalformedData> {
            readReport(validFields + (F.MEDIA to ArrayValue(listOf(StringValue("video.mp4")))))
        }
        val badType = MapValue(video.fields + (F.MEDIA_TYPE to StringValue("AUDIO")))
        assertFailsWith<DataException.MalformedData> { readReport(validFields + (F.MEDIA to ArrayValue(listOf(badType)))) }
    }

    @Test
    fun missingReportIsNotFound() = runTest {
        assertFailsWith<DataException.NotFound> { repository.getReport("r1") }
    }

    @Test
    fun readFailureBecomesDomainException() = runTest {
        gateway.readResult = ReadResult.Failure(GatewayError(DataErrorKind.PERMISSION_DENIED, "denied"))
        assertFailsWith<DataException.PermissionDenied> { repository.getReports("client-1") }
    }

    @Test
    fun writeFailureBecomesDomainException() = runTest {
        gateway.writeResult = WriteResult.Failure(GatewayError(DataErrorKind.UNAVAILABLE, "offline"))
        assertFailsWith<DataException.Unavailable> { repository.deleteReport("r1") }
    }

    @Test
    fun createReportWritesServerTimestampsAndEmptyMedia() = runTest {
        val id = repository.createReport(NewReport("client-1", "Tytuł", description = null, "trainer-1", UserRole.TRAINER))

        val write = gateway.writes.single()
        assertEquals("new-id", id)
        assertEquals("add", write.operation)
        assertEquals("reports", write.collection)
        val fields = write.fields!!
        assertEquals(ServerTimestamp, fields[F.CREATED_AT])
        assertEquals(ServerTimestamp, fields[F.UPDATED_AT])
        assertEquals(NullValue, fields[F.DESCRIPTION])
        assertEquals(ArrayValue(emptyList()), fields[F.MEDIA])
        assertEquals(StringValue("TRAINER"), fields[F.AUTHOR_ROLE])
        assertEquals(StringValue("trainer-1"), fields[F.LAST_EDITED_BY])
    }

    @Test
    fun updateReportRecordsTheEditor() = runTest {
        repository.updateReport("r1", "Nowy tytuł", "Opis", "client-1")

        val write = gateway.writes.single()
        assertEquals("update", write.operation)
        assertEquals("r1", write.id)
        assertEquals(
            mapOf(
                F.TITLE to StringValue("Nowy tytuł"),
                F.DESCRIPTION to StringValue("Opis"),
                F.LAST_EDITED_BY to StringValue("client-1"),
                F.UPDATED_AT to ServerTimestamp,
            ),
            write.fields,
        )
    }

    @Test
    fun setCommentWritesTheWholeCommentWithServerTimestamp() = runTest {
        repository.setComment("r1", "Super", "trainer-1", UserRole.TRAINER)

        val written = gateway.writes.single().fields!![F.COMMENT] as MapValue
        assertEquals(StringValue("Super"), written.fields[F.COMMENT_TEXT])
        assertEquals(StringValue("TRAINER"), written.fields[F.COMMENT_AUTHOR_ROLE])
        assertEquals(ServerTimestamp, written.fields[F.COMMENT_UPDATED_AT])
    }

    @Test
    fun deleteCommentRemovesTheField() = runTest {
        repository.deleteComment("r1")

        assertEquals(mapOf(F.COMMENT to Delete), gateway.writes.single().fields)
    }

    @Test
    fun successfulWriteNotifiesChanges() = runTest {
        val notified = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.changes.first() }

        repository.createReport(NewReport("client-1", "Tytuł", null, "trainer-1", UserRole.TRAINER))

        assertTrue(notified.isCompleted)
    }

    @Test
    fun failedWriteDoesNotNotifyChanges() = runTest {
        val notified = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.changes.first() }
        gateway.writeResult = WriteResult.Failure(GatewayError(DataErrorKind.UNAVAILABLE, "offline"))

        assertFailsWith<DataException.Unavailable> { repository.deleteReport("r1") }

        assertTrue(notified.isActive)
    }
}