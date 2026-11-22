package pl.ceranka.happypies.infrastructure.media

import android.content.Context
import android.media.MediaMetadataRetriever
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.ChannelMixingAudioProcessor
import androidx.media3.common.audio.ChannelMixingMatrix
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.FrameDropEffect
import androidx.media3.effect.Presentation
import androidx.media3.transformer.AudioEncoderSettings
import androidx.media3.transformer.Composition
import androidx.media3.transformer.DefaultEncoderFactory
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.ProgressHolder
import androidx.media3.transformer.Transformer
import androidx.media3.transformer.VideoEncoderSettings
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@OptIn(UnstableApi::class)
class AndroidVideoTranscoder(private val context: Context) : VideoTranscoder {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override suspend fun readDuration(sourceUri: String): VideoDurationResult = withContext(Dispatchers.IO) {
        try {
            val durationMs = readMetadata(sourceUri, MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            if (durationMs == null) {
                VideoDurationResult.Failure("Missing duration of $sourceUri")
            } else {
                VideoDurationResult.Success(durationMs)
            }
        } catch (e: Exception) {
            VideoDurationResult.Failure(e.message)
        }
    }

    override fun startCompression(
        sourceUri: String,
        outputUri: String,
        settings: VideoCompressionSettings,
        listener: VideoCompressionListener
    ): CompressionHandle {
        val job = scope.launch {
            val event = try {
                val sourceShortSide = withContext(Dispatchers.IO) { sourceShortSideOrNull(sourceUri) }
                export(editedMediaItem(sourceUri, settings, sourceShortSide), outputPathOf(outputUri), settings) { percent ->
                    listener.onCompressionEvent(VideoCompressionEvent.Progress(percent))
                }
                VideoCompressionEvent.Success
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                VideoCompressionEvent.Failure(e.message)
            }
            listener.onCompressionEvent(event)
        }
        return CompressionHandle { job.cancel() }
    }

    private suspend fun export(
        item: EditedMediaItem,
        outputPath: String,
        settings: VideoCompressionSettings,
        onProgress: (percent: Int) -> Unit
    ) = coroutineScope {
        val result = CompletableDeferred<Unit>()
        val transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setEncoderFactory(encoderFactory(settings))
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, exportResult: ExportResult) {
                    result.complete(Unit)
                }

                override fun onError(composition: Composition, exportResult: ExportResult, exportException: ExportException) {
                    result.completeExceptionally(exportException)
                }
            })
            .build()
        transformer.start(item, outputPath)
        val progressPolling = launch { pollProgress(transformer, onProgress) }
        try {
            result.await()
        } catch (e: CancellationException) {
            transformer.cancel()
            throw e
        } finally {
            progressPolling.cancel()
        }
    }

    private suspend fun pollProgress(transformer: Transformer, onProgress: (percent: Int) -> Unit) {
        val progressHolder = ProgressHolder()
        while (true) {
            if (transformer.getProgress(progressHolder) == Transformer.PROGRESS_STATE_AVAILABLE) {
                onProgress(progressHolder.progress)
            }
            delay(PROGRESS_INTERVAL)
        }
    }

    private fun encoderFactory(settings: VideoCompressionSettings): DefaultEncoderFactory =
        DefaultEncoderFactory.Builder(context)
            .setRequestedVideoEncoderSettings(VideoEncoderSettings.Builder().setBitrate(settings.videoBitrate).build())
            .setRequestedAudioEncoderSettings(AudioEncoderSettings.Builder().setBitrate(settings.audioBitrate).build())
            .setEnableFallback(true)
            .build()

    private fun editedMediaItem(sourceUri: String, settings: VideoCompressionSettings, sourceShortSide: Int?): EditedMediaItem {
        val videoEffects = buildList {
            if (sourceShortSide == null || sourceShortSide > settings.maxShortSide) {
                add(Presentation.createForShortSide(settings.maxShortSide))
            }
            add(FrameDropEffect.createDefaultFrameDropEffect(MAX_FRAME_RATE))
        }
        val channelMixing = ChannelMixingAudioProcessor().apply {
            (1..2).forEach { inputChannels ->
                putChannelMixingMatrix(ChannelMixingMatrix.createForConstantGain(inputChannels, settings.audioChannels))
            }
        }
        return EditedMediaItem.Builder(MediaItem.fromUri(sourceUri))
            .setEffects(Effects(listOf(channelMixing), videoEffects))
            .build()
    }

    private fun sourceShortSideOrNull(sourceUri: String): Int? = try {
        val width = readMetadata(sourceUri, MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
        val height = readMetadata(sourceUri, MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
        if (width != null && height != null) minOf(width, height) else null
    } catch (e: Exception) {
        null
    }

    private fun readMetadata(sourceUri: String, key: Int): String? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, sourceUri.toUri())
            retriever.extractMetadata(key)
        } finally {
            retriever.release()
        }
    }

    private fun outputPathOf(outputUri: String): String = outputUri.toUri().path ?: outputUri

    companion object {
        private val PROGRESS_INTERVAL = 250.milliseconds
        private const val MAX_FRAME_RATE = 30f
    }
}