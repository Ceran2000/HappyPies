package pl.ceranka.happypies.ui.common

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class FirstNameTest {

    @Test
    fun takesTheFirstWordOfTheName() {
        assertEquals("maria", firstNameOf("  maria anna nowak "))
    }

    @Test
    fun blankNameHasNoFirstName() {
        assertNull(firstNameOf("  "))
    }
}