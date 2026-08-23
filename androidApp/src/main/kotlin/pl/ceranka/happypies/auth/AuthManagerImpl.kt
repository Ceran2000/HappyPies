package pl.ceranka.happypies.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthManagerImpl(private val auth: FirebaseAuth) : AuthManager {

    override suspend fun signIn(email: String, password: String): AppUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return AppUser(result.user!!.uid, result.user!!.email)
    }

    override suspend fun signOut() {
        auth.signOut()
    }

}