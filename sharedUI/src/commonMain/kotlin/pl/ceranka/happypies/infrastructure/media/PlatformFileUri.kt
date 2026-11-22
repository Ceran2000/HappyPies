package pl.ceranka.happypies.infrastructure.media

import io.github.vinceglb.filekit.PlatformFile

expect fun PlatformFile.uploadUri(): String

expect fun platformFileFromUri(uri: String): PlatformFile