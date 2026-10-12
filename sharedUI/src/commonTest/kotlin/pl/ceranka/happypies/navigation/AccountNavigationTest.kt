package pl.ceranka.happypies.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.account.AccountState
import pl.ceranka.happypies.data.DataException
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.fakes.trainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Uses the real NavBackStack: App.kt passes it, and it must be compared by content. */
class SessionNavigationTest {

    private fun stackOf(vararg routes: NavKey) = NavBackStack(*routes)

    @Test
    fun loadingKeepsTheStack() {
        assertNull(requiredBackStack(AccountState.Loading, stackOf(Route.Login)))
    }

    @Test
    fun unavailableAccountKeepsTheStack() {
        val offline = AccountState.Unavailable(DataException.Unavailable("offline"))
        assertNull(requiredBackStack(offline, stackOf(Route.ClientList)))
    }

    @Test
    fun signedOutOnLoginNeedsNothing() {
        assertNull(requiredBackStack(AccountState.SignedOut(), stackOf(Route.Login)))
    }

    @Test
    fun signedOutElsewhereGoesToLogin() {
        assertEquals(listOf(Route.Login), requiredBackStack(AccountState.SignedOut(), stackOf(Route.ClientList)))
    }

    @Test
    fun signedInOnLoginGoesToTheRolesStartScreen() {
        assertEquals(listOf(Route.ClientList), requiredBackStack(AccountState.SignedIn(trainer), stackOf(Route.Login)))
        val client = Account("client-1", "anna@przyklad.pl", "Anna", UserRole.CLIENT)
        assertEquals(listOf(Route.ClientHome), requiredBackStack(AccountState.SignedIn(client), stackOf(Route.Login)))
    }

    @Test
    fun signedInKeepsARestoredStack() {
        assertNull(
            requiredBackStack(AccountState.SignedIn(trainer), stackOf(Route.ClientList, Route.ClientDetail("client-1")))
        )
    }
}
