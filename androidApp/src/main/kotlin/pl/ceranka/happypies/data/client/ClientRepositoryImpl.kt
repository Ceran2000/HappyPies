package pl.ceranka.happypies.data.client

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class ClientRepositoryImpl(private val firestore: FirebaseFirestore) : ClientRepository {

    override suspend fun getClients(trainerId: String): List<Client> {
        val snapshot = firestore.collection("clients")
            .whereEqualTo(ClientFirestoreFields.TRAINER_ID, trainerId)
            .get()
            .await()
        return snapshot.documents.map { it.toClient() }
    }

    override suspend fun getClient(clientId: String): Client {
        val snapshot = firestore.collection("clients").document(clientId).get().await()
        return snapshot.toClient()
    }
}

private fun DocumentSnapshot.toClient(): Client {
    val name = getString(ClientFirestoreFields.NAME)
        ?: throw ClientMappingException("Missing or invalid field '${ClientFirestoreFields.NAME}' for client $id")
    val email = getString(ClientFirestoreFields.EMAIL)
        ?: throw ClientMappingException("Missing or invalid field '${ClientFirestoreFields.EMAIL}' for client $id")
    val dogsRaw = get(ClientFirestoreFields.DOGS) as? List<*>
        ?: throw ClientMappingException("Missing or invalid field '${ClientFirestoreFields.DOGS}' for client $id")
    val dogs = dogsRaw.map { dogRaw ->
        val dogMap = dogRaw as? Map<*, *>
            ?: throw ClientMappingException("Invalid dog entry for client $id")
        val dogName = dogMap[ClientFirestoreFields.DOG_NAME] as? String
            ?: throw ClientMappingException("Missing field '${ClientFirestoreFields.DOG_NAME}' in dog entry for client $id")
        val breed = dogMap[ClientFirestoreFields.DOG_BREED] as? String
            ?: throw ClientMappingException("Missing field '${ClientFirestoreFields.DOG_BREED}' in dog entry for client $id")
        Dog(name = dogName, breed = breed)
    }
    return Client(id = id, name = name, email = email, dogs = dogs)
}

class ClientMappingException(message: String) : Exception(message)