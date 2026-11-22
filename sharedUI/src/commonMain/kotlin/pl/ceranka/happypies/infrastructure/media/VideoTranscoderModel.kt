package pl.ceranka.happypies.infrastructure.media

sealed class VideoDurationResult {
    data class Success(val durationMs: Long) : VideoDurationResult()
    data class Failure(val message: String?) : VideoDurationResult()
}

fun interface VideoCompressionListener {
    fun onCompressionEvent(event: VideoCompressionEvent)
}

sealed class VideoCompressionEvent {
    data class Progress(val percent: Int) : VideoCompressionEvent()
    data object Success : VideoCompressionEvent()
    data class Failure(val message: String?) : VideoCompressionEvent()
}

fun interface CompressionHandle {
    fun cancel()
}

class VideoCompressionException(message: String?) : Exception(message)