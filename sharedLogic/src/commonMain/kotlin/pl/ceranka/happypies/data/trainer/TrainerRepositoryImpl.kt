package pl.ceranka.happypies.data.trainer

import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.DocumentReader
import pl.ceranka.happypies.data.firestore.FirestoreGateway
import pl.ceranka.happypies.data.firestore.documentOrThrow
import pl.ceranka.happypies.data.user.UserFirestoreFields
import pl.ceranka.happypies.data.user.UserRole

private typealias Fields = UserFirestoreFields

class TrainerRepositoryImpl(private val gateway: FirestoreGateway) : TrainerRepository {

    override suspend fun getTrainer(trainerId: String): Trainer {
        val document = gateway.getDocument(Fields.COLLECTION, trainerId).documentOrThrow { "Trainer $trainerId does not exist" }
        val reader = DocumentReader(Fields.COLLECTION, document)
        if (reader.requireEnum(Fields.ROLE, UserRole.entries) != UserRole.TRAINER) {
            throw DataException.NotFound("User $trainerId is not a trainer")
        }
        return Trainer(id = document.id, name = reader.requireString(Fields.NAME))
    }
}