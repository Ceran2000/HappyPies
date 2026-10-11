package pl.ceranka.happypies.data.firestore

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.FirebaseFirestoreException.Code
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
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

    override suspend fun getDocument(collection: String, id: String): ReadResult = read {
        val snapshot = firestore.collection(collection).document(id).get().await()
        if (snapshot.exists()) listOf(snapshot.toFirestoreDocument()) else emptyList()
    }

    override suspend fun query(query: CollectionQuery): ReadResult = read {
        var request: Query = firestore.collection(query.collection)
        query.whereEqualTo.forEach { (field, value) -> request = request.whereEqualTo(field, value.toPlatform()) }
        query.orderBy?.let { field ->
            request = request.orderBy(field, if (query.descending) Query.Direction.DESCENDING else Query.Direction.ASCENDING)
        }
        query.limit?.let { request = request.limit(it.toLong()) }
        request.get().await().documents.map { it.toFirestoreDocument() }
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
}

// Only Firestore errors become a Failure; anything else (cancellation included) propagates.
private inline fun read(block: () -> List<FirestoreDocument>): ReadResult =
    try {
        ReadResult.Success(block())
    } catch (e: FirebaseFirestoreException) {
        ReadResult.Failure(e.toGatewayError())
    }

private inline fun write(block: () -> String): WriteResult =
    try {
        WriteResult.Success(block())
    } catch (e: FirebaseFirestoreException) {
        WriteResult.Failure(e.toGatewayError())
    }

private fun FirebaseFirestoreException.toGatewayError(): GatewayError {
    val kind = when (code) {
        Code.NOT_FOUND -> DataErrorKind.NOT_FOUND
        Code.PERMISSION_DENIED -> DataErrorKind.PERMISSION_DENIED
        Code.UNAUTHENTICATED -> DataErrorKind.UNAUTHENTICATED
        Code.UNAVAILABLE, Code.DEADLINE_EXCEEDED -> DataErrorKind.UNAVAILABLE
        Code.FAILED_PRECONDITION -> DataErrorKind.FAILED_PRECONDITION
        else -> DataErrorKind.OTHER
    }
    return GatewayError(kind, message ?: code.name)
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