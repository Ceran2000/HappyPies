package pl.ceranka.happypies.ui.screen.add_edit_report

import co.touchlab.kermit.Logger
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.report_form_media_unreadable
import kotlinx.coroutines.CancellationException
import org.jetbrains.compose.resources.StringResource
import pl.ceranka.happypies.data.report.MediaType
import pl.ceranka.happypies.infrastructure.media.PhotoCompressor
import pl.ceranka.happypies.ui.media.PickedFile

private val log = Logger.withTag("ReportMediaPick")

suspend fun PhotoCompressor.preparedMediaOrNull(file: PickedFile): ReportMedia.Picked? {
    if (file.sizeBytes < 0) return null
    return when (file.mediaTypeOrNull) {
        MediaType.VIDEO -> ReportMedia.Picked(file.uri, file.sizeBytes, MediaType.VIDEO, MediaUploadStatus.NotStarted)
        MediaType.PHOTO -> compressedPhotoOrNull(file)
        null -> null
    }
}

private suspend fun PhotoCompressor.compressedPhotoOrNull(file: PickedFile): ReportMedia.Picked? =
    try {
        val photo = compress(file.uri)
        ReportMedia.Picked(photo.uri, photo.sizeBytes, MediaType.PHOTO, MediaUploadStatus.NotStarted)
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        log.e(e) { "Compressing photo ${file.uri} failed" }
        null
    }

fun pickErrorMessage(prepared: List<ReportMedia.Picked?>): StringResource? =
    if (prepared.any { it == null }) Res.string.report_form_media_unreadable else null