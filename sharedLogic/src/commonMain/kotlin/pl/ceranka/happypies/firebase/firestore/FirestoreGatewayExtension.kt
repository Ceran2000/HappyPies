package pl.ceranka.happypies.firebase.firestore

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

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