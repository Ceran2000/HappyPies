package pl.ceranka.happypies.data.firestore

import pl.ceranka.happypies.core.ListenerHandle
import pl.ceranka.happypies.data.DataException

/** In-memory gateway: returns the configured results and records every call. */
class FakeFirestoreGateway : FirestoreGateway {

    data class Write(val operation: String, val collection: String, val id: String?, val fields: Map<String, FirestoreValue>?)

    /** A page read from [pageDocuments]: the index it starts at and how many documents it asks for. */
    data class PageRequest(val from: Int, val limit: Int)

    var readResult: ReadResult = ReadResult.Success(emptyList())

    /** What queryPage pages through; pageError makes it fail instead. */
    var pageDocuments: List<FirestoreDocument> = emptyList()
    var pageError: DataException? = null
    var pageFromCache = false
    val pageQueries = mutableListOf<CollectionQuery>()
    val pageRequests = mutableListOf<PageRequest>()
    var writeResult: WriteResult? = null

    val gets = mutableListOf<Pair<String, String>>()
    val queries = mutableListOf<CollectionQuery>()
    val writes = mutableListOf<Write>()

    val documentListeners = mutableListOf<DocumentListener>()
    val listenedDocuments = mutableListOf<Pair<String, String>>()

    fun emitDocument(result: DocumentResult) = documentListeners.toList().forEach { it.onDocumentResult(result) }

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

    override suspend fun queryPage(query: CollectionQuery): PageResult {
        pageQueries += query
        return page(from = 0, limit = query.limit!!)
    }

    private fun page(from: Int, limit: Int): PageResult {
        pageRequests += PageRequest(from, limit)
        pageError?.let { return PageResult.Failure(it) }
        val documents = pageDocuments.drop(from).take(limit)
        return PageResult.Success(documents, if (documents.isEmpty()) null else IndexCursor(from + documents.size), pageFromCache)
    }

    private inner class IndexCursor(private val from: Int) : PageCursor {
        override suspend fun nextPage(limit: Int): PageResult = page(from, limit)
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

    override fun addDocumentListener(collection: String, id: String, listener: DocumentListener): ListenerHandle {
        listenedDocuments += collection to id
        documentListeners += listener
        return ListenerHandle { documentListeners -= listener }
    }
}