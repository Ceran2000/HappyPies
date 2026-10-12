package pl.ceranka.happypies.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import pl.ceranka.happypies.auth.SessionState
import pl.ceranka.happypies.data.user.UserProfile
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.fakes.trainer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** Uses the real NavBackStack: App.kt passes it, and it must be compared by content. */
class SessionNavigationTest {

    private fun stackOf(vararg routes: NavKey) = NavBackStack(*routes)

    @Test
    fun restoringKeepsTheStack() {
        assertNull(requiredBackStack(SessionState.Restoring, stackOf(Route.Login)))
    }

    @Test
    fun signedOutOnLoginNeedsNothing() {
        assertNull(requiredBackStack(SessionState.SignedOut, stackOf(Route.Login)))
    }

    @Test
    fun signedOutElsewhereGoesToLogin() {
        assertEquals(listOf(Route.Login), requiredBackStack(SessionState.SignedOut, stackOf(Route.ClientList)))
    }

    @Test
    fun signedInOnLoginGoesToTheRolesStartScreen() {
        assertEquals(listOf(Route.ClientList), requiredBackStack(SessionState.SignedIn(trainer), stackOf(Route.Login)))
        val client = UserProfile("client-1", "Anna", "anna@przyklad.pl", UserRole.CLIENT)
        assertEquals(listOf(Route.ClientHome), requiredBackStack(SessionState.SignedIn(client), stackOf(Route.Login)))
    }

    @Test
    fun signedInKeepsARestoredStack() {
        assertNull(
            requiredBackStack(SessionState.SignedIn(trainer), stackOf(Route.ClientList, Route.ClientDetail("client-1")))
        )
    }
}
