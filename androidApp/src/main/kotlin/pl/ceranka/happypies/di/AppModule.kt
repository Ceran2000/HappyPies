package pl.ceranka.happypies.di

import org.koin.dsl.module
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.auth.AuthManagerImpl
import pl.ceranka.happypies.data.user.UserRepository
import pl.ceranka.happypies.data.user.UserRepositoryImpl

val appModule = module {
    single<AuthManager> { AuthManagerImpl(get()) }
    single<UserRepository> { UserRepositoryImpl(get()) }
}
