package pl.ceranka.happypies.infrastructure.media

import io.github.vinceglb.filekit.PlatformFile
import platform.Foundation.NSURL

actual fun PlatformFile.uploadUri(): String = nsUrl.absoluteString ?: error("File URL has no absolute string: $nsUrl")

actual fun platformFileFromUri(uri: String): PlatformFile = PlatformFile(NSURL(string = uri))