package pl.ceranka.happypies.data.client

import kotlinx.coroutines.test.runTest
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.CollectionQuery
import pl.ceranka.happypies.data.firestore.FakeFirestoreGateway
import pl.ceranka.happypies.data.firestore.FirestoreDocument
import pl.ceranka.happypies.data.firestore.FirestoreValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.ArrayValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.MapValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.StringValue
import pl.ceranka.happypies.data.user.UserFirestoreFields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ClientRepositoryImplTest {

    private val gateway = FakeFirestoreGateway()
    private val repository = ClientRepositoryImpl(gateway)

    private val bala = MapValue(mapOf(UserFirestoreFields.DOG_NAME to StringValue("Bala"), UserFirestoreFields.DOG_BREED to StringValue("border collie")))

    private val validFields: Map<String, FirestoreValue> = mapOf(
        UserFirestoreFields.ROLE to StringValue("CLIENT"),
        UserFirestoreFields.TRAINER_ID to StringValue("trainer-1"),
        UserFirestoreFields.NAME to StringValue("Anna Kowalska"),
        UserFirestoreFields.EMAIL to StringValue("anna@przyklad.pl"),
        UserFirestoreFields.DOGS to ArrayValue(listOf(bala)),
    )

    private suspend fun readClient(fields: Map<String, FirestoreValue>): Client {
        gateway.returnDocuments(FirestoreDocument("client-1", fields))
        return repository.getClient("client-1")
    }

    @Test
    fun mapsAClientWithDogs() = runTest {
        assertEquals(
            Client("client-1", "Anna Kowalska", "anna@przyklad.pl", listOf(Dog("Bala", "border collie"))),
            readClient(validFields),
        )
        assertEquals("users" to "client-1", gateway.gets.single())
    }

    @Test
    fun queriesClientUsersOfTheTrainer() = runTest {
        repository.getClients("trainer-1")

        assertEquals(
            CollectionQuery(
                collection = "users",
                whereEqualTo = mapOf(UserFirestoreFields.TRAINER_ID to StringValue("trainer-1"), UserFirestoreFields.ROLE to StringValue("CLIENT")),
            ),
            gateway.queries.single(),
        )
    }

    @Test
    fun trainerIsNotAClient() = runTest {
        assertFailsWith<DataException.NotFound> { readClient(validFields + (UserFirestoreFields.ROLE to StringValue("TRAINER"))) }
    }

    @Test
    fun missingDogsIsMalformed() = runTest {
        assertFailsWith<DataException.MalformedData> { readClient(validFields - UserFirestoreFields.DOGS) }
    }

    @Test
    fun dogWithoutBreedIsMalformed() = runTest {
        val incomplete = MapValue(mapOf(UserFirestoreFields.DOG_NAME to StringValue("Rex")))
        assertFailsWith<DataException.MalformedData> { readClient(validFields + (UserFirestoreFields.DOGS to ArrayValue(listOf(incomplete)))) }
    }

    @Test
    fun missingClientIsNotFound() = runTest {
        assertFailsWith<DataException.NotFound> { repository.getClient("client-1") }
    }
}