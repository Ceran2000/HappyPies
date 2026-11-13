package pl.ceranka.happypies.firebase.storage

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

fun StorageGateway.uploadFlow(localFileUri: String, storagePath: String): Flow<UploadEvent.Progress> = callbackFlow {
    val handle = startUpload(localFileUri, storagePath) { event ->
        when (event) {
            is UploadEvent.Progress -> trySend(event)
            is UploadEvent.Success -> close()
            is UploadEvent.Failure -> close(event.error)
        }
    }
    awaitClose { handle.cancel() }
}
    .conflate()

suspend fun StorageGateway.deleteOrThrow(storagePath: String): Unit =
    when (val result = delete(storagePath)) {
        is DeleteResult.Success -> Unit
        is DeleteResult.Failure -> throw result.error
    }

suspend fun StorageGateway.downloadUrlOrThrow(storagePath: String): String =
    when (val result = getDownloadUrl(storagePath)) {
        is DownloadUrlResult.Success -> result.url
        is DownloadUrlResult.Failure -> throw result.error
    }