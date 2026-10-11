package pl.ceranka.happypies.data.client

import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.CollectionQuery
import pl.ceranka.happypies.data.firestore.DocumentReader
import pl.ceranka.happypies.data.firestore.FirestoreDocument
import pl.ceranka.happypies.data.firestore.FirestoreGateway
import pl.ceranka.happypies.data.firestore.FirestoreValue
import pl.ceranka.happypies.data.firestore.documentsOrThrow

private typealias Fields = ClientFirestoreFields

class ClientRepositoryImpl(private val gateway: FirestoreGateway) : ClientRepository {

    override suspend fun getClients(trainerId: String): List<Client> {
        val query = CollectionQuery(
            collection = Fields.COLLECTION,
            whereEqualTo = mapOf(Fields.TRAINER_ID to FirestoreValue.StringValue(trainerId)),
        )
        return gateway.query(query).documentsOrThrow().map { it.toClient() }
    }

    override suspend fun getClient(clientId: String): Client {
        val document = gateway.getDocument(Fields.COLLECTION, clientId).documentsOrThrow().firstOrNull()
            ?: throw DataException.NotFound("Client $clientId does not exist")
        return document.toClient()
    }
}

internal fun FirestoreDocument.toClient(): Client {
    val reader = DocumentReader(Fields.COLLECTION, this)
    return Client(
        id = id,
        name = reader.requireString(Fields.NAME),
        email = reader.requireString(Fields.EMAIL),
        dogs = reader.requireMapArray(Fields.DOGS).map { dog ->
            Dog(name = dog.requireString(Fields.DOG_NAME), breed = dog.requireString(Fields.DOG_BREED))
        },
    )
}
