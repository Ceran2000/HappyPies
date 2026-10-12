package pl.ceranka.happypies.data.user

import pl.ceranka.happypies.data.firestore.DocumentReader
import pl.ceranka.happypies.data.firestore.FirestoreGateway
import pl.ceranka.happypies.data.firestore.documentOrThrow

private typealias Fields = UserFirestoreFields

class UserRepositoryImpl(private val gateway: FirestoreGateway) : UserRepository {

    override suspend fun getProfile(uid: String): UserProfile {
        val document = gateway.getDocument(Fields.COLLECTION, uid).documentOrThrow { "User $uid does not exist" }
        val reader = DocumentReader(Fields.COLLECTION, document)
        return UserProfile(
            uid = document.id,
            name = reader.requireString(Fields.NAME),
            email = reader.requireString(Fields.EMAIL),
            role = reader.requireEnum(Fields.ROLE, UserRole.entries),
        )
    }
}