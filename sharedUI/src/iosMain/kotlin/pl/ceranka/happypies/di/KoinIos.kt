package pl.ceranka.happypies.di

import org.koin.core.context.startKoin
import org.koin.dsl.module
import pl.ceranka.happypies.firebase.auth.AuthManager
import pl.ceranka.happypies.firebase.firestore.FirestoreGateway
import pl.ceranka.happypies.firebase.storage.StorageGateway

fun setUpKoin(
    authManager: AuthManager,
    firestoreGateway: FirestoreGateway,
    storageGateway: StorageGateway
) {
    startKoin {
        modules(
            module {
                single<AuthManager> { authManager }
                single<FirestoreGateway> { firestoreGateway }
                single<StorageGateway> { storageGateway }
            },
            dataModule,
            viewModelModule
        )
    }
}