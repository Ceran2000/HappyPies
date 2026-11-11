import SwiftUI
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import FirebaseStorage
import Shared

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        KoinIosKt.setUpKoin(
            authManager: AuthManagerImpl(auth: Auth.auth()),
            firestoreGateway: IosFirestoreGateway(firestore: Firestore.firestore()),
            storageGateway: IosStorageGateway(storage: Storage.storage())
        )
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea(.keyboard)
        }
    }
}
