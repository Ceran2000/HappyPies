package pl.ceranka.happypies.data.firestore

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.ceranka.happypies.data.DataException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FirestoreFlowsTest {

    private val gateway = FakeFirestoreGateway()
    private val document = FirestoreDocument("abc", emptyMap())

    @Test
    fun documentFlowEmitsEveryResultAndNullWhenDeleted() = runTest {
        val seen = mutableListOf<FirestoreDocument?>()
        gateway.documentFlow("reports", "abc").onEach { seen += it }.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))

        gateway.emitDocument(DocumentResult.Success(document))
        gateway.emitDocument(DocumentResult.Success(null))

        assertEquals(listOf(document, null), seen)
    }

    @Test
    fun queryFlowEmitsTheDocuments() = runTest {
        val seen = mutableListOf<List<FirestoreDocument>>()
        gateway.queryFlow(CollectionQuery("reports")).onEach { seen += it }.launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))

        gateway.emitQuery(ReadResult.Success(listOf(document)))

        assertEquals(listOf(listOf(document)), seen)
    }

    @Test
    fun failureEndsTheFlowWithTheErrorAndRemovesTheListener() = runTest {
        val collected = async(UnconfinedTestDispatcher(testScheduler)) {
            runCatching { gateway.queryFlow(CollectionQuery("reports")).collect {} }
        }

        gateway.emitQuery(ReadResult.Failure(DataException.Unavailable("offline")))

        val error = collected.await().exceptionOrNull()
        assertIs<DataException.Unavailable>(error)
        assertEquals("offline", error.message)
        assertTrue(gateway.queryListeners.isEmpty())
    }

    @Test
    fun stoppingTheCollectionRemovesTheListener() = runTest {
        val job = gateway.documentFlow("reports", "abc").launchIn(backgroundScope + UnconfinedTestDispatcher(testScheduler))
        assertEquals(1, gateway.documentListeners.size)

        job.cancel()

        assertTrue(gateway.documentListeners.isEmpty())
    }
}