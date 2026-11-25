package pl.ceranka.happypies.navigation

import androidx.navigation3.runtime.NavKey
import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.account.AccountState
import pl.ceranka.happypies.data.user.UserRole

/**
 * The back stack the account requires, or null when the current one is fine:
 * signed out → Login, optionally with the invitation screens on top;
 * signed in on a signed-out screen (or nothing) → the role's start screen.
 * A stack restored after process death is kept.
 */
fun requiredBackStack(state: AccountState, backStack: List<NavKey>): List<NavKey>? = when (state) {
    is AccountState.Unavailable, AccountState.Loading -> null
    is AccountState.SignedOut -> backStack.loginBackStackIfNeeded()
    is AccountState.SignedIn -> backStack.roleStartBackStackIfNeeded(account = state.account)
}

private fun List<NavKey>.loginBackStackIfNeeded(): List<NavKey>? {
    if (firstOrNull() == Route.Login && all { it is Route.SignedOut }) return null
    return listOf(Route.Login)
}

private fun List<NavKey>.roleStartBackStackIfNeeded(account: Account): List<NavKey>? {
    val current = lastOrNull()
    if (current is Route.SignedIn) return null

    val startRoute = when (account.role) {
        UserRole.TRAINER -> Route.ClientList
        UserRole.CLIENT -> Route.ClientHome(account.uid)
    }
    return listOf(startRoute)
}