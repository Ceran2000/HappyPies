package pl.ceranka.happypies.data.firestore

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import pl.ceranka.happypies.core.ListenerHandle
import pl.ceranka.happypies.data.DataException

data class FirestoreDocument(val id: String, val fields: Map<String, FirestoreValue>)

data class OrderBy(val field: String, val descending: Boolean = false)

data class CollectionQuery(
    val collection: String,
    val whereEqualTo: Map<String, FirestoreValue> = emptyMap(),
    val orderBy: List<OrderBy> = emptyList(),
    val limit: Int? = null,
) {
    init {
        require(limit == null || limit > 0) { "limit must be positive, was $limit" }
        require(whereEqualTo.values.all { it.isQueryable() }) { "whereEqualTo holds a value that cannot be queried" }
    }
}

private fun FirestoreValue.isQueryable(): Boolean = when (this) {
    is FirestoreValue.ArrayValue -> values.all { it.isQueryable() }
    is FirestoreValue.MapValue -> fields.values.all { it.isQueryable() }
    is FirestoreValue.StringValue, is FirestoreValue.LongValue, is FirestoreValue.DoubleValue, is FirestoreValue.BooleanValue,
    is FirestoreValue.TimestampValue, FirestoreValue.NullValue -> true
    FirestoreValue.ServerTimestamp, FirestoreValue.Delete, is FirestoreValue.Unsupported -> false
}

sealed class DocumentResult {
    data class Success(val document: FirestoreDocument?) : DocumentResult()
    data class Failure(val error: DataException) : DocumentResult()
}

sealed class ReadResult {
    data class Success(val documents: List<FirestoreDocument>) : ReadResult()
    data class Failure(val error: DataException) : ReadResult()
}

sealed class WriteResult {
    data class Success(val documentId: String) : WriteResult()
    data class Failure(val error: DataException) : WriteResult()
}

/**
 * The only Firestore bridge, implemented natively on Android (Kotlin) and iOS (Swift).
 *
 * Every error except cancellation is returned as `Failure`, never thrown, on both platforms:
 * what Kotlin receives when a Swift implementation throws is not documented by Kotlin/Native,
 * so each platform classifies the error into a [DataException] and passes it as data. Errors that
 * are not Firestore's (e.g. a value the SDK rejects) become [DataException.Unknown].
 * Documents are read with pending server timestamps estimated.
 *
 * Listeners report errors as a `Failure` result and never throw. After a `Failure` no more
 * results arrive. [ListenerHandle.remove] must be safe to call at any time, also after a `Failure`.
 */
interface FirestoreGateway {

    @Throws(Exception::class)
    suspend fun getDocument(collection: String, id: String): DocumentResult

    @Throws(Exception::class)
    suspend fun query(query: CollectionQuery): ReadResult

    @Throws(Exception::class)
    suspend fun add(collection: String, fields: Map<String, FirestoreValue>): WriteResult

    @Throws(Exception::class)
    suspend fun update(collection: String, id: String, fields: Map<String, FirestoreValue>): WriteResult

    @Throws(Exception::class)
    suspend fun delete(collection: String, id: String): WriteResult

    fun addDocumentListener(collection: String, id: String, listener: DocumentListener): ListenerHandle

    fun addQueryListener(query: CollectionQuery, listener: QueryListener): ListenerHandle
}

fun interface DocumentListener {
    fun onDocumentResult(result: DocumentResult)
}

fun interface QueryListener {
    fun onQueryResult(result: ReadResult)
}

fun FirestoreGateway.documentFlow(collection: String, id: String): Flow<FirestoreDocument?> =
    callbackFlow {
        val handle = addDocumentListener(collection, id) { result ->
            when (result) {
                is DocumentResult.Success -> trySend(result.document)
                is DocumentResult.Failure -> close(result.error)
            }
        }
        awaitClose { handle.remove() }
    }
        .conflate()

fun FirestoreGateway.queryFlow(query: CollectionQuery): Flow<List<FirestoreDocument>> =
    callbackFlow {
        val handle = addQueryListener(query) { result ->
            when (result) {
                is ReadResult.Success -> trySend(result.documents)
                is ReadResult.Failure -> close(result.error)
            }
        }
        awaitClose { handle.remove() }
    }
        .conflate()