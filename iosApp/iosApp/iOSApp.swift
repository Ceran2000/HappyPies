import SwiftUI
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import SharedUI

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        KoinIosKt.setUpKoin(
            authManager: AuthManagerImpl(auth: Auth.auth()),
            firestoreGateway: IosFirestoreGateway(firestore: Firestore.firestore())
        )
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.keyboard)
        }
    }
}
