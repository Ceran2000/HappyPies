package pl.ceranka.happypies.infrastructure.media

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate

suspend fun VideoTranscoder.compress(
    sourceUri: String,
    outputUri: String,
    maxSizeBytes: Long,
    onProgress: (percent: Int) -> Unit
) {
    val settings = videoCompressionSettings(readDurationOrThrow(sourceUri), maxSizeBytes)
    compressionFlow(sourceUri, outputUri, settings).collect(onProgress)
}

private fun VideoTranscoder.compressionFlow(sourceUri: String, outputUri: String, settings: VideoCompressionSettings): Flow<Int> =
    callbackFlow {
        val handle = startCompression(sourceUri, outputUri, settings) { event ->
            when (event) {
                is VideoCompressionEvent.Progress -> trySend(event.percent)
                is VideoCompressionEvent.Success -> close()
                is VideoCompressionEvent.Failure -> close(VideoCompressionException(event.message))
            }
        }
        awaitClose { handle.cancel() }
    }
        .conflate()

private suspend fun VideoTranscoder.readDurationOrThrow(sourceUri: String): Long =
    when (val result = readDuration(sourceUri)) {
        is VideoDurationResult.Success -> result.durationMs
        is VideoDurationResult.Failure -> throw VideoCompressionException(result.message)
    }