package pl.ceranka.happypies.navigation

import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertNotNull

class RouteSavedStateTest {

    @Test
    fun everyRouteCanBeSavedWithTheBackStack() {
        val routes = listOf(
            Route.Login,
            Route.InvitationCode,
            Route.AcceptInvitation("ABCD2345"),
            Route.ClientList,
            Route.InviteClient,
            Route.ClientHome("client-1"),
            Route.ClientDetail("client-1"),
            Route.ReportDetail("client-1", "r1"),
            Route.AddReport("client-1"),
            Route.EditReport("r1")
        )

        routes.forEach { route ->
            assertNotNull(routeSavedStateConfiguration.serializersModule.getPolymorphic(NavKey::class, route), "$route is not registered")
        }
    }
}