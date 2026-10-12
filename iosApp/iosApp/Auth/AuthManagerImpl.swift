import FirebaseAuth
import SharedUI

final class AuthManagerImpl: AuthManager {

    private let auth: Auth

    init(auth: Auth) {
        self.auth = auth
    }

    func signIn(email: String, password: String) async throws -> AppUser {
        let result = try await auth.signIn(withEmail: email, password: password)
        return toAppUser(result.user)
    }

    func signOut() async throws {
        try auth.signOut()
    }

    var currentUser: AppUser? {
        auth.currentUser.map(toAppUser)
    }
    
}

private func toAppUser(_ user: User) -> AppUser {
    AppUser(uid: user.uid, email: user.email)
}