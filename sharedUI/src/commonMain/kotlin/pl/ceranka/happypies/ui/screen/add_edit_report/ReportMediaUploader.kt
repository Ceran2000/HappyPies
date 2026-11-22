package pl.ceranka.happypies.ui.screen.add_edit_report

import pl.ceranka.happypies.data.media.MediaRepository
import pl.ceranka.happypies.data.report.MediaItem

class ReportMediaUploader(private val mediaRepository: MediaRepository) {

    suspend fun uploadCompressed(
        clientId: String,
        media: List<ReportMedia>,
        onProgress: (totalPercent: Int) -> Unit,
        onUploaded: (file: ReportMedia.Compressed, item: MediaItem) -> Unit
    ) {
        val picked = media.compressedMedia()
        val totalBytes = picked.sumOf { it.sizeBytes }
        var doneBytes = 0L
        for (file in picked) {
            val item = mediaRepository.upload(clientId, file.type, file.localFileUri, file.sizeBytes) { uploadedBytes ->
                onProgress(percentOf(doneBytes + uploadedBytes, totalBytes))
            }
            doneBytes += file.sizeBytes
            onUploaded(file, item)
        }
    }
}

private fun percentOf(bytes: Long, totalBytes: Long): Int =
    if (totalBytes == 0L) 100 else (bytes * 100 / totalBytes).toInt().coerceIn(0, 100)