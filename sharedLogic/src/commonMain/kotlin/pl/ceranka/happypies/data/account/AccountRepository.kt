package pl.ceranka.happypies.data.account

import kotlinx.coroutines.flow.StateFlow

interface AccountRepository {

    val state: StateFlow<AccountState>

    @Throws(Exception::class)
    suspend fun signIn(email: String, password: String)

    suspend fun signOut()

    fun retry()
}