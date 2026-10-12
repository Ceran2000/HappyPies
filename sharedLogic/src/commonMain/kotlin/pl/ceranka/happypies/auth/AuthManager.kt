package pl.ceranka.happypies.auth

import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

interface AuthManager {
    @Throws(Exception::class)
    suspend fun signIn(email: String, password: String)

    @Throws(Exception::class)
    suspend fun signOut()

    fun addUserListener(listener: AuthUserListener): ListenerHandle
}

data class AuthUser(val uid: String, val email: String?)

fun interface AuthUserListener {
    fun onAuthUserChanged(user: AuthUser?)
}

fun interface ListenerHandle {
    fun remove()
}

// An extension, not a member: Swift implements AuthManager and cannot implement a Flow.
val AuthManager.authUser: Flow<AuthUser?>
    get() = callbackFlow {
        val handle = addUserListener { trySend(it) }
        awaitClose { handle.remove() }
    }.distinctUntilChanged()