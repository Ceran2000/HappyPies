package pl.ceranka.happypies.data.trainer

import kotlinx.coroutines.test.runTest
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.FakeFirestoreGateway
import pl.ceranka.happypies.data.firestore.FirestoreDocument
import pl.ceranka.happypies.data.firestore.FirestoreValue
import pl.ceranka.happypies.data.firestore.FirestoreValue.StringValue
import pl.ceranka.happypies.data.user.UserFirestoreFields
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TrainerRepositoryImplTest {

    private val gateway = FakeFirestoreGateway()
    private val repository = TrainerRepositoryImpl(gateway)

    private val validFields: Map<String, FirestoreValue> = mapOf(
        UserFirestoreFields.ROLE to StringValue("TRAINER"),
        UserFirestoreFields.NAME to StringValue("Jan Nowak"),
        UserFirestoreFields.EMAIL to StringValue("jan@przyklad.pl")
    )

    private suspend fun readTrainer(fields: Map<String, FirestoreValue>): Trainer {
        gateway.returnDocuments(FirestoreDocument("trainer-1", fields))
        return repository.getTrainer("trainer-1")
    }

    @Test
    fun mapsATrainer() = runTest {
        assertEquals(Trainer("trainer-1", "Jan Nowak"), readTrainer(validFields))
        assertEquals("users" to "trainer-1", gateway.gets.single())
    }

    @Test
    fun clientIsNotATrainer() = runTest {
        assertFailsWith<DataException.NotFound> { readTrainer(validFields + (UserFirestoreFields.ROLE to StringValue("CLIENT"))) }
    }

    @Test
    fun missingNameIsMalformed() = runTest {
        assertFailsWith<DataException.MalformedData> { readTrainer(validFields - UserFirestoreFields.NAME) }
    }

    @Test
    fun missingTrainerIsNotFound() = runTest {
        assertFailsWith<DataException.NotFound> { repository.getTrainer("trainer-1") }
    }
}