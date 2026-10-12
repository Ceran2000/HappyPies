package pl.ceranka.happypies.auth

import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.user.UserProfile
import pl.ceranka.happypies.data.user.UserRepository

sealed interface SessionState {
    data object Restoring : SessionState
    data object SignedOut : SessionState
    data class SignedIn(val profile: UserProfile) : SessionState
}

class SessionManager(
    private val authManager: AuthManager,
    private val userRepository: UserRepository,
) {
    private val _state = MutableStateFlow<SessionState>(SessionState.Restoring)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    private val restoreLock = Mutex()
    private val log = Logger.withTag("Session")

    suspend fun restore() = restoreLock.withLock {
        if (_state.value != SessionState.Restoring) return@withLock

        val user = authManager.currentUser
        if (user == null) {
            _state.value = SessionState.SignedOut
            return@withLock
        }
        _state.value = try {
            SessionState.SignedIn(userRepository.getProfile(user.uid))
        } catch (e: CancellationException) {
            throw e
        } catch (e: DataException) {
            log.w(e) { "Restoring the session failed: profile of ${user.uid} not loaded" }
            // No usable profile: drop the Auth session too. Network and other errors keep it,
            // so signing in by hand works once the problem is gone.
            if (e is DataException.NotFound || e is DataException.MalformedData || e is DataException.PermissionDenied) {
                signOutQuietly()
            }
            SessionState.SignedOut
        } catch (e: Exception) {
            log.w(e) { "Restoring the session failed" }
            SessionState.SignedOut
        }
    }

    /** Signs in and loads the profile; if the profile fails, signs out again and rethrows. */
    @Throws(Exception::class)
    suspend fun signIn(email: String, password: String) {
        val user = authManager.signIn(email, password)
        val profile = try {
            userRepository.getProfile(user.uid)
        } catch (e: Throwable) {
            withContext(NonCancellable) { signOutQuietly() }
            throw e
        }
        _state.value = SessionState.SignedIn(profile)
    }

    /** Always ends signed out: a failing Firebase sign-out does not keep the app signed in. */
    suspend fun signOut() {
        signOutQuietly()
        _state.value = SessionState.SignedOut
    }

    private suspend fun signOutQuietly() {
        try {
            authManager.signOut()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Best effort: the session state is what the app follows.
            log.w(e) { "Firebase sign-out failed" }
        }
    }
}

/** The signed-in profile, or null when signed out or still restoring. */
val SessionState.profileOrNull: UserProfile? get() = (this as? SessionState.SignedIn)?.profile