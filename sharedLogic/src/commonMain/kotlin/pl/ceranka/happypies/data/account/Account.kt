package pl.ceranka.happypies.data.account

import pl.ceranka.happypies.data.user.UserRole

/** The signed-in person: [uid] and [email] from Firebase Auth, [name] and [role] from `users/{uid}`. */
data class Account(val uid: String, val email: String, val name: String, val role: UserRole)

sealed interface AccountState {

    data object Loading : AccountState

    data class SignedOut(val rejected: Boolean = false) : AccountState

    data class SignedIn(val account: Account) : AccountState

    /** Firebase has a user, but their account could not be loaded for now (e.g. offline). */
    data class Unavailable(val error: Exception) : AccountState
}

val AccountState.accountOrNull: Account? get() = (this as? AccountState.SignedIn)?.account