package pl.ceranka.happypies.ui.screen.add_edit_report

import pl.ceranka.happypies.data.report.MediaItem
import pl.ceranka.happypies.data.report.MediaType

const val MAX_REPORT_PHOTOS = 5
const val MAX_REPORT_VIDEOS = 3

sealed interface ReportMedia {
    val type: MediaType

    data class Uploaded(val item: MediaItem, val previewUri: String) : ReportMedia {
        override val type: MediaType get() = item.type
    }

    data class Picked(
        val localFileUri: String,
        val sizeBytes: Long,
        override val type: MediaType,
        val uploadStatus: MediaUploadStatus
    ) : ReportMedia
}

fun List<MediaItem>.toReportMedia(): List<ReportMedia> = map { ReportMedia.Uploaded(it, previewUri = it.downloadUrl) }

fun List<ReportMedia>.withPicked(picked: List<ReportMedia.Picked>): List<ReportMedia> {
    val pickedUris = pickedMedia().map { it.localFileUri }.toSet()
    return this + picked.distinctBy { it.localFileUri }.filterNot { it.localFileUri in pickedUris }
}

fun List<ReportMedia>.countOf(type: MediaType): Int = count { it.type == type }

fun List<ReportMedia>.exceedsLimitOf(type: MediaType): Boolean = countOf(type) > maxCountOf(type)

fun List<ReportMedia>.exceedsLimits(): Boolean = MediaType.entries.any { exceedsLimitOf(it) }

fun List<ReportMedia>.freeSlotsOf(type: MediaType): Int = (maxCountOf(type) - countOf(type)).coerceAtLeast(0)

fun maxCountOf(type: MediaType): Int = when (type) {
    MediaType.PHOTO -> MAX_REPORT_PHOTOS
    MediaType.VIDEO -> MAX_REPORT_VIDEOS
}

fun List<ReportMedia>.withUploadsWaiting(): List<ReportMedia> = withPickedUploadStatus { MediaUploadStatus.Waiting }

fun List<ReportMedia>.withUploadsNotStarted(): List<ReportMedia> = withPickedUploadStatus { MediaUploadStatus.NotStarted }

fun List<ReportMedia>.withUploadProgress(localFileUri: String, percent: Int): List<ReportMedia> =
    withPickedUploadStatus { if (it.localFileUri == localFileUri) MediaUploadStatus.InProgress(percent) else it.uploadStatus }

fun List<ReportMedia>.withUploaded(localFileUri: String, item: MediaItem): List<ReportMedia> =
    map { if (it is ReportMedia.Picked && it.localFileUri == localFileUri) ReportMedia.Uploaded(item, previewUri = localFileUri) else it }

private fun List<ReportMedia>.withPickedUploadStatus(status: (ReportMedia.Picked) -> MediaUploadStatus): List<ReportMedia> =
    map { if (it is ReportMedia.Picked) it.copy(uploadStatus = status(it)) else it }

fun List<ReportMedia>.pickedMedia(): List<ReportMedia.Picked> = filterIsInstance<ReportMedia.Picked>()

fun List<ReportMedia>.uploadedItems(): List<MediaItem> = filterIsInstance<ReportMedia.Uploaded>().map { it.item }

fun List<ReportMedia>.firstSaveStatus(): SaveStatus =
    if (pickedMedia().isEmpty()) SaveStatus.Saving else SaveStatus.Uploading(totalPercent = 0)