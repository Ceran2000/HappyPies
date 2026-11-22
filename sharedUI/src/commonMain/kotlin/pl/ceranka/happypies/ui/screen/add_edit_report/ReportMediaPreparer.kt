package pl.ceranka.happypies.ui.screen.add_edit_report

import co.touchlab.kermit.Logger
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.report_form_media_too_large
import happypies.sharedui.generated.resources.report_form_media_unreadable
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.data.report.MediaType
import pl.ceranka.happypies.infrastructure.media.PhotoCompressor
import pl.ceranka.happypies.infrastructure.media.VideoCompressor

private val log = Logger.withTag("ReportMediaPreparer")

sealed interface PreparedMedia {
    data class Ready(val media: ReportMedia.Compressed) : PreparedMedia
    data object Unreadable : PreparedMedia
    data object TooLarge : PreparedMedia
}

class ReportMediaPreparer(private val photoCompressor: PhotoCompressor, private val videoCompressor: VideoCompressor) {

    suspend fun prepare(
        media: List<ReportMedia.Compressing>,
        onProgress: (file: ReportMedia.Compressing, percent: Int) -> Unit,
        onPrepared: (file: ReportMedia.Compressing, result: PreparedMedia) -> Unit
    ): List<PreparedMedia> = media.map { file ->
        prepare(file) { percent -> onProgress(file, percent) }.also { onPrepared(file, it) }
    }

    private suspend fun prepare(file: ReportMedia.Compressing, onProgress: (percent: Int) -> Unit): PreparedMedia =
        try {
            when (file.type) {
                MediaType.PHOTO -> photoCompressor.compress(file.sourceUri).let { readyOrTooLarge(file, it.uri, it.sizeBytes) }
                MediaType.VIDEO -> videoCompressor.compress(file.sourceUri, file.type.maxReportSizeBytes(), onProgress)
                    .let { readyOrTooLarge(file, it.uri, it.sizeBytes) }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            log.e(e) { "Preparing ${file.sourceUri} failed" }
            PreparedMedia.Unreadable
        }

    private fun readyOrTooLarge(file: ReportMedia.Compressing, uri: String, sizeBytes: Long): PreparedMedia =
        if (sizeBytes > file.type.maxReportSizeBytes()) {
            PreparedMedia.TooLarge
        } else {
            PreparedMedia.Ready(ReportMedia.Compressed(file.sourceUri, uri, sizeBytes, file.type))
        }
}

fun pickErrorMessage(prepared: List<PreparedMedia>): StringResource? = when {
    PreparedMedia.Unreadable in prepared -> Res.string.report_form_media_unreadable
    PreparedMedia.TooLarge in prepared -> Res.string.report_form_media_too_large
    else -> null
}