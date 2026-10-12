package pl.ceranka.happypies

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pl.ceranka.happypies.auth.SessionManager
import pl.ceranka.happypies.auth.SessionState
import pl.ceranka.happypies.navigation.Route
import pl.ceranka.happypies.navigation.requiredBackStack
import pl.ceranka.happypies.navigation.routeSavedStateConfiguration
import pl.ceranka.happypies.screen.client_details.ClientDetailsScreen
import pl.ceranka.happypies.screen.client_list.ClientListScreen
import pl.ceranka.happypies.screen.clienthome.ClientHomeScreen
import pl.ceranka.happypies.screen.login.LoginScreen
import pl.ceranka.happypies.screen.report_form.ReportFormScreen
import pl.ceranka.happypies.ui.common.AuthenticatedScreen

//TODO: should have ViewModel?

@Composable
fun App() {
    val sessionManager = koinInject<SessionManager>()
    MaterialTheme {
        // Restored after process death; kept as long as the session allows it.
        val backStack = rememberNavBackStack(routeSavedStateConfiguration, Route.Login)
        val session by sessionManager.state.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()

        LaunchedEffect(Unit) { sessionManager.restore() }

        // Navigation follows the session: signing in, signing out and restoring move the back stack.
        val requiredStack = requiredBackStack(session, backStack)
        LaunchedEffect(requiredStack) {
            requiredStack?.let {
                backStack.clear()
                backStack.addAll(it)
            }
        }

        fun logout() {
            scope.launch { sessionManager.signOut() }
        }

        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            if (session == SessionState.Restoring || requiredStack != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else NavDisplay(
                backStack = backStack,
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberViewModelStoreNavEntryDecorator(),
                ),
                entryProvider = entryProvider {
                    entry<Route.Login> {
                        LoginScreen()
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
                                onBack = { backStack.removeLastOrNull() },
                                onAddReport = { backStack.add(Route.ReportForm(route.clientId)) },
                            )
                        }
                    }
                    entry<Route.ReportForm> { route ->
                        AuthenticatedScreen(onLogout = ::logout) {
                            ReportFormScreen(
                                clientId = route.clientId,
                                onSaved = { backStack.removeLastOrNull() },
                                onCancel = { backStack.removeLastOrNull() },
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
}