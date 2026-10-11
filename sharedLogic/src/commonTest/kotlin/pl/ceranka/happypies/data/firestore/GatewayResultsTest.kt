package pl.ceranka.happypies.data.firestore

import pl.ceranka.happypies.data.DataException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class GatewayResultsTest {

    private fun exceptionFor(kind: DataErrorKind) = GatewayError(kind, "boom").toDataException()

    @Test
    fun mapsEveryErrorKindToADataException() {
        assertIs<DataException.NotFound>(exceptionFor(DataErrorKind.NOT_FOUND))
        assertIs<DataException.PermissionDenied>(exceptionFor(DataErrorKind.PERMISSION_DENIED))
        assertIs<DataException.PermissionDenied>(exceptionFor(DataErrorKind.UNAUTHENTICATED))
        assertIs<DataException.Unavailable>(exceptionFor(DataErrorKind.UNAVAILABLE))
        assertIs<DataException.Unknown>(exceptionFor(DataErrorKind.FAILED_PRECONDITION))
        assertIs<DataException.Unknown>(exceptionFor(DataErrorKind.OTHER))
    }

    @Test
    fun keepsTheOriginalMessage() {
        assertEquals("boom", exceptionFor(DataErrorKind.FAILED_PRECONDITION).message)
    }
}
