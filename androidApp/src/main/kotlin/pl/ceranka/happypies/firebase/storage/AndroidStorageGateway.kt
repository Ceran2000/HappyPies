package pl.ceranka.happypies.firebase.storage

import androidx.core.net.toUri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await
import pl.ceranka.happypies.core.DataException

class AndroidStorageGateway(private val storage: FirebaseStorage) : StorageGateway {

    override fun startUpload(
        localFileUri: String,
        storagePath: String,
        listener: UploadListener
    ): UploadHandle {
        val reference = storage.reference.child(storagePath)
        val uploadTask = reference.putFile(localFileUri.toUri())
        uploadTask
            .addOnSuccessListener { listener.onUploadEvent(UploadEvent.Success) }
            .addOnFailureListener { listener.onUploadEvent(UploadEvent.Failure(it.toDataException())) }
            .addOnProgressListener { listener.onUploadEvent(UploadEvent.Progress(it.bytesTransferred, it.totalByteCount)) }

        return UploadHandle { uploadTask.cancel() }
    }

    override suspend fun delete(storagePath: String): DeleteResult {
        return try {
            storage.reference.child(storagePath).delete().await()
            DeleteResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DeleteResult.Failure(e.toDataException())
        }
    }

    override suspend fun getDownloadUrl(storagePath: String): DownloadUrlResult {
        return try {
            val downloadUrl = storage.reference.child(storagePath).downloadUrl.await()
            DownloadUrlResult.Success(downloadUrl.toString())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            DownloadUrlResult.Failure(e.toDataException())
        }
    }

}

private fun Exception.toDataException(): DataException {
    if (this !is StorageException) return DataException.Unknown(message)
    return when (errorCode) {
        StorageException.ERROR_OBJECT_NOT_FOUND -> DataException.NotFound(message)
        StorageException.ERROR_NOT_AUTHORIZED, StorageException.ERROR_NOT_AUTHENTICATED -> DataException.PermissionDenied(message)
        StorageException.ERROR_RETRY_LIMIT_EXCEEDED -> DataException.Unavailable(message)
        else -> DataException.Unknown(message)
    }
}