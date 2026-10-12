package pl.ceranka.happypies.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthManagerImpl(private val auth: FirebaseAuth) : AuthManager {

    override suspend fun signIn(email: String, password: String): AppUser {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user!!.toAppUser()
    }

    override suspend fun signOut() {
        auth.signOut()
    }

    override val currentUser: AppUser? get() = auth.currentUser?.toAppUser()
}

private fun FirebaseUser.toAppUser() = AppUser(uid, email)