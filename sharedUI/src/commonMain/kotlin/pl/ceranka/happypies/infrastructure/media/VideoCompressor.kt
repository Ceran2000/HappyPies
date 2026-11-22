package pl.ceranka.happypies.infrastructure.media

interface VideoCompressor {
    suspend fun compress(uri: String, maxSizeBytes: Long, onProgress: (percent: Int) -> Unit): CompressedVideo
}

data class CompressedVideo(val uri: String, val sizeBytes: Long)