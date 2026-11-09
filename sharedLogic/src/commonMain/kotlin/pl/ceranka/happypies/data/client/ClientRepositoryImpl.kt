package pl.ceranka.happypies.data.client

import pl.ceranka.happypies.core.DataException
import pl.ceranka.happypies.data.user.UserFirestoreFields
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.firebase.firestore.CollectionQuery
import pl.ceranka.happypies.firebase.firestore.DocumentReader
import pl.ceranka.happypies.firebase.firestore.FirestoreDocument
import pl.ceranka.happypies.firebase.firestore.FirestoreGateway
import pl.ceranka.happypies.firebase.firestore.FirestoreValue.StringValue
import pl.ceranka.happypies.firebase.firestore.documentOrThrow
import pl.ceranka.happypies.firebase.firestore.documentsOrThrow

private typealias Fields = UserFirestoreFields

class ClientRepositoryImpl(private val gateway: FirestoreGateway) : ClientRepository {

    override suspend fun getClients(trainerId: String): List<Client> {
        val query = CollectionQuery(
            collection = Fields.COLLECTION,
            whereEqualTo = mapOf(
                Fields.TRAINER_ID to StringValue(trainerId),
                Fields.ROLE to StringValue(UserRole.CLIENT.name),
            ),
        )
        return gateway.query(query).documentsOrThrow().map { it.toClient() }
    }

    override suspend fun getClient(clientId: String): Client {
        val document = gateway.getDocument(Fields.COLLECTION, clientId).documentOrThrow { "Client $clientId does not exist" }
        if (DocumentReader(Fields.COLLECTION, document).requireEnum(Fields.ROLE, UserRole.entries) != UserRole.CLIENT) {
            throw DataException.NotFound("User $clientId is not a client")
        }
        return document.toClient()
    }
}

internal fun FirestoreDocument.toClient(): Client {
    val reader = DocumentReader(Fields.COLLECTION, this)
    return Client(
        id = id,
        name = reader.requireString(Fields.NAME),
        email = reader.requireString(Fields.EMAIL),
        trainerId = reader.requireString(Fields.TRAINER_ID),
        dogs = reader.requireMapArray(Fields.DOGS).map { dog ->
            Dog(name = dog.requireString(Fields.DOG_NAME), breed = dog.requireString(Fields.DOG_BREED))
        },
    )
}