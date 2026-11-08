package pl.ceranka.happypies.ui.common

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** E.g. "12.10.2026" in the given time zone (the device's by default). */
fun formatDate(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String =
    localDateTime(epochMillis, timeZone).formattedDate()

/** E.g. "12.10.2026, 18:40" in the given time zone (the device's by default). */
fun formatDateTime(epochMillis: Long, timeZone: TimeZone = TimeZone.currentSystemDefault()): String {
    val dateTime = localDateTime(epochMillis, timeZone)
    return "${dateTime.formattedDate()}, ${two(dateTime.hour)}:${two(dateTime.minute)}"
}

private fun localDateTime(epochMillis: Long, timeZone: TimeZone): LocalDateTime =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(timeZone)

private fun LocalDateTime.formattedDate(): String = "${two(day)}.${two(month.number)}.$year"

private fun two(value: Int): String = value.toString().padStart(2, '0')