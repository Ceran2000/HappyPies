import FirebaseAuth
import SharedUI

final class AuthManagerImpl: AuthManager {

    private let auth: Auth

    init(auth: Auth) {
        self.auth = auth
    }

    func signIn(email: String, password: String) async throws -> AppUser {
        let result = try await auth.signIn(withEmail: email, password: password)
        return AppUser(uid: result.user.uid, email: result.user.email)
    }

    func signOut() async throws {
        try auth.signOut()
    }
}
