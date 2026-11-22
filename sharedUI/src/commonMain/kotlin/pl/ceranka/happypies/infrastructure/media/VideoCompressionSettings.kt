package pl.ceranka.happypies.infrastructure.media

private const val SIZE_BUDGET_RATIO = 0.92
private const val MIN_VIDEO_BITRATE = 150_000
private const val MAX_VIDEO_BITRATE = 2_500_000
private const val HD_MIN_VIDEO_BITRATE = 1_200_000
private const val HD_SHORT_SIDE = 720
private const val SD_SHORT_SIDE = 480
private const val AUDIO_BITRATE = 64_000
private const val AUDIO_CHANNELS = 1

data class VideoCompressionSettings(
    val videoBitrate: Int,
    val maxShortSide: Int,
    val audioBitrate: Int,
    val audioChannels: Int
)

fun videoCompressionSettings(durationMs: Long, maxSizeBytes: Long): VideoCompressionSettings {
    val seconds = (durationMs / 1000.0).coerceAtLeast(1.0)
    val budgetBits = maxSizeBytes * 8 * SIZE_BUDGET_RATIO
    val videoBitrate = (budgetBits / seconds - AUDIO_BITRATE).toInt().coerceIn(MIN_VIDEO_BITRATE, MAX_VIDEO_BITRATE)
    val maxShortSide = if (videoBitrate < HD_MIN_VIDEO_BITRATE) SD_SHORT_SIDE else HD_SHORT_SIDE
    return VideoCompressionSettings(videoBitrate, maxShortSide, AUDIO_BITRATE, AUDIO_CHANNELS)
}