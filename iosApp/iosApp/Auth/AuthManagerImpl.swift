import FirebaseAuth
import SharedUI

final class AuthManagerImpl: AuthManager {

    private let auth: Auth
    private let sessionStore: AuthSessionStore

    init(auth: Auth, sessionStore: AuthSessionStore) {
        self.auth = auth
        self.sessionStore = sessionStore
    }

    func signIn(email: String, password: String) async throws -> AppUser {
        let result = try await auth.signIn(withEmail: email, password: password)
        let user = AppUser(uid: result.user.uid, email: result.user.email)
        sessionStore.onSignedIn(user: user)
        return user
    }

    func signOut() async throws {
        try auth.signOut()
        sessionStore.onSignedOut()
    }
}
