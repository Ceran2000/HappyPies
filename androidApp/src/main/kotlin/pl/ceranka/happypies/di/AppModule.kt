package pl.ceranka.happypies.di

import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module
import pl.ceranka.happypies.firebase.auth.AuthManager
import pl.ceranka.happypies.firebase.auth.AuthManagerImpl
import pl.ceranka.happypies.firebase.firestore.AndroidFirestoreGateway
import pl.ceranka.happypies.firebase.firestore.FirestoreGateway
import pl.ceranka.happypies.firebase.functions.AndroidFunctionsGateway
import pl.ceranka.happypies.firebase.functions.FunctionsGateway
import pl.ceranka.happypies.firebase.storage.AndroidStorageGateway
import pl.ceranka.happypies.firebase.storage.StorageGateway
import pl.ceranka.happypies.infrastructure.media.AndroidVideoTranscoder
import pl.ceranka.happypies.infrastructure.media.VideoTranscoder

val appModule = module {
    single<AuthManager> { AuthManagerImpl(get()) }
    single<FirestoreGateway> { AndroidFirestoreGateway(get()) }
    single<FunctionsGateway> { AndroidFunctionsGateway(get()) }
    single<StorageGateway> { AndroidStorageGateway(get()) }
    single<VideoTranscoder> { AndroidVideoTranscoder(androidContext()) }
}