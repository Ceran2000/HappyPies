package pl.ceranka.happypies.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class AuthSessionStore {

    private val _currentUser = MutableStateFlow<AppUser?>(null)
    val currentUser: StateFlow<AppUser?> = _currentUser.asStateFlow()

    fun onSignedIn(user: AppUser) {
        _currentUser.value = user
    }

    fun onSignedOut() {
        _currentUser.value = null
    }
}