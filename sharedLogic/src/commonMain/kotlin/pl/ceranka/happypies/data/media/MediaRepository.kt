package pl.ceranka.happypies.data.media

import pl.ceranka.happypies.data.report.MediaItem
import pl.ceranka.happypies.data.report.MediaType

interface MediaRepository {

    @Throws(Exception::class)
    suspend fun upload(
        clientId: String,
        type: MediaType,
        localFileUri: String,
        sizeBytes: Long,
        onProgress: (uploadedBytes: Long) -> Unit
    ): MediaItem
}