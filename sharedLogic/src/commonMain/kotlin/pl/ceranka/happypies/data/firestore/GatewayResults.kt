package pl.ceranka.happypies.data.firestore

import pl.ceranka.happypies.data.DataException

internal fun GatewayError.toDataException(): DataException = when (kind) {
    DataErrorKind.NOT_FOUND -> DataException.NotFound(message)
    DataErrorKind.PERMISSION_DENIED, DataErrorKind.UNAUTHENTICATED -> DataException.PermissionDenied(message)
    DataErrorKind.UNAVAILABLE -> DataException.Unavailable(message)
    DataErrorKind.FAILED_PRECONDITION, DataErrorKind.OTHER -> DataException.Unknown(message)
}

internal inline fun DocumentResult.documentOrThrow(notFoundMessage: () -> String): FirestoreDocument = when (this) {
    is DocumentResult.Success -> document ?: throw DataException.NotFound(notFoundMessage())
    is DocumentResult.Failure -> throw error.toDataException()
}

internal fun ReadResult.documentsOrThrow(): List<FirestoreDocument> = when (this) {
    is ReadResult.Success -> documents
    is ReadResult.Failure -> throw error.toDataException()
}

internal fun WriteResult.documentIdOrThrow(): String = when (this) {
    is WriteResult.Success -> documentId
    is WriteResult.Failure -> throw error.toDataException()
}
