package pl.ceranka.happypies.ui.screen.add_edit_report

import pl.ceranka.happypies.data.media.MediaRepository

suspend fun MediaRepository.uploadPickedVideos(
    clientId: String,
    videos: List<ReportVideo>,
    onProgress: (percent: Int) -> Unit,
    onUploaded: (picked: ReportVideo.Picked, uploaded: ReportVideo.Uploaded) -> Unit
) {
    val picked = videos.pickedVideos()
    val totalBytes = picked.sumOf { it.sizeBytes }
    var doneBytes = 0L
    for (video in picked) {
        val item = uploadVideo(clientId, video.localFileUri, video.sizeBytes) { uploadedBytes ->
            onProgress(percentOf(doneBytes + uploadedBytes, totalBytes))
        }
        doneBytes += video.sizeBytes
        onUploaded(video, ReportVideo.Uploaded(item))
    }
}

private fun percentOf(bytes: Long, totalBytes: Long): Int =
    if (totalBytes == 0L) 100 else (bytes * 100 / totalBytes).toInt().coerceIn(0, 100)