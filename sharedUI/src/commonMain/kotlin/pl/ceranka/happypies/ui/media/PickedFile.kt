package pl.ceranka.happypies.ui.media

import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.mimeType
import io.github.vinceglb.filekit.size
import pl.ceranka.happypies.data.report.MediaType
import pl.ceranka.happypies.infrastructure.media.uploadUri

data class PickedFile(val uri: String, val mimeType: String?, val sizeBytes: Long) {
    val mediaTypeOrNull: MediaType?
        get() = when {
            mimeType?.startsWith("image/") == true -> MediaType.PHOTO
            mimeType?.startsWith("video/") == true -> MediaType.VIDEO
            else -> null
        }
}

fun PlatformFile.toPickedFile(): PickedFile =
    PickedFile(uploadUri(), mimeType()?.let { "${it.primaryType}/${it.subtype}" }, size())