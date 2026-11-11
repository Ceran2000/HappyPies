package pl.ceranka.happypies.di

import org.koin.dsl.module
import pl.ceranka.happypies.firebase.auth.AuthManager
import pl.ceranka.happypies.firebase.auth.AuthManagerImpl
import pl.ceranka.happypies.firebase.firestore.AndroidFirestoreGateway
import pl.ceranka.happypies.firebase.firestore.FirestoreGateway
import pl.ceranka.happypies.firebase.storage.AndroidStorageGateway
import pl.ceranka.happypies.firebase.storage.StorageGateway

val appModule = module {
    single<AuthManager> { AuthManagerImpl(get()) }
    single<FirestoreGateway> { AndroidFirestoreGateway(get()) }
    single<StorageGateway> { AndroidStorageGateway(get()) }
}