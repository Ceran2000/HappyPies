import SwiftUI
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import SharedUI

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        let firestore = Firestore.firestore()
        let session = AuthSessionStore()
        let authManager = AuthManagerImpl(auth: Auth.auth(), sessionStore: session)
        let userRepository = UserRepositoryImpl(firestore: firestore)
        let clientRepository = ClientRepositoryImpl(firestore: firestore)
        
        KoinIosKt.setUpKoin(
            authManager: authManager,
            userRepository: userRepository,
            clientRepository: clientRepository,
            sessionStore: session
        )
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.keyboard)
        }
    }
}
