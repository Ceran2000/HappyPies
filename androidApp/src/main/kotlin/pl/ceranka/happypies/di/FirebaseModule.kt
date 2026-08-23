package pl.ceranka.happypies.di

import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.firestore
import org.koin.dsl.module

val firebaseModule = module {
    single { Firebase.auth }
    single { Firebase.firestore }
}
