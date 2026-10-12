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
}
