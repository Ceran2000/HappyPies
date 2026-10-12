package pl.ceranka.happypies.data.firestore

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
    FirestoreValue.ServerTimestamp, FirestoreValue.Delete, is FirestoreValue.Unsupported -> false
    else -> true
}

enum class DataErrorKind { NOT_FOUND, PERMISSION_DENIED, UNAUTHENTICATED, UNAVAILABLE, FAILED_PRECONDITION, OTHER }

data class GatewayError(val kind: DataErrorKind, val message: String)

sealed class DocumentResult {
    data class Success(val document: FirestoreDocument?) : DocumentResult()
    data class Failure(val error: GatewayError) : DocumentResult()
}

sealed class ReadResult {
    data class Success(val documents: List<FirestoreDocument>) : ReadResult()
    data class Failure(val error: GatewayError) : ReadResult()
}

sealed class WriteResult {
    data class Success(val documentId: String) : WriteResult()
    data class Failure(val error: GatewayError) : WriteResult()
}

/**
 * The only Firestore bridge, implemented natively on Android (Kotlin) and iOS (Swift).
 *
 * Every error except cancellation is returned as `Failure`, never thrown, on both platforms:
 * what Kotlin receives when a Swift implementation throws is not documented by Kotlin/Native,
 * so the classification travels as data. Errors that are not Firestore's (e.g. a value the SDK
 * rejects) become [DataErrorKind.OTHER]. Documents are read with pending server timestamps estimated.
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
}
