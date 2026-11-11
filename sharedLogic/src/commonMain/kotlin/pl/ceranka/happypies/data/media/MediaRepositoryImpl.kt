package pl.ceranka.happypies.data.media

import pl.ceranka.happypies.data.report.MediaItem
import pl.ceranka.happypies.data.report.MediaType
import pl.ceranka.happypies.firebase.storage.StorageGateway
import pl.ceranka.happypies.firebase.storage.deleteOrThrow
import pl.ceranka.happypies.firebase.storage.uploadFlow
import kotlin.uuid.Uuid

class MediaRepositoryImpl(private val gateway: StorageGateway) : MediaRepository {

    override suspend fun uploadVideo(clientId: String, localFileUri: String, sizeBytes: Long, onProgress: (uploadedBytes: Long) -> Unit): MediaItem {
        val storagePath = newStoragePath(clientId)
        gateway.uploadFlow(localFileUri, storagePath).collect { onProgress(it.uploadedBytes) }
        return MediaItem(MediaType.VIDEO, storagePath, sizeBytes)
    }

    override suspend fun delete(item: MediaItem) {
        gateway.deleteOrThrow(item.storagePath)
    }
}

private fun newStoragePath(clientId: String): String = "reports/$clientId/${Uuid.random()}"