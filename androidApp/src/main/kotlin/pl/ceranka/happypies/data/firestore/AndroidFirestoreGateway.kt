package pl.ceranka.happypies.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import pl.ceranka.happypies.core.ListenerHandle
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.FirestoreValue.ArrayValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.BooleanValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.Delete
import pl.ceranka.happypies.data.firestore.FirestoreValue.DoubleValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.LongValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.MapValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.NullValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.ServerTimestamp
import pl.ceranka.happypies.data.firestore.FirestoreValue.StringValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.TimestampValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.Unsupported
import java.util.Date

class AndroidFirestoreGateway(private val firestore: FirebaseFirestore) : FirestoreGateway {

    override suspend fun getDocument(collection: String, id: String): DocumentResult = catching({ DocumentResult.Failure(it) }) {
        val snapshot = firestore.collection(collection).document(id).get().await()
        DocumentResult.Success(if (snapshot.exists()) snapshot.toFirestoreDocument() else null)
    }

    override suspend fun query(query: CollectionQuery): ReadResult = catching({ ReadResult.Failure(it) }) {
        ReadResult.Success(query.toPlatformQuery().get().await().documents.map { it.toFirestoreDocument() })
    }

    override suspend fun add(collection: String, fields: Map<String, FirestoreValue>): WriteResult = write {
        firestore.collection(collection).add(fields.toPlatform()).await().id
    }

    override suspend fun update(collection: String, id: String, fields: Map<String, FirestoreValue>): WriteResult = write {
        firestore.collection(collection).document(id).update(fields.toPlatform()).await()
        id
    }

    override suspend fun delete(collection: String, id: String): WriteResult = write {
        firestore.collection(collection).document(id).delete().await()
        id
    }

    override fun addDocumentListener(collection: String, id: String, listener: DocumentListener): ListenerHandle {
        val registration = firestore.collection(collection).document(id).addSnapshotListener { snapshot, error ->
            if (error != null) {
                listener.onDocumentResult(DocumentResult.Failure(error.toDataException()))
            } else if (snapshot != null) {
                listener.onDocumentResult(DocumentResult.Success(if (snapshot.exists()) snapshot.toFirestoreDocument() else null))
            }
        }
        return ListenerHandle { registration.remove() }
    }

    override fun addQueryListener(query: CollectionQuery, listener: QueryListener): ListenerHandle {
        val registration = query.toPlatformQuery().addSnapshotListener { snapshot, error ->
            if (error != null) {
                listener.onQueryResult(ReadResult.Failure(error.toDataException()))
            } else if (snapshot != null) {
                listener.onQueryResult(ReadResult.Success(snapshot.documents.map { it.toFirestoreDocument() }))
            }
        }
        return ListenerHandle { registration.remove() }
    }

    private fun CollectionQuery.toPlatformQuery(): Query {
        var request: Query = firestore.collection(collection)
        whereEqualTo.forEach { (field, value) -> request = request.whereEqualTo(field, value.toPlatform()) }
        orderBy.forEach { order ->
            val direction = if (order.descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING
            request = request.orderBy(order.field, direction)
        }
        limit?.let { request = request.limit(it.toLong()) }
        return request
    }
}

private inline fun write(block: () -> String): WriteResult =
    catching({ WriteResult.Failure(it) }) { WriteResult.Success(block()) }

private inline fun <T> catching(failure: (DataException) -> T, block: () -> T): T =
    try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        failure(e.toDataException())
    }

private fun Exception.toDataException(): DataException {
    val message = message ?: this::class.java.simpleName
    if (this !is FirebaseFirestoreException) return DataException.Unknown(message)
    return when (code) {
        Code.NOT_FOUND -> DataException.NotFound(message)
        Code.PERMISSION_DENIED, Code.UNAUTHENTICATED -> DataException.PermissionDenied(message)
        Code.UNAVAILABLE, Code.DEADLINE_EXCEEDED -> DataException.Unavailable(message)
        else -> DataException.Unknown(message)
    }
}

// ESTIMATE: a document just written by this device still has a pending server timestamp.
private fun DocumentSnapshot.toFirestoreDocument() = FirestoreDocument(
    id = id,
    fields = (getData(ServerTimestampBehavior.ESTIMATE) ?: emptyMap()).mapValues { it.value.toFirestoreValue() },
)

private fun Any?.toFirestoreValue(): FirestoreValue = when (this) {
    null -> NullValue
    is String -> StringValue(this)
    is Long -> LongValue(this)
    is Int -> LongValue(toLong())
    is Double -> DoubleValue(this)
    is Boolean -> BooleanValue(this)
    is Timestamp -> TimestampValue(toDate().time)
    is List<*> -> ArrayValue(map { it.toFirestoreValue() })
    is Map<*, *> -> MapValue(entries.associate { (key, value) -> key.toString() to value.toFirestoreValue() })
    else -> Unsupported(this::class.java.simpleName)
}

private fun Map<String, FirestoreValue>.toPlatform(): Map<String, Any?> = mapValues { it.value.toPlatform() }

private fun FirestoreValue.toPlatform(): Any? = when (this) {
    is StringValue -> value
    is LongValue -> value
    is DoubleValue -> value
    is BooleanValue -> value
    is TimestampValue -> Timestamp(Date(epochMillis))
    is ArrayValue -> values.map { it.toPlatform() }
    is MapValue -> fields.toPlatform()
    NullValue -> null
    ServerTimestamp -> FieldValue.serverTimestamp()
    Delete -> FieldValue.delete()
    is Unsupported -> throw IllegalArgumentException("Cannot write a value of unsupported type $typeName")
}