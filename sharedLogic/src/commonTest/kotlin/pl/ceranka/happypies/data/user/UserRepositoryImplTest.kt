package pl.ceranka.happypies.data.user

import kotlinx.coroutines.test.runTest
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.FakeFirestoreGateway
import pl.ceranka.happypies.data.firestore.FirestoreDocument
import pl.ceranka.happypies.data.firestore.FirestoreValue.StringValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserRepositoryImplTest {

    private val gateway = FakeFirestoreGateway()
    private val repository = UserRepositoryImpl(gateway)

    private fun userWithRole(role: String) =
        gateway.returnDocuments(FirestoreDocument("uid-1", mapOf(UserFirestoreFields.ROLE to StringValue(role))))

    @Test
    fun readsTheRole() = runTest {
        userWithRole("TRAINER")

        assertEquals(UserRole.TRAINER, repository.getRole("uid-1"))
        assertEquals("users" to "uid-1", gateway.gets.single())
    }

    @Test
    fun invalidRoleIsMalformed() = runTest {
        userWithRole("ADMIN")

        assertFailsWith<DataException.MalformedData> { repository.getRole("uid-1") }
    }

    @Test
    fun missingUserIsNotFound() = runTest {
        assertFailsWith<DataException.NotFound> { repository.getRole("uid-1") }
    }
}
