package pl.ceranka.happypies.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthManagerImpl(private val auth: FirebaseAuth) : AuthManager {

    override suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun addUserListener(listener: AuthUserListener): ListenerHandle {
        val authStateListener = FirebaseAuth.AuthStateListener { listener.onAuthUserChanged(it.currentUser?.toAuthUser()) }
        auth.addAuthStateListener(authStateListener)
        return ListenerHandle { auth.removeAuthStateListener(authStateListener) }
    }
}

private fun FirebaseUser.toAuthUser() = AuthUser(uid, email)