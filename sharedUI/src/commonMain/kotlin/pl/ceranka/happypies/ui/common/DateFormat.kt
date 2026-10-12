package pl.ceranka.happypies.ui.common

import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** E.g. "12.10.2026, 18:40" in the given time zone (the device's by default). */
fun formatDateTime(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val dateTime = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)
    fun two(value: Int) = value.toString().padStart(2, '0')
    return "${two(dateTime.day)}.${two(dateTime.month.number)}.${dateTime.year}, ${two(dateTime.hour)}:${two(dateTime.minute)}"
}