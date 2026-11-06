import FirebaseAuth
import SharedUI

final class AuthManagerImpl: AuthManager {

    private let auth: Auth

    init(auth: Auth) {
        self.auth = auth
    }

    var authUser: AuthUser? {
        auth.currentUser.flatMap(toAuthUser)
    }

    func signIn(email: String, password: String) async throws {
        _ = try await auth.signIn(withEmail: email, password: password)
    }

    func signOut() async throws {
        try auth.signOut()
    }

    func addAuthStateListener(listener: AuthStateListener) -> ListenerHandle {
        let handle = auth.addStateDidChangeListener { _, _ in
            listener.onAuthStateChanged()
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

private func toAuthUser(_ user: User) -> AuthUser? {
    guard let email = user.email else { return nil }
    return AuthUser(uid: user.uid, email: email)
}