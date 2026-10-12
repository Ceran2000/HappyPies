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

private typealias F = UserFirestoreFields

class ClientRepositoryImplTest {

    private val gateway = FakeFirestoreGateway()
    private val repository = ClientRepositoryImpl(gateway)

    private val bala = MapValue(mapOf(F.DOG_NAME to StringValue("Bala"), F.DOG_BREED to StringValue("border collie")))

    private val validFields: Map<String, FirestoreValue> = mapOf(
        F.ROLE to StringValue("CLIENT"),
        F.TRAINER_ID to StringValue("trainer-1"),
        F.NAME to StringValue("Anna Kowalska"),
        F.EMAIL to StringValue("anna@przyklad.pl"),
        F.DOGS to ArrayValue(listOf(bala)),
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
                whereEqualTo = mapOf(F.TRAINER_ID to StringValue("trainer-1"), F.ROLE to StringValue("CLIENT")),
            ),
            gateway.queries.single(),
        )
    }

    @Test
    fun trainerIsNotAClient() = runTest {
        assertFailsWith<DataException.NotFound> { readClient(validFields + (F.ROLE to StringValue("TRAINER"))) }
    }

    @Test
    fun missingDogsIsMalformed() = runTest {
        assertFailsWith<DataException.MalformedData> { readClient(validFields - F.DOGS) }
    }

    @Test
    fun dogWithoutBreedIsMalformed() = runTest {
        val incomplete = MapValue(mapOf(F.DOG_NAME to StringValue("Rex")))
        assertFailsWith<DataException.MalformedData> { readClient(validFields + (F.DOGS to ArrayValue(listOf(incomplete)))) }
    }

    @Test
    fun missingClientIsNotFound() = runTest {
        assertFailsWith<DataException.NotFound> { repository.getClient("client-1") }
    }
}