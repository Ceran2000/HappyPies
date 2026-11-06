package pl.ceranka.happypies.di

import org.koin.dsl.module
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.auth.AuthManagerImpl
import pl.ceranka.happypies.data.firestore.AndroidFirestoreGateway
import pl.ceranka.happypies.data.firestore.FirestoreGateway

val appModule = module {
    single<AuthManager> { AuthManagerImpl(get()) }
    single<FirestoreGateway> { AndroidFirestoreGateway(get()) }
}
