package pl.ceranka.happypies.infrastructure.media

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VideoCompressionSettingsTest {

    private val maxSizeBytes = 35L * 1024 * 1024

    @Test
    fun shortVideoGetsTheMaximumBitrateInHd() {
        val settings = videoCompressionSettings(durationMs = 60_000, maxSizeBytes)

        assertEquals(VideoCompressionSettings(videoBitrate = 2_500_000, maxShortSide = 720, audioBitrate = 64_000, audioChannels = 1), settings)
    }

    @Test
    fun threeMinuteVideoStaysInHdWithALowerBitrate() {
        val settings = videoCompressionSettings(durationMs = 180_000, maxSizeBytes)

        assertEquals(720, settings.maxShortSide)
        assertTrue(settings.videoBitrate in 1_200_000..2_500_000)
    }

    @Test
    fun fiveMinuteVideoDropsTo480p() {
        val settings = videoCompressionSettings(durationMs = 300_000, maxSizeBytes)

        assertEquals(480, settings.maxShortSide)
    }

    @Test
    fun estimatedSizeFitsTheLimit() {
        listOf(30_000L, 180_000L, 300_000L, 900_000L).forEach { durationMs ->
            val settings = videoCompressionSettings(durationMs, maxSizeBytes)
            val estimatedBytes = (settings.videoBitrate + settings.audioBitrate) * (durationMs / 1000) / 8

            assertTrue(estimatedBytes <= maxSizeBytes, "$durationMs ms -> $estimatedBytes bytes")
        }
    }

    @Test
    fun veryLongVideoKeepsTheMinimumBitrate() {
        val settings = videoCompressionSettings(durationMs = 3_600_000, maxSizeBytes)

        assertEquals(150_000, settings.videoBitrate)
    }
}