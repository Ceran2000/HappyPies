package pl.ceranka.happypies.infrastructure.media

import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.cacheDir
import io.github.vinceglb.filekit.createDirectories
import io.github.vinceglb.filekit.delete
import io.github.vinceglb.filekit.div
import io.github.vinceglb.filekit.size
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.uuid.Uuid

class TranscoderVideoCompressor(private val transcoder: VideoTranscoder) : VideoCompressor {

    override suspend fun compress(uri: String, maxSizeBytes: Long, onProgress: (percent: Int) -> Unit): CompressedVideo {
        val directory = FileKit.cacheDir / CACHE_DIRECTORY
        directory.createDirectories()
        val file = directory / "${Uuid.random()}.mp4"
        try {
            transcoder.compress(uri, file.uploadUri(), maxSizeBytes, onProgress)
        } catch (e: Throwable) {
            withContext(NonCancellable) { file.delete(mustExist = false) }
            throw e
        }
        return CompressedVideo(file.uploadUri(), file.size())
    }

    companion object {
        private const val CACHE_DIRECTORY = "compressed-videos"
    }
}