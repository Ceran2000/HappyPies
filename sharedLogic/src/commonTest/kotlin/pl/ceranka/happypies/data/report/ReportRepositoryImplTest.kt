package pl.ceranka.happypies.data.report

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.CollectionQuery
import pl.ceranka.happypies.data.firestore.DocumentResult
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
import pl.ceranka.happypies.data.firestore.OrderBy
import pl.ceranka.happypies.data.firestore.ReadResult
import pl.ceranka.happypies.data.firestore.WriteResult
import pl.ceranka.happypies.data.user.UserRole
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ReportRepositoryImplTest {

    private val gateway = FakeFirestoreGateway()
    private val repository = ReportRepositoryImpl(gateway)

    private val comment = MapValue(
        mapOf(
            ReportFirestoreFields.COMMENT_AUTHOR_ID to StringValue("trainer-1"),
            ReportFirestoreFields.COMMENT_AUTHOR_ROLE to StringValue("TRAINER"),
            ReportFirestoreFields.COMMENT_TEXT to StringValue("Dobra robota"),
            ReportFirestoreFields.COMMENT_UPDATED_AT to TimestampValue(3_000),
        )
    )

    private val video = MapValue(
        mapOf(
            ReportFirestoreFields.MEDIA_TYPE to StringValue("VIDEO"),
            ReportFirestoreFields.MEDIA_STORAGE_PATH to StringValue("reports/r1/video.mp4"),
            ReportFirestoreFields.MEDIA_SIZE_BYTES to LongValue(24_000_000),
        )
    )

    private val validFields: Map<String, FirestoreValue> = mapOf(
        ReportFirestoreFields.CLIENT_ID to StringValue("client-1"),
        ReportFirestoreFields.TITLE to StringValue("Przywołanie w parku"),
        ReportFirestoreFields.DESCRIPTION to StringValue("Trzy próby"),
        ReportFirestoreFields.AUTHOR_ID to StringValue("client-1"),
        ReportFirestoreFields.AUTHOR_ROLE to StringValue("CLIENT"),
        ReportFirestoreFields.LAST_EDITED_BY to StringValue("client-1"),
        ReportFirestoreFields.CREATED_AT to TimestampValue(1_000),
        ReportFirestoreFields.UPDATED_AT to TimestampValue(2_000),
        ReportFirestoreFields.COMMENT to comment,
        ReportFirestoreFields.MEDIA to ArrayValue(listOf(video)),
    )

    private fun reportDocument(id: String, createdAt: Long) =
        FirestoreDocument(id, validFields + (ReportFirestoreFields.CREATED_AT to TimestampValue(createdAt)))

    private suspend fun readReport(fields: Map<String, FirestoreValue>): Report {
        gateway.returnDocuments(FirestoreDocument("r1", fields))
        return repository.getReport("r1")
    }

    private fun <T> TestScope.collect(flow: Flow<T>): List<T> {
        val seen = mutableListOf<T>()
        flow.onEach { seen += it }.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))
        return seen
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
        collect(repository.observeReports("client-1"))

        assertEquals(
            CollectionQuery(
                collection = "reports",
                whereEqualTo = mapOf(ReportFirestoreFields.CLIENT_ID to StringValue("client-1")),
                orderBy = listOf(OrderBy(ReportFirestoreFields.CREATED_AT, descending = true)),
            ),
            gateway.listenedQueries.single(),
        )
    }

    @Test
    fun emitsTheReportsInTheQueryOrderOnEveryChange() = runTest {
        val seen = collect(repository.observeReports("client-1"))

        gateway.emitQuery(ReadResult.Success(listOf(reportDocument("r1", 1_000))))
        gateway.emitQuery(ReadResult.Success(listOf(reportDocument("r2", 2_000), reportDocument("r1", 1_000))))

        assertEquals(listOf(listOf("r1"), listOf("r2", "r1")), seen.map { reports -> reports.map { it.id } })
    }

    @Test
    fun observesASingleReportUntilItIsDeleted() = runTest {
        val seen = collect(repository.observeReport("r1"))

        gateway.emitDocument(DocumentResult.Success(FirestoreDocument("r1", validFields)))
        gateway.emitDocument(DocumentResult.Success(null))

        assertEquals(listOf("r1", null), seen.map { it?.id })
        assertEquals("reports" to "r1", gateway.listenedDocuments.single())
    }

    @Test
    fun malformedDocumentEndsTheReportFlow() = runTest {
        val collected = async(UnconfinedTestDispatcher(testScheduler)) { runCatching { repository.observeReport("r1").collect {} } }

        gateway.emitDocument(DocumentResult.Success(FirestoreDocument("r1", validFields - ReportFirestoreFields.TITLE)))

        assertIs<DataException.MalformedData>(collected.await().exceptionOrNull())
        assertTrue(gateway.documentListeners.isEmpty())
    }

    @Test
    fun missingRequiredFieldIsMalformed() = runTest {
        val error = assertFailsWith<DataException.MalformedData> { readReport(validFields - ReportFirestoreFields.TITLE) }
        assertTrue(error.message!!.contains("'title'"))
        assertTrue(error.message!!.contains("reports/r1"))
    }

    @Test
    fun wrongTypeIsMalformed() = runTest {
        assertFailsWith<DataException.MalformedData> { readReport(validFields + (ReportFirestoreFields.TITLE to LongValue(1))) }
    }

    @Test
    fun invalidRoleIsMalformed() = runTest {
        assertFailsWith<DataException.MalformedData> { readReport(validFields + (ReportFirestoreFields.AUTHOR_ROLE to StringValue("ADMIN"))) }
    }

    @Test
    fun missingOrNullDescriptionIsNull() = runTest {
        assertNull(readReport(validFields - ReportFirestoreFields.DESCRIPTION).description)
        assertNull(readReport(validFields + (ReportFirestoreFields.DESCRIPTION to NullValue)).description)
    }

    @Test
    fun missingCommentIsNull() = runTest {
        assertNull(readReport(validFields - ReportFirestoreFields.COMMENT).comment)
    }

    @Test
    fun commentOfWrongTypeIsMalformed() = runTest {
        assertFailsWith<DataException.MalformedData> { readReport(validFields + (ReportFirestoreFields.COMMENT to StringValue("x"))) }
    }

    @Test
    fun incompleteCommentIsMalformed() = runTest {
        val incomplete = MapValue(comment.fields - ReportFirestoreFields.COMMENT_TEXT)
        val error = assertFailsWith<DataException.MalformedData> { readReport(validFields + (ReportFirestoreFields.COMMENT to incomplete)) }
        assertTrue(error.message!!.contains("reports/r1.comment"))
    }

    @Test
    fun emptyMediaIsAllowedButMissingMediaIsMalformed() = runTest {
        assertEquals(emptyList(), readReport(validFields + (ReportFirestoreFields.MEDIA to ArrayValue(emptyList()))).media)
        assertFailsWith<DataException.MalformedData> { readReport(validFields - ReportFirestoreFields.MEDIA) }
    }

    @Test
    fun invalidMediaEntryIsMalformed() = runTest {
        assertFailsWith<DataException.MalformedData> {
            readReport(validFields + (ReportFirestoreFields.MEDIA to ArrayValue(listOf(StringValue("video.mp4")))))
        }
        val badType = MapValue(video.fields + (ReportFirestoreFields.MEDIA_TYPE to StringValue("AUDIO")))
        assertFailsWith<DataException.MalformedData> { readReport(validFields + (ReportFirestoreFields.MEDIA to ArrayValue(listOf(badType)))) }
    }

    @Test
    fun missingReportIsNotFound() = runTest {
        assertFailsWith<DataException.NotFound> { repository.getReport("r1") }
    }

    @Test
    fun readFailureEndsTheReportsFlow() = runTest {
        val collected = async(UnconfinedTestDispatcher(testScheduler)) { runCatching { repository.observeReports("client-1").collect {} } }

        gateway.emitQuery(ReadResult.Failure(DataException.PermissionDenied("denied")))

        assertIs<DataException.PermissionDenied>(collected.await().exceptionOrNull())
    }

    @Test
    fun writeFailureBecomesDomainException() = runTest {
        gateway.writeResult = WriteResult.Failure(DataException.Unavailable("offline"))
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
        assertEquals(ServerTimestamp, fields[ReportFirestoreFields.CREATED_AT])
        assertEquals(ServerTimestamp, fields[ReportFirestoreFields.UPDATED_AT])
        assertEquals(NullValue, fields[ReportFirestoreFields.DESCRIPTION])
        assertEquals(ArrayValue(emptyList()), fields[ReportFirestoreFields.MEDIA])
        assertEquals(StringValue("TRAINER"), fields[ReportFirestoreFields.AUTHOR_ROLE])
        assertEquals(StringValue("trainer-1"), fields[ReportFirestoreFields.LAST_EDITED_BY])
    }

    @Test
    fun updateReportRecordsTheEditor() = runTest {
        repository.updateReport("r1", "Nowy tytuł", "Opis", "client-1")

        val write = gateway.writes.single()
        assertEquals("update", write.operation)
        assertEquals("r1", write.id)
        assertEquals(
            mapOf(
                ReportFirestoreFields.TITLE to StringValue("Nowy tytuł"),
                ReportFirestoreFields.DESCRIPTION to StringValue("Opis"),
                ReportFirestoreFields.LAST_EDITED_BY to StringValue("client-1"),
                ReportFirestoreFields.UPDATED_AT to ServerTimestamp,
            ),
            write.fields,
        )
    }

    @Test
    fun setCommentWritesTheWholeCommentWithServerTimestamp() = runTest {
        repository.setComment("r1", "Super", "trainer-1", UserRole.TRAINER)

        val written = gateway.writes.single().fields!![ReportFirestoreFields.COMMENT] as MapValue
        assertEquals(StringValue("Super"), written.fields[ReportFirestoreFields.COMMENT_TEXT])
        assertEquals(StringValue("TRAINER"), written.fields[ReportFirestoreFields.COMMENT_AUTHOR_ROLE])
        assertEquals(ServerTimestamp, written.fields[ReportFirestoreFields.COMMENT_UPDATED_AT])
    }

    @Test
    fun deleteCommentRemovesTheField() = runTest {
        repository.deleteComment("r1")

        assertEquals(mapOf(ReportFirestoreFields.COMMENT to Delete), gateway.writes.single().fields)
    }

}