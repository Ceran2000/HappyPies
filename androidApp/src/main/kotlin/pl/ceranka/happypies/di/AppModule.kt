package pl.ceranka.happypies.di

import org.koin.dsl.module
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.auth.AuthManagerImpl
import pl.ceranka.happypies.auth.AuthSessionStore
import pl.ceranka.happypies.data.client.ClientRepository
import pl.ceranka.happypies.data.client.ClientRepositoryImpl
import pl.ceranka.happypies.data.user.UserRepository
import pl.ceranka.happypies.data.user.UserRepositoryImpl

val appModule = module {
    single<AuthManager> { AuthManagerImpl(get(), get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }
    single<ClientRepository> { ClientRepositoryImpl(get()) }
    single { AuthSessionStore() }
}
