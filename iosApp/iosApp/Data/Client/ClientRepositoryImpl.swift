import FirebaseFirestore
import SharedUI

final class ClientRepositoryImpl: ClientRepository {

    private let firestore: Firestore

    init(firestore: Firestore) {
        self.firestore = firestore
    }

    func getClients(trainerId: String) async throws -> [Client] {
        let snapshot = try await firestore.collection("clients")
            .whereField(ClientFirestoreFields.shared.TRAINER_ID, isEqualTo: trainerId)
            .getDocuments()
        return try snapshot.documents.map { try parseClient($0) }
    }

    func getClient(clientId: String) async throws -> Client {
        let snapshot = try await firestore.collection("clients").document(clientId).getDocument()
        return try parseClient(snapshot)
    }

}

private func parseClient(_ doc: DocumentSnapshot) throws -> Client {
    let fields = ClientFirestoreFields.shared
    guard let data = doc.data() else {
        throw ClientMappingError.missingField("data", clientId: doc.documentID)
    }
    guard let name = data[fields.NAME] as? String else {
        throw ClientMappingError.missingField(fields.NAME, clientId: doc.documentID)
    }
    guard let email = data[fields.EMAIL] as? String else {
        throw ClientMappingError.missingField(fields.EMAIL, clientId: doc.documentID)
    }
    guard let dogsRaw = data[fields.DOGS] as? [[String: Any]] else {
        throw ClientMappingError.missingField(fields.DOGS, clientId: doc.documentID)
    }
    let dogs = try dogsRaw.map { dogMap -> Dog in
        guard let dogName = dogMap[fields.DOG_NAME] as? String else {
            throw ClientMappingError.missingField("dog.\(fields.DOG_NAME)", clientId: doc.documentID)
        }
        guard let breed = dogMap[fields.DOG_BREED] as? String else {
            throw ClientMappingError.missingField("dog.\(fields.DOG_BREED)", clientId: doc.documentID)
        }
        return Dog(name: dogName, breed: breed)
    }
    return Client(id: doc.documentID, name: name, email: email, dogs: dogs)
}

enum ClientMappingError: Error {
    case missingField(String, clientId: String)
}
