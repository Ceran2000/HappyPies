package pl.ceranka.happypies.ui.common

import pl.ceranka.happypies.data.DataException
import kotlin.test.Test
import kotlin.test.assertEquals

class ErrorMessagesTest {

    @Test
    fun unknownErrorsDoNotShowTechnicalText() {
        val missingIndex = DataException.Unknown(
            "FAILED_PRECONDITION: The query requires an index. You can create it here: https://console.firebase.google.com/..."
        )

        assertEquals("Coś poszło nie tak. Spróbuj ponownie później.", missingIndex.toUserMessage())
        assertEquals("Coś poszło nie tak. Spróbuj ponownie później.", IllegalStateException("boom").toUserMessage())
    }

    @Test
    fun knownDataErrorsHaveTheirOwnMessages() {
        assertEquals("Nie masz dostępu do tych danych.", DataException.PermissionDenied("denied").toUserMessage())
        assertEquals(
            "Brak połączenia z internetem. Sprawdź sieć i spróbuj ponownie.",
            DataException.Unavailable("offline").toUserMessage(),
        )
    }
}
