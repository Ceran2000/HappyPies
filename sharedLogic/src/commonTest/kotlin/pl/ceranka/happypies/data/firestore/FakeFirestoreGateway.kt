package pl.ceranka.happypies.data.firestore

/** In-memory gateway: returns the configured results and records every call. */
class FakeFirestoreGateway : FirestoreGateway {

    data class Write(val operation: String, val collection: String, val id: String?, val fields: Map<String, FirestoreValue>?)

    var readResult: ReadResult = ReadResult.Success(emptyList())
    var writeResult: WriteResult? = null

    val gets = mutableListOf<Pair<String, String>>()
    val queries = mutableListOf<CollectionQuery>()
    val writes = mutableListOf<Write>()

    fun returnDocuments(vararg documents: FirestoreDocument) {
        readResult = ReadResult.Success(documents.toList())
    }

    override suspend fun getDocument(collection: String, id: String): DocumentResult {
        gets += collection to id
        return when (val result = readResult) {
            is ReadResult.Success -> DocumentResult.Success(result.documents.firstOrNull())
            is ReadResult.Failure -> DocumentResult.Failure(result.error)
        }
    }

    override suspend fun query(query: CollectionQuery): ReadResult {
        queries += query
        return readResult
    }

    override suspend fun add(collection: String, fields: Map<String, FirestoreValue>): WriteResult {
        writes += Write("add", collection, null, fields)
        return writeResult ?: WriteResult.Success("new-id")
    }

    override suspend fun update(collection: String, id: String, fields: Map<String, FirestoreValue>): WriteResult {
        writes += Write("update", collection, id, fields)
        return writeResult ?: WriteResult.Success(id)
    }

    override suspend fun delete(collection: String, id: String): WriteResult {
        writes += Write("delete", collection, id, null)
        return writeResult ?: WriteResult.Success(id)
    }
}