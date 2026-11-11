package pl.ceranka.happypies.ui.screen.add_edit_report

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.size
import pl.ceranka.happypies.data.report.MediaItem
import pl.ceranka.happypies.data.report.MediaType
import pl.ceranka.happypies.ui.media.uploadUri

const val MAX_REPORT_VIDEOS = 3

sealed interface ReportVideo {
    data class Uploaded(val item: MediaItem) : ReportVideo
    data class Picked(val localFileUri: String, val sizeBytes: Long) : ReportVideo
}

fun PlatformFile.toPickedVideo(): ReportVideo.Picked = ReportVideo.Picked(uploadUri(), size())

fun List<MediaItem>.toReportVideos(): List<ReportVideo> = filter { it.type == MediaType.VIDEO }.map { ReportVideo.Uploaded(it) }

fun List<ReportVideo>.withPicked(picked: List<ReportVideo.Picked>): List<ReportVideo> = (this + picked).take(MAX_REPORT_VIDEOS)

fun List<ReportVideo>.replace(picked: ReportVideo.Picked, uploaded: ReportVideo.Uploaded): List<ReportVideo> =
    map { if (it == picked) uploaded else it }

fun List<ReportVideo>.pickedVideos(): List<ReportVideo.Picked> = filterIsInstance<ReportVideo.Picked>()

fun List<ReportVideo>.uploadedItems(): List<MediaItem> = filterIsInstance<ReportVideo.Uploaded>().map { it.item }

fun List<ReportVideo>.firstSaveStatus(): SaveStatus = if (pickedVideos().isEmpty()) SaveStatus.Saving else SaveStatus.Uploading(0)