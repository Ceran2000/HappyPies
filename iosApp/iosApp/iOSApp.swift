import SwiftUI
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import SharedUI

@main
struct iOSApp: App {
    private let authManager: AuthManager
    private let userRepository: UserRepository
    private let clientRepository: ClientRepository
    private let sessionStore: AuthSessionStore

    init() {
        FirebaseApp.configure()
        let firestore = Firestore.firestore()
        let session = AuthSessionStore()
        authManager = AuthManagerImpl(auth: Auth.auth(), sessionStore: session)
        userRepository = UserRepositoryImpl(firestore: firestore)
        clientRepository = ClientRepositoryImpl(firestore: firestore)
        sessionStore = session
    }

    var body: some Scene {
        WindowGroup {
            ComposeView(
                authManager: authManager,
                userRepository: userRepository,
                clientRepository: clientRepository,
                sessionStore: sessionStore
            )
                .ignoresSafeArea(.keyboard)
        }
    }
}
