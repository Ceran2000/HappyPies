package pl.ceranka.happypies.navigation

import androidx.navigation3.runtime.NavKey
import pl.ceranka.happypies.data.account.AccountState
import pl.ceranka.happypies.data.user.UserRole

/**
 * The back stack the account requires, or null when the current one is fine:
 * signed out → only Login; signed in on Login (or nothing) → the role's start screen.
 * A stack restored after process death is kept.
 */
internal fun requiredBackStack(state: AccountState, backStack: List<NavKey>): List<NavKey>? = when (state) {
    AccountState.Loading, is AccountState.Unavailable -> null
    is AccountState.SignedOut -> if (backStack.toList() == listOf(Route.Login)) null else listOf(Route.Login)
    is AccountState.SignedIn -> if (backStack.lastOrNull() in listOf(null, Route.Login)) {
        listOf(
            when (state.account.role) {
                UserRole.TRAINER -> Route.ClientList
                UserRole.CLIENT -> Route.ClientHome(state.account.uid)
            }
        )
    } else {
        null
    }
}