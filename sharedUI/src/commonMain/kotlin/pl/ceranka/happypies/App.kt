package pl.ceranka.happypies

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.navigation.Route
import pl.ceranka.happypies.navigation.routeSavedStateConfiguration
import pl.ceranka.happypies.screen.client_details.ClientDetailsScreen
import pl.ceranka.happypies.screen.client_list.ClientListScreen
import pl.ceranka.happypies.screen.clienthome.ClientHomeScreen
import pl.ceranka.happypies.screen.login.LoginScreen
import pl.ceranka.happypies.ui.common.AuthenticatedScreen

@Composable
fun App() {
    val authManager = koinInject<AuthManager>()
    MaterialTheme {
        val backStack = rememberNavBackStack(routeSavedStateConfiguration, Route.Login)
        val scope = rememberCoroutineScope()

        fun logout() {
            scope.launch {
                authManager.signOut()
                backStack.clear()
                backStack.add(Route.Login)
            }
        }

        NavDisplay(
            backStack = backStack,
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<Route.Login> {
                    LoginScreen(
                        onLoggedIn = { role ->
                            backStack.clear()
                            backStack.add(
                                when (role) {
                                    UserRole.TRAINER -> Route.ClientList
                                    UserRole.CLIENT -> Route.ClientHome
                                }
                            )
                        }
                    )
                }
                entry<Route.ClientList> {
                    AuthenticatedScreen(onLogout = ::logout) {
                        ClientListScreen(
                            onClientClick = { clientId -> backStack.add(Route.ClientDetail(clientId)) }
                        )
                    }
                }
                entry<Route.ClientDetail> { route ->
                    AuthenticatedScreen(onLogout = ::logout) {
                        ClientDetailsScreen(
                            clientId = route.clientId,
                            onBack = { backStack.removeLastOrNull() }
                        )
                    }
                }
                entry<Route.ClientHome> {
                    AuthenticatedScreen(onLogout = ::logout) {
                        ClientHomeScreen()
                    }
                }
            }
        )
    }
}