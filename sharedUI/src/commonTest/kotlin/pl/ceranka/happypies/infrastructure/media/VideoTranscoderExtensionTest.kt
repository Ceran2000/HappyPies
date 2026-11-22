package pl.ceranka.happypies.infrastructure.media

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.ceranka.happypies.fakes.FakeVideoTranscoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class VideoTranscoderExtensionTest {

    private val transcoder = FakeVideoTranscoder()
    private val maxSizeBytes = 35L * 1024 * 1024

    @Test
    fun compressesWithSettingsForTheVideoDurationAndReportsProgress() = runTest {
        transcoder.duration = VideoDurationResult.Success(300_000)
        transcoder.events = listOf(VideoCompressionEvent.Progress(40), VideoCompressionEvent.Success)
        val progress = mutableListOf<Int>()

        transcoder.compress("file:///in.mov", "file:///out.mp4", maxSizeBytes) { progress += it }

        assertEquals(listOf(videoCompressionSettings(300_000, maxSizeBytes)), transcoder.started)
        assertEquals(listOf(40), progress)
    }

    @Test
    fun compressionFailureIsThrown() = runTest {
        transcoder.events = listOf(VideoCompressionEvent.Failure("encoder"))

        assertFailsWith<VideoCompressionException> {
            transcoder.compress("file:///in.mov", "file:///out.mp4", maxSizeBytes) {}
        }
    }

    @Test
    fun unreadableDurationIsThrownBeforeCompressionStarts() = runTest {
        transcoder.duration = VideoDurationResult.Failure("no track")

        assertFailsWith<VideoCompressionException> {
            transcoder.compress("file:///in.mov", "file:///out.mp4", maxSizeBytes) {}
        }
        assertTrue(transcoder.started.isEmpty())
    }

    @Test
    fun cancellingTheCoroutineCancelsTheCompression() = runTest(UnconfinedTestDispatcher()) {
        transcoder.events = emptyList()
        val job = launch { transcoder.compress("file:///in.mov", "file:///out.mp4", maxSizeBytes) {} }

        job.cancel()

        assertTrue(transcoder.cancelled)
    }
}