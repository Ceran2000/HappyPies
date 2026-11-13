package pl.ceranka.happypies.data.report

import androidx.paging.PagingConfig
import androidx.paging.PagingSource
import androidx.paging.testing.TestPager
import androidx.paging.testing.asSnapshot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.firebase.firestore.CollectionQuery
import pl.ceranka.happypies.firebase.firestore.DocumentResult
import pl.ceranka.happypies.firebase.firestore.FakeFirestoreGateway
import pl.ceranka.happypies.firebase.firestore.FakeFirestoreGateway.PageRequest
import pl.ceranka.happypies.firebase.firestore.FirestoreDocument
import pl.ceranka.happypies.firebase.firestore.FirestoreValue
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.ArrayValue
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.Delete
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.LongValue
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.MapValue
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.NullValue
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.ServerTimestamp
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.StringValue
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.TimestampValue
import pl.ceranka.happypies.firebase.firestore.OrderBy
import pl.ceranka.happypies.firebase.firestore.PageCursor
import pl.ceranka.happypies.firebase.firestore.WriteResult
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
            ReportFirestoreFields.MEDIA_STORAGE_PATH to StringValue("clients/client-1/media/v1"),
            ReportFirestoreFields.MEDIA_DOWNLOAD_URL to StringValue("https://example.com/v1"),
            ReportFirestoreFields.MEDIA_SIZE_BYTES to LongValue(24_000_000)
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
                media = listOf(MediaItem(MediaType.VIDEO, "clients/client-1/media/v1", "https://example.com/v1", 24_000_000)),
            ),
            readReport(validFields),
        )
    }

    private val firstPageQuery = CollectionQuery(
        collection = "reports",
        whereEqualTo = mapOf(ReportFirestoreFields.CLIENT_ID to StringValue("client-1")),
        orderBy = listOf(OrderBy(ReportFirestoreFields.CREATED_AT, descending = true)),
        limit = 20
    )

    private fun reportDocuments(count: Int) = (1..count).map { reportDocument("r$it", createdAt = 100_000L - it) }

    @Test
    fun loadsTwentyReportsThenTenAfterTheLastOne() = runTest {
        gateway.pageDocuments = reportDocuments(35)

        val loaded = repository.reports("client-1").asSnapshot { appendScrollWhile { true } }

        assertEquals((1..35).map { "r$it" }, loaded.map { it.id })
        assertEquals(listOf(firstPageQuery), gateway.pageQueries)
        assertEquals(listOf(PageRequest(0, 20), PageRequest(20, 10), PageRequest(30, 10)), gateway.pageRequests)
    }

    @Test
    fun aFullLastPageEndsWithAnEmptyOne() = runTest {
        gateway.pageDocuments = reportDocuments(30)

        val loaded = repository.reports("client-1").asSnapshot { appendScrollWhile { true } }

        assertEquals(30, loaded.size)
        assertEquals(listOf(PageRequest(0, 20), PageRequest(20, 10), PageRequest(30, 10)), gateway.pageRequests)
    }

    @Test
    fun readFailureIsALoadError() = runTest {
        gateway.pageError = DataException.PermissionDenied("denied")
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 20), ReportPagingSource(gateway, "client-1", onCachedRefresh = {}))

        val result = assertIs<PagingSource.LoadResult.Error<PageCursor, Report>>(pager.refresh())

        assertIs<DataException.PermissionDenied>(result.throwable)
    }

    @Test
    fun refreshFromCacheIsReported() = runTest {
        gateway.pageDocuments = reportDocuments(30)
        gateway.pageFromCache = true
        var cachedRefreshes = 0
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 20), ReportPagingSource(gateway, "client-1", onCachedRefresh = { cachedRefreshes++ }))

        pager.refresh()
        pager.append()

        assertEquals(1, cachedRefreshes)
    }

    @Test
    fun refreshFromServerIsNotReported() = runTest {
        gateway.pageDocuments = reportDocuments(30)
        var cachedRefreshes = 0
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 20), ReportPagingSource(gateway, "client-1", onCachedRefresh = { cachedRefreshes++ }))

        pager.refresh()

        assertEquals(0, cachedRefreshes)
    }

    @Test
    fun malformedReportIsALoadError() = runTest {
        gateway.pageDocuments = listOf(FirestoreDocument("r1", validFields - ReportFirestoreFields.TITLE))
        val pager = TestPager(PagingConfig(pageSize = 10, initialLoadSize = 20), ReportPagingSource(gateway, "client-1", onCachedRefresh = {}))

        val result = assertIs<PagingSource.LoadResult.Error<PageCursor, Report>>(pager.refresh())

        assertIs<DataException.MalformedData>(result.throwable)
    }

    @Test
    fun reportWritesReloadTheReports() = runTest {
        val generations = collect(repository.reports("client-1"))

        repository.createReport(NewReport("client-1", "Tytuł", description = null, "trainer-1", UserRole.TRAINER, media = emptyList()))
        repository.updateReport("r1", "Nowy tytuł", null, emptyList(), "trainer-1")
        repository.deleteReport("r1")

        assertEquals(4, generations.size)
    }

    @Test
    fun commentWritesDoNotReloadTheReports() = runTest {
        val generations = collect(repository.reports("client-1"))

        repository.setComment("r1", "Super", "client-1", UserRole.CLIENT)
        repository.deleteComment("r1")

        assertEquals(1, generations.size)
    }

    @Test
    fun failedWriteDoesNotReload() = runTest {
        val generations = collect(repository.reports("client-1"))
        gateway.writeResult = WriteResult.Failure(DataException.Unavailable("offline"))

        assertFailsWith<DataException.Unavailable> { repository.deleteReport("r1") }

        assertEquals(1, generations.size)
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
    fun mediaEntryWithoutDownloadUrlIsMalformed() = runTest {
        val withoutUrl = MapValue(video.fields - ReportFirestoreFields.MEDIA_DOWNLOAD_URL)
        assertFailsWith<DataException.MalformedData> { readReport(validFields + (ReportFirestoreFields.MEDIA to ArrayValue(listOf(withoutUrl)))) }
    }

    @Test
    fun missingReportIsNotFound() = runTest {
        assertFailsWith<DataException.NotFound> { repository.getReport("r1") }
    }

    @Test
    fun writeFailureBecomesDomainException() = runTest {
        gateway.writeResult = WriteResult.Failure(DataException.Unavailable("offline"))
        assertFailsWith<DataException.Unavailable> { repository.deleteReport("r1") }
    }

    @Test
    fun createReportWritesServerTimestampsAndEmptyMedia() = runTest {
        val id = repository.createReport(NewReport("client-1", "Tytuł", description = null, "trainer-1", UserRole.TRAINER, media = emptyList()))

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
    fun createReportWritesTheMedia() = runTest {
        val item = MediaItem(MediaType.VIDEO, "clients/client-1/media/v1", "https://example.com/v1", 24_000_000)

        repository.createReport(NewReport("client-1", "Tytuł", description = null, "trainer-1", UserRole.TRAINER, media = listOf(item)))

        assertEquals(ArrayValue(listOf(video)), gateway.writes.single().fields!![ReportFirestoreFields.MEDIA])
    }

    @Test
    fun updateReportRecordsTheEditor() = runTest {
        repository.updateReport("r1", "Nowy tytuł", "Opis", listOf(MediaItem(MediaType.VIDEO, "clients/client-1/media/v1", "https://example.com/v1", 24_000_000)), "client-1")

        val write = gateway.writes.single()
        assertEquals("update", write.operation)
        assertEquals("r1", write.id)
        assertEquals(
            mapOf(
                ReportFirestoreFields.TITLE to StringValue("Nowy tytuł"),
                ReportFirestoreFields.DESCRIPTION to StringValue("Opis"),
                ReportFirestoreFields.MEDIA to ArrayValue(listOf(video)),
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