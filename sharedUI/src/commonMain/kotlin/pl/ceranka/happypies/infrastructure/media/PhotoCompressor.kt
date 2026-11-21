package pl.ceranka.happypies.infrastructure.media

interface PhotoCompressor {
    suspend fun compress(uri: String): CompressedPhoto
}

data class CompressedPhoto(val uri: String, val sizeBytes: Long)