import SwiftUI
import FirebaseCore
import FirebaseAuth
import FirebaseFirestore
import FirebaseFunctions
import FirebaseStorage
import Shared

@main
struct iOSApp: App {
    init() {
        FirebaseApp.configure()
        KoinIosKt.setUpKoin(
            authManager: AuthManagerImpl(auth: Auth.auth()),
            firestoreGateway: IosFirestoreGateway(firestore: Firestore.firestore()),
            functionsGateway: IosFunctionsGateway(functions: Functions.functions(region: "europe-west1")),
            storageGateway: IosStorageGateway(storage: Storage.storage()),
            videoTranscoder: IosVideoTranscoder()
        )
    }

    var body: some Scene {
        WindowGroup {
            ComposeView()
                .ignoresSafeArea()
        }
    }
}