package pl.ceranka.happypies.di

import org.koin.core.context.startKoin
import org.koin.dsl.module
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.auth.AuthSessionStore
import pl.ceranka.happypies.data.firestore.FirestoreGateway

fun setUpKoin(
    authManager: AuthManager,
    firestoreGateway: FirestoreGateway,
    sessionStore: AuthSessionStore,
) {
    startKoin {
        modules(
            module {
                single<AuthManager> { authManager }
                single<FirestoreGateway> { firestoreGateway }
                single<AuthSessionStore> { sessionStore }
            },
            dataModule,
            viewModelModule
        )
    }
}
