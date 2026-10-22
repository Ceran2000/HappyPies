package pl.ceranka.happypies.auth

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import pl.ceranka.happypies.core.ListenerHandle

interface AuthManager {
    val authUser: AuthUser?

    @Throws(Exception::class)
    suspend fun signIn(email: String, password: String)

    @Throws(Exception::class)
    suspend fun signOut()

    fun addAuthStateListener(listener: AuthStateListener): ListenerHandle
}

data class AuthUser(val uid: String, val email: String)

fun interface AuthStateListener {
    fun onAuthStateChanged()
}

// An extension, not a member: Swift implements AuthManager and cannot implement a Flow.
val AuthManager.authUserFlow: Flow<AuthUser?>
    get() = callbackFlow {
        val handle = addAuthStateListener { trySend(authUser) }
        awaitClose { handle.remove() }
    }.distinctUntilChanged()