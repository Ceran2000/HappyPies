import FirebaseAuth
import SharedUI

final class AuthManagerImpl: AuthManager {

    private let auth: Auth

    init(auth: Auth) {
        self.auth = auth
    }

    func signIn(email: String, password: String) async throws {
        _ = try await auth.signIn(withEmail: email, password: password)
    }

    func signOut() async throws {
        try auth.signOut()
    }

    func addUserListener(listener: AuthUserListener) -> ListenerHandle {
        let handle = auth.addStateDidChangeListener { _, user in
            listener.onAuthUserChanged(user: user.map(toAuthUser))
        }
        return AuthListenerHandle(auth: auth, handle: handle)
    }
}

private final class AuthListenerHandle: ListenerHandle {

    private let auth: Auth
    private let handle: AuthStateDidChangeListenerHandle

    init(auth: Auth, handle: AuthStateDidChangeListenerHandle) {
        self.auth = auth
        self.handle = handle
    }

    func remove() {
        auth.removeStateDidChangeListener(handle)
    }
}

private func toAuthUser(_ user: User) -> AuthUser {
    AuthUser(uid: user.uid, email: user.email)
}