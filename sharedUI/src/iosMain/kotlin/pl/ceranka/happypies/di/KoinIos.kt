package pl.ceranka.happypies.di

import org.koin.core.context.startKoin
import org.koin.dsl.module
import pl.ceranka.happypies.firebase.auth.AuthManager
import pl.ceranka.happypies.firebase.firestore.FirestoreGateway
import pl.ceranka.happypies.firebase.functions.FunctionsGateway
import pl.ceranka.happypies.firebase.storage.StorageGateway
import pl.ceranka.happypies.infrastructure.media.VideoTranscoder

fun setUpKoin(
    authManager: AuthManager,
    firestoreGateway: FirestoreGateway,
    functionsGateway: FunctionsGateway,
    storageGateway: StorageGateway,
    videoTranscoder: VideoTranscoder
) {
    startKoin {
        modules(
            module {
                single<AuthManager> { authManager }
                single<FirestoreGateway> { firestoreGateway }
                single<FunctionsGateway> { functionsGateway }
                single<StorageGateway> { storageGateway }
                single<VideoTranscoder> { videoTranscoder }
            },
            dataModule,
            viewModelModule
        )
    }
}