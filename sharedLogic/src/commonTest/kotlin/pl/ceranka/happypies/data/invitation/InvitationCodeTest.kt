package pl.ceranka.happypies.data.invitation

import kotlin.test.Test
import kotlin.test.assertEquals

class InvitationCodeTest {

    @Test
    fun normalizesSpacesAndCase() {
        assertEquals("ABCD2345", InvitationCode.normalize(" abcd2345 "))
    }
}