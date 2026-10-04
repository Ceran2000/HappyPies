package pl.ceranka.happypies.di

import org.koin.core.context.startKoin
import org.koin.dsl.module
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.auth.AuthSessionStore
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.user.UserRepository

fun setUpKoin(
    authManager: AuthManager,
    userRepository: UserRepository,
    clientRepository: ClientRepository,
    sessionStore: AuthSessionStore,
) {
    startKoin {
        modules(
            module {
                single<AuthManager> { authManager }
                single<UserRepository> { userRepository }
                single<ClientRepository> { clientRepository }
                single<AuthSessionStore> { sessionStore }
            },
            viewModelModule
        )
    }
}