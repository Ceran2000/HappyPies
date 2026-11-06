package pl.ceranka.happypies.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import pl.ceranka.happypies.core.ListenerHandle

class AuthManagerImpl(private val auth: FirebaseAuth) : AuthManager {

    override val authUser: AuthUser?
        get() = auth.currentUser?.toAuthUser()

    override suspend fun signIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password).await()
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override fun addAuthStateListener(listener: AuthStateListener): ListenerHandle {
        val authStateListener = FirebaseAuth.AuthStateListener { listener.onAuthStateChanged() }
        auth.addAuthStateListener(authStateListener)
        return ListenerHandle { auth.removeAuthStateListener(authStateListener) }
    }
}

private fun FirebaseUser.toAuthUser() = email?.let { AuthUser(uid, it) }