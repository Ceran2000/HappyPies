package pl.ceranka.happypies.infrastructure.media

import android.net.Uri
import io.github.vinceglb.filekit.AndroidFile
import io.github.vinceglb.filekit.PlatformFile

actual fun PlatformFile.uploadUri(): String = when (val file = androidFile) {
    is AndroidFile.UriWrapper -> file.uri.toString()
    is AndroidFile.FileWrapper -> Uri.fromFile(file.file).toString()
}

actual fun platformFileFromUri(uri: String): PlatformFile = PlatformFile(path = uri)