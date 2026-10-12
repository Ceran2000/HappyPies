package pl.ceranka.happypies.navigation

import androidx.navigation3.runtime.NavKey
import pl.ceranka.happypies.auth.SessionState
import pl.ceranka.happypies.data.user.UserRole

/**
 * The back stack the session requires, or null when the current one is fine:
 * signed out → only Login; signed in on Login (or nothing) → the role's start screen.
 * A signed-in stack restored after process death is kept.
 */
internal fun requiredBackStack(session: SessionState, backStack: List<NavKey>): List<NavKey>? = when (session) {
    SessionState.Restoring -> null
    SessionState.SignedOut -> if (backStack.toList() == listOf(Route.Login)) null else listOf(Route.Login)
    is SessionState.SignedIn -> if (backStack.lastOrNull() in listOf(null, Route.Login)) {
        listOf(
            when (session.profile.role) {
                UserRole.TRAINER -> Route.ClientList
                UserRole.CLIENT -> Route.ClientHome
            }
        )
    } else {
        null
    }
}