package pl.ceranka.happypies.ui.media

import io.github.vinceglb.filekit.PlatformFile

actual fun PlatformFile.uploadUri(): String = nsUrl.absoluteString ?: error("File URL has no absolute string: $nsUrl")