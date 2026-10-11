import SwiftUI
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import SharedUI

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        let session = AuthSessionStore()
        let authManager = AuthManagerImpl(auth: Auth.auth(), sessionStore: session)
        let firestoreGateway = IosFirestoreGateway(firestore: Firestore.firestore())

        KoinIosKt.setUpKoin(
            authManager: authManager,
            firestoreGateway: firestoreGateway,
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
