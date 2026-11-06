package pl.ceranka.happypies.data.firestore

import pl.ceranka.happypies.data.DataException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class GatewayResultsTest {

    private val error = DataException.Unavailable("offline")
    private val document = FirestoreDocument("abc", emptyMap())

    @Test
    fun throwsTheGatewayErrorAsIs() {
        assertSame(error, assertFailsWith<DataException> { DocumentResult.Failure(error).documentOrThrow { "missing" } })
        assertSame(error, assertFailsWith<DataException> { ReadResult.Failure(error).documentsOrThrow() })
        assertSame(error, assertFailsWith<DataException> { WriteResult.Failure(error).documentIdOrThrow() })
    }

    @Test
    fun missingDocumentIsNotFound() {
        val exception = assertFailsWith<DataException.NotFound> { DocumentResult.Success(null).documentOrThrow { "missing" } }
        assertEquals("missing", exception.message)
    }

    @Test
    fun returnsTheSuccessValue() {
        assertEquals(document, DocumentResult.Success(document).documentOrThrow { "missing" })
        assertEquals(listOf(document), ReadResult.Success(listOf(document)).documentsOrThrow())
        assertEquals("abc", WriteResult.Success("abc").documentIdOrThrow())
    }
}