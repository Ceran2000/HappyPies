package pl.ceranka.happypies.ui.common

import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.error_permission_denied
import happypies.sharedui.generated.resources.error_unavailable
import happypies.sharedui.generated.resources.error_unknown
import pl.ceranka.happypies.data.DataException
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorMessagesTest {

    @Test
    fun unknownErrorsDoNotShowTechnicalText() {
        val missingIndex = DataException.Unknown(
            "FAILED_PRECONDITION: The query requires an index. You can create it here: https://console.firebase.google.com/..."
        )

        assertEquals(Res.string.error_unknown, missingIndex.toUserMessage())
        assertEquals(Res.string.error_unknown, IllegalStateException("boom").toUserMessage())
    }

    @Test
    fun knownDataErrorsHaveTheirOwnMessages() {
        assertEquals(Res.string.error_permission_denied, DataException.PermissionDenied("denied").toUserMessage())
        assertEquals(Res.string.error_unavailable, DataException.Unavailable("offline").toUserMessage())
    }
}