package pl.ceranka.happypies.ui.screen.add_edit_report

import pl.ceranka.happypies.data.report.MediaItem
import pl.ceranka.happypies.data.report.MediaType
import pl.ceranka.happypies.ui.media.PickedFile

private const val MAX_REPORT_PHOTOS = 5
private const val MAX_REPORT_VIDEOS = 3
private const val MAX_REPORT_PHOTO_BYTES = 5L * 1024 * 1024
private const val MAX_REPORT_VIDEO_BYTES = 35L * 1024 * 1024

sealed interface ReportMedia {
    val type: MediaType

    data class Compressing(val sourceUri: String, override val type: MediaType, val percent: Int) : ReportMedia {
        companion object {
            fun fromPickedFileOrNull(file: PickedFile): Compressing? {
                val type = file.mediaTypeOrNull ?: return null
                return if (file.sizeBytes < 0) null else Compressing(file.uri, type, percent = 0)
            }
        }
    }

    data class Compressed(val sourceUri: String, val localFileUri: String, val sizeBytes: Long, override val type: MediaType) : ReportMedia

    data class Uploaded(val item: MediaItem, val previewUri: String) : ReportMedia {
        override val type: MediaType get() = item.type
    }
}

fun List<MediaItem>.toReportMedia(): List<ReportMedia> = map { ReportMedia.Uploaded(it, previewUri = it.downloadUrl) }

fun List<PickedFile>.distinctFrom(mediaInForm: List<ReportMedia>): List<PickedFile> {
    val formMediaSourceUris = mediaInForm.mapNotNull { it.sourceUriOrNull() }.toSet()
    return distinctBy { it.uri }.filterNot { it.uri in formMediaSourceUris }
}

fun List<ReportMedia>.withCompressionProgress(sourceUri: String, percent: Int): List<ReportMedia> =
    replaceCompressing(sourceUri) { it.copy(percent = percent) }

fun List<ReportMedia>.withPreparationResult(sourceUri: String, result: PreparedMedia): List<ReportMedia> =
    replaceCompressing(sourceUri) {
        when (result) {
            is PreparedMedia.Ready -> result.media
            PreparedMedia.Unreadable, PreparedMedia.TooLarge -> null
        }
    }

fun List<ReportMedia>.isCompressing(): Boolean = any { it is ReportMedia.Compressing }

fun List<ReportMedia>.countOf(type: MediaType): Int = count { it.type == type }

fun List<ReportMedia>.exceedsLimitOf(type: MediaType): Boolean = countOf(type) > type.maxReportCount()

fun List<ReportMedia>.exceedsLimits(): Boolean = MediaType.entries.any { exceedsLimitOf(it) }

fun List<ReportMedia>.freeSlotsOf(type: MediaType): Int = (type.maxReportCount() - countOf(type)).coerceAtLeast(0)

fun MediaType.maxReportCount(): Int = when (this) {
    MediaType.PHOTO -> MAX_REPORT_PHOTOS
    MediaType.VIDEO -> MAX_REPORT_VIDEOS
}

fun MediaType.maxReportSizeBytes(): Long = when (this) {
    MediaType.PHOTO -> MAX_REPORT_PHOTO_BYTES
    MediaType.VIDEO -> MAX_REPORT_VIDEO_BYTES
}

fun List<ReportMedia>.saveStartStatus(): SaveStatus =
    if (compressedMedia().isEmpty()) SaveStatus.Saving else SaveStatus.Uploading(totalPercent = 0)

fun List<ReportMedia>.compressedMedia(): List<ReportMedia.Compressed> = filterIsInstance<ReportMedia.Compressed>()

fun List<ReportMedia>.withUploaded(sourceUri: String, item: MediaItem): List<ReportMedia> = map {
    if (it is ReportMedia.Compressed && it.sourceUri == sourceUri) ReportMedia.Uploaded(item, previewUri = it.localFileUri) else it
}

fun List<ReportMedia>.uploadedItems(): List<MediaItem> = filterIsInstance<ReportMedia.Uploaded>().map { it.item }

private fun List<ReportMedia>.replaceCompressing(
    sourceUri: String,
    replacement: (ReportMedia.Compressing) -> ReportMedia?
): List<ReportMedia> = mapNotNull {
    if (it is ReportMedia.Compressing && it.sourceUri == sourceUri) replacement(it) else it
}

private fun ReportMedia.sourceUriOrNull(): String? = when (this) {
    is ReportMedia.Compressing -> sourceUri
    is ReportMedia.Compressed -> sourceUri
    is ReportMedia.Uploaded -> null
}