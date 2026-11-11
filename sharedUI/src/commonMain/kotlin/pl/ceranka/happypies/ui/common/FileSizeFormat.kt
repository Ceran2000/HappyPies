package pl.ceranka.happypies.ui.common

import kotlin.math.roundToLong

fun formatMegabytes(bytes: Long): String {
    val tenths = (bytes / 100_000.0).roundToLong()
    return "${tenths / 10},${tenths % 10} MB"
}