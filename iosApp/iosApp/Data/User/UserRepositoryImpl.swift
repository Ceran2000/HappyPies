import FirebaseFirestore
import SharedUI

final class UserRepositoryImpl: UserRepository {
    
    private let firestore: Firestore
    
    init(firestore: Firestore) {
        self.firestore = firestore
    }
    
    func getRole(uid: String) async throws -> UserRole {
        let snapshot = try await firestore.collection("users").document(uid).getDocument()
        guard let rawRole = snapshot.data()?["role"] as? String else {
            throw UserMappingError.missingField("role", uid: uid)
        }
        guard let role = parseRole(rawRole) else {
            throw UserMappingError.invalidRole(rawRole, uid: uid)
        }
        return role
    }
    
    private func parseRole(_ raw: String) -> UserRole? {
        switch raw {
        case "TRAINER": return .trainer
        case "CLIENT": return .client
        default: return nil
        }
    }
}

enum UserMappingError: Error {
    case missingField(String, uid: String)
    case invalidRole(String, uid: String)
}
