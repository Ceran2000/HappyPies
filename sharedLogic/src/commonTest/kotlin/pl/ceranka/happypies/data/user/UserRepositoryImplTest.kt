package pl.ceranka.happypies.data.user

import kotlinx.coroutines.test.runTest
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.FakeFirestoreGateway
import pl.ceranka.happypies.data.firestore.FirestoreDocument
import pl.ceranka.happypies.data.firestore.FirestoreValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.StringValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

private typealias F = UserFirestoreFields

class UserRepositoryImplTest {

    private val gateway = FakeFirestoreGateway()
    private val repository = UserRepositoryImpl(gateway)

    private val trainerFields: Map<String, FirestoreValue> = mapOf(
        F.ROLE to StringValue("TRAINER"),
        F.NAME to StringValue("Jan Trener"),
        F.EMAIL to StringValue("jan@przyklad.pl"),
    )

    private fun userWith(fields: Map<String, FirestoreValue>) =
        gateway.returnDocuments(FirestoreDocument("uid-1", fields))

    @Test
    fun readsTheProfile() = runTest {
        userWith(trainerFields)

        assertEquals(UserProfile("uid-1", "Jan Trener", "jan@przyklad.pl", UserRole.TRAINER), repository.getProfile("uid-1"))
        assertEquals("users" to "uid-1", gateway.gets.single())
    }

    @Test
    fun missingNameIsMalformed() = runTest {
        userWith(trainerFields - F.NAME)

        assertFailsWith<DataException.MalformedData> { repository.getProfile("uid-1") }
    }

    @Test
    fun invalidRoleIsMalformed() = runTest {
        userWith(trainerFields + (F.ROLE to StringValue("ADMIN")))

        assertFailsWith<DataException.MalformedData> { repository.getProfile("uid-1") }
    }

    @Test
    fun missingUserIsNotFound() = runTest {
        assertFailsWith<DataException.NotFound> { repository.getProfile("uid-1") }
    }
}