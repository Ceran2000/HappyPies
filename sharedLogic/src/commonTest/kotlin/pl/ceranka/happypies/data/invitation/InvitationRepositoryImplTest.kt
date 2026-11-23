package pl.ceranka.happypies.data.invitation

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.firebase.firestore.CollectionQuery
import pl.ceranka.happypies.firebase.firestore.FakeFirestoreGateway
import pl.ceranka.happypies.firebase.firestore.FirestoreDocument
import pl.ceranka.happypies.firebase.firestore.FirestoreValue
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.StringValue
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.TimestampValue
import pl.ceranka.happypies.firebase.functions.FakeFunctionsGateway
import pl.ceranka.happypies.firebase.functions.FunctionCallResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class InvitationRepositoryImplTest {

    private val firestore = FakeFirestoreGateway()
    private val functions = FakeFunctionsGateway()
    private val repository = InvitationRepositoryImpl(firestore, functions)

    private val validFields: Map<String, FirestoreValue> = mapOf(
        InvitationFirestoreFields.NAME to StringValue("Tomasz Nowak"),
        InvitationFirestoreFields.EMAIL to StringValue("tomasz@przyklad.pl"),
        InvitationFirestoreFields.TRAINER_ID to StringValue("trainer-1"),
        InvitationFirestoreFields.EXPIRES_AT to TimestampValue(1_000L)
    )

    @Test
    fun queriesInvitationsOfTheTrainer() = runTest {
        firestore.returnDocuments(FirestoreDocument("inv-1", validFields))

        val invitations = repository.getInvitations("trainer-1")

        assertEquals(listOf(Invitation("inv-1", "Tomasz Nowak", "tomasz@przyklad.pl", expiresAt = 1_000L)), invitations)
        assertEquals(
            CollectionQuery(collection = "invitations", whereEqualTo = mapOf(InvitationFirestoreFields.TRAINER_ID to StringValue("trainer-1"))),
            firestore.queries.single()
        )
    }

    @Test
    fun invitationWithoutExpiryIsMalformed() = runTest {
        firestore.returnDocuments(FirestoreDocument("inv-1", validFields - InvitationFirestoreFields.EXPIRES_AT))

        assertFailsWith<DataException.MalformedData> { repository.getInvitations("trainer-1") }
    }

    @Test
    fun invitationExpiresAtItsExpiryTime() {
        val invitation = Invitation("inv-1", "Tomasz Nowak", "tomasz@przyklad.pl", expiresAt = 1_000L)

        assertFalse(invitation.isExpired(nowMillis = 999L))
        assertTrue(invitation.isExpired(nowMillis = 1_000L))
    }

    @Test
    fun inviteCallsTheFunctionAndSignalsAChange() = runTest {
        val changes = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.changes.collect { changes += it } }

        repository.invite("Tomasz Nowak", "tomasz@przyklad.pl")

        assertEquals(FakeFunctionsGateway.Call("inviteClient", mapOf("name" to "Tomasz Nowak", "email" to "tomasz@przyklad.pl")), functions.calls.single())
        assertEquals(1, changes.size)
    }

    @Test
    fun resendCallsTheFunctionWithTheInvitationId() = runTest {
        repository.resend("inv-1")

        assertEquals(FakeFunctionsGateway.Call("resendInvitation", mapOf("invitationId" to "inv-1")), functions.calls.single())
    }

    @Test
    fun cancelDeletesTheInvitationAndSignalsAChange() = runTest {
        val changes = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.changes.collect { changes += it } }

        repository.cancel("inv-1")

        assertEquals(FakeFirestoreGateway.Write("delete", "invitations", "inv-1", null), firestore.writes.single())
        assertEquals(1, changes.size)
    }

    @Test
    fun knownRejectionsBecomeInvitationExceptions() = runTest {
        functions.result = FunctionCallResult.Rejected("account-exists")
        assertFailsWith<InvitationException.AccountExists> { repository.invite("Tomasz Nowak", "tomasz@przyklad.pl") }

        functions.result = FunctionCallResult.Rejected("invitation-exists")
        assertFailsWith<InvitationException.AlreadyInvited> { repository.invite("Tomasz Nowak", "tomasz@przyklad.pl") }

        functions.result = FunctionCallResult.Rejected("expired")
        assertFailsWith<InvitationException.Expired> { repository.getInvitation("K7P2QXHM") }
    }

    @Test
    fun unknownRejectionIsAnUnknownError() = runTest {
        functions.result = FunctionCallResult.Rejected("something-new")

        assertFailsWith<DataException.Unknown> { repository.accept("K7P2QXHM", "haslo1234") }
    }

    @Test
    fun failedCallThrowsTheGatewayErrorAndSignalsNoChange() = runTest {
        val changes = mutableListOf<Unit>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repository.changes.collect { changes += it } }
        val error = DataException.Unavailable("offline")
        functions.result = FunctionCallResult.Failure(error)

        assertSame(error, assertFailsWith<DataException.Unavailable> { repository.invite("Tomasz Nowak", "tomasz@przyklad.pl") })
        assertTrue(changes.isEmpty())
    }

    @Test
    fun mapsTheInvitationPreview() = runTest {
        functions.result = FunctionCallResult.Success(
            mapOf("name" to "Tomasz Nowak", "email" to "tomasz@przyklad.pl", "trainerName" to "Jan Trener", "expiresAtMillis" to "1764460800000")
        )

        val preview = repository.getInvitation("K7P2QXHM")

        assertEquals(InvitationPreview("Tomasz Nowak", "tomasz@przyklad.pl", "Jan Trener", expiresAt = 1_764_460_800_000L), preview)
        assertEquals(FakeFunctionsGateway.Call("getInvitation", mapOf("code" to "K7P2QXHM")), functions.calls.single())
    }

    @Test
    fun previewWithMissingOrInvalidFieldIsMalformed() = runTest {
        functions.result = FunctionCallResult.Success(mapOf("name" to "Tomasz Nowak", "email" to "tomasz@przyklad.pl", "expiresAtMillis" to "1"))
        assertFailsWith<DataException.MalformedData> { repository.getInvitation("K7P2QXHM") }

        functions.result = FunctionCallResult.Success(
            mapOf("name" to "Tomasz Nowak", "email" to "tomasz@przyklad.pl", "trainerName" to "Jan Trener", "expiresAtMillis" to "jutro")
        )
        assertFailsWith<DataException.MalformedData> { repository.getInvitation("K7P2QXHM") }
    }

    @Test
    fun acceptCallsTheFunctionWithCodeAndPassword() = runTest {
        repository.accept("K7P2QXHM", "haslo1234")

        assertEquals(FakeFunctionsGateway.Call("acceptInvitation", mapOf("code" to "K7P2QXHM", "password" to "haslo1234")), functions.calls.single())
    }
}