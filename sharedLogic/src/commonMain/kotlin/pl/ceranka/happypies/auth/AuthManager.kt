package pl.ceranka.happypies.auth

interface AuthManager {
    @Throws(Exception::class)
    suspend fun signIn(email: String, password: String): AppUser

    @Throws(Exception::class)
    suspend fun signOut()
}

data class AppUser(val uid: String, val email: String?)