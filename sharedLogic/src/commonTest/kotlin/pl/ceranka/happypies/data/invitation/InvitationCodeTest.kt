package pl.ceranka.happypies.data.invitation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class InvitationCodeTest {

    @Test
    fun normalizesSpacesAndCase() {
        assertEquals("ABCD2345", InvitationCode.normalize(" abcd2345 "))
    }

    @Test
    fun readsTheCodeFromTheInvitationLinkPath() {
        assertEquals("ABCD2345", InvitationCode.fromLinkPath("/i/ABCD2345"))
    }

    @Test
    fun readsTheCodeIgnoringCaseAndTrailingSlash() {
        assertEquals("ABCD2345", InvitationCode.fromLinkPath("/i/abcd2345/"))
    }

    @Test
    fun rejectsAPathWithoutACode() {
        assertNull(InvitationCode.fromLinkPath("/i/"))
        assertNull(InvitationCode.fromLinkPath("/i/ABC"))
        assertNull(InvitationCode.fromLinkPath("/"))
        assertNull(InvitationCode.fromLinkPath(""))
    }

    @Test
    fun rejectsAnotherPath() {
        assertNull(InvitationCode.fromLinkPath("/other/i/ABCD2345"))
        assertNull(InvitationCode.fromLinkPath("/invite/ABCD2345"))
    }
}