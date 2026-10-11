package pl.ceranka.happypies.data.firestore

data class FirestoreDocument(val id: String, val fields: Map<String, FirestoreValue>)

data class CollectionQuery(
    val collection: String,
    val whereEqualTo: Map<String, FirestoreValue> = emptyMap(),
    val orderBy: String? = null,
    val descending: Boolean = false,
    val limit: Int? = null,
)

enum class DataErrorKind { NOT_FOUND, PERMISSION_DENIED, UNAUTHENTICATED, UNAVAILABLE, FAILED_PRECONDITION, OTHER }

data class GatewayError(val kind: DataErrorKind, val message: String)

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
 * Firestore errors are returned as `Failure`, not thrown: what Kotlin receives when a Swift
 * implementation throws is not documented by Kotlin/Native, so the classification travels as data.
 * Documents are read with pending server timestamps estimated.
 */
interface FirestoreGateway {

    /** A list with the document, or an empty list if it does not exist. */
    @Throws(Exception::class)
    suspend fun getDocument(collection: String, id: String): ReadResult

    @Throws(Exception::class)
    suspend fun query(query: CollectionQuery): ReadResult

    @Throws(Exception::class)
    suspend fun add(collection: String, fields: Map<String, FirestoreValue>): WriteResult

    @Throws(Exception::class)
    suspend fun update(collection: String, id: String, fields: Map<String, FirestoreValue>): WriteResult

    @Throws(Exception::class)
    suspend fun delete(collection: String, id: String): WriteResult
}
