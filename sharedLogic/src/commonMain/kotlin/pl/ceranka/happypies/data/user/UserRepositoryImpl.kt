package pl.ceranka.happypies.data.user

import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.firestore.DocumentReader
import pl.ceranka.happypies.data.firestore.FirestoreGateway
import pl.ceranka.happypies.data.firestore.documentsOrThrow

class UserRepositoryImpl(private val gateway: FirestoreGateway) : UserRepository {

    override suspend fun getRole(uid: String): UserRole {
        val document = gateway.getDocument(UserFirestoreFields.COLLECTION, uid).documentsOrThrow().firstOrNull()
            ?: throw DataException.NotFound("User $uid does not exist")
        return DocumentReader(UserFirestoreFields.COLLECTION, document)
            .requireEnum(UserFirestoreFields.ROLE, UserRole.entries)
    }
}
