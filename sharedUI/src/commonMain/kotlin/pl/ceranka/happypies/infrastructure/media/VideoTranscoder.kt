package pl.ceranka.happypies.infrastructure.media

interface VideoTranscoder {

    @Throws(Exception::class)
    suspend fun readDuration(sourceUri: String): VideoDurationResult

    fun startCompression(
        sourceUri: String,
        outputUri: String,
        settings: VideoCompressionSettings,
        listener: VideoCompressionListener
    ): CompressionHandle
}