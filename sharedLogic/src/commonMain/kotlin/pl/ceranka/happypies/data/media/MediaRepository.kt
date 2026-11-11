package pl.ceranka.happypies.data.media

import pl.ceranka.happypies.data.report.MediaItem

interface MediaRepository {

    @Throws(Exception::class)
    suspend fun uploadVideo(clientId: String, localFileUri: String, sizeBytes: Long, onProgress: (uploadedBytes: Long) -> Unit): MediaItem

    @Throws(Exception::class)
    suspend fun delete(item: MediaItem)
}