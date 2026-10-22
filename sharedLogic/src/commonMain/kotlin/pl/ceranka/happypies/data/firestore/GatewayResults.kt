package pl.ceranka.happypies.data.firestore

import pl.ceranka.happypies.data.DataException

internal inline fun DocumentResult.documentOrThrow(notFoundMessage: () -> String): FirestoreDocument = when (this) {
    is DocumentResult.Success -> document ?: throw DataException.NotFound(notFoundMessage())
    is DocumentResult.Failure -> throw error
}

internal fun ReadResult.documentsOrThrow(): List<FirestoreDocument> = when (this) {
    is ReadResult.Success -> documents
    is ReadResult.Failure -> throw error
}

internal fun WriteResult.documentIdOrThrow(): String = when (this) {
    is WriteResult.Success -> documentId
    is WriteResult.Failure -> throw error
}