package pl.ceranka.happypies.ui.common

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatTest {

    private val october12At1840Utc = 1_791_830_400_000L

    @Test
    fun formatsDayMonthYearAndTime() {
        assertEquals("12.10.2026, 18:40", formatDateTime(october12At1840Utc, TimeZone.UTC))
    }

    @Test
    fun usesTheGivenTimeZone() {
        assertEquals("12.10.2026, 20:40", formatDateTime(october12At1840Utc, TimeZone.of("Europe/Warsaw")))
    }

    @Test
    fun padsSingleDigits() {
        assertEquals("01.01.1970, 00:00", formatDateTime(0, TimeZone.UTC))
    }

    @Test
    fun formatsDateWithoutTime() {
        assertEquals("12.10.2026", formatDate(october12At1840Utc, TimeZone.UTC))
    }

    @Test
    fun dateUsesTheGivenTimeZone() {
        val october12At2330Utc = october12At1840Utc + (4 * 60 + 50) * 60_000L
        assertEquals("13.10.2026", formatDate(october12At2330Utc, TimeZone.of("Europe/Warsaw")))
    }
}