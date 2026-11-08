package pl.ceranka.happypies.ui.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GreetingTest {

    @Test
    fun greetsByFirstName() {
        assertEquals("Cześć, maria", greetingFor("  maria anna nowak "))
    }

    @Test
    fun blankNameHasNoGreeting() {
        assertNull(greetingFor("  "))
    }
}