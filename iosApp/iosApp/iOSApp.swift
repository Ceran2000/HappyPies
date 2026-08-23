import SwiftUI
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import SharedUI

@main
struct iOSApp: App {
    private let authManager: AuthManager
    private let userRepository: UserRepository

    init() {
        FirebaseApp.configure()
        authManager = AuthManagerImpl(auth: Auth.auth())
        userRepository = UserRepositoryImpl(firestore: Firestore.firestore())
    }

    var body: some Scene {
        WindowGroup {
            ComposeView(
                authManager: authManager,
                userRepository: userRepository
            )
                .ignoresSafeArea(.keyboard)
        }
    }
}
