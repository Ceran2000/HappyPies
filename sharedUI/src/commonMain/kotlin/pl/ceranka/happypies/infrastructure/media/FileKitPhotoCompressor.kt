package pl.ceranka.happypies.infrastructure.media

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.compressImage
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.write
import kotlin.uuid.Uuid

class FileKitPhotoCompressor : PhotoCompressor {

    override suspend fun compress(uri: String): CompressedPhoto {
        val bytes = FileKit.compressImage(
            file = platformFileFromUri(uri),
            quality = PHOTO_JPEG_QUALITY,
            maxWidth = MAX_PHOTO_SIDE_PX,
            maxHeight = MAX_PHOTO_SIDE_PX
        )
        val directory = FileKit.cacheDir / CACHE_DIRECTORY
        directory.createDirectories()
        val file = directory / "${Uuid.random()}.jpg"
        file.write(bytes)
        return CompressedPhoto(file.uploadUri(), bytes.size.toLong())
    }

    companion object {
        private const val MAX_PHOTO_SIDE_PX = 2048
        private const val PHOTO_JPEG_QUALITY = 80
        private const val CACHE_DIRECTORY = "compressed-photos"
    }
}