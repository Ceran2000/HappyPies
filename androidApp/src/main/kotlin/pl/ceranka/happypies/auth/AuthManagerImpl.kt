package pl.ceranka.happypies.auth

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

class AuthManagerImpl(
    private val auth: FirebaseAuth,
    private val sessionStore: AuthSessionStore,
) : AuthManager {

    override suspend fun signIn(email: String, password: String): AppUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        val user = result.user!!.let { AppUser(it.uid, it.email) }
        sessionStore.onSignedIn(user)
        return user
    }

    override suspend fun signOut() {
        auth.signOut()
        sessionStore.onSignedOut()
    }

}