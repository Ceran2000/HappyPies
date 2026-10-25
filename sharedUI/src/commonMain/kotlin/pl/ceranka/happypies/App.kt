package pl.ceranka.happypies

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import pl.ceranka.happypies.auth.AuthManager
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.AccountState
import pl.ceranka.happypies.navigation.Route
import pl.ceranka.happypies.navigation.requiredBackStack
import pl.ceranka.happypies.navigation.routeSavedStateConfiguration
import pl.ceranka.happypies.screen.client_details.ClientDetailsScreen
import pl.ceranka.happypies.screen.client_list.ClientListScreen
import pl.ceranka.happypies.screen.clienthome.ClientHomeScreen
import pl.ceranka.happypies.screen.login.LoginScreen
import pl.ceranka.happypies.screen.report_details.ReportDetailsScreen
import pl.ceranka.happypies.screen.report_form.AddReportScreen
import pl.ceranka.happypies.screen.report_form.EditReportScreen
import pl.ceranka.happypies.ui.common.AuthenticatedScreen
import pl.ceranka.happypies.ui.common.AccountUnavailableScreen
import pl.ceranka.happypies.ui.common.toUserMessage
import pl.ceranka.happypies.ui.theme.HappyPiesTheme

//TODO: should have ViewModel?

@Composable
fun App() {
    val authManager = koinInject<AuthManager>()
    val accountRepository = koinInject<AccountRepository>()
    HappyPiesTheme {
        // Restored after process death; kept as long as the account allows it.
        val backStack = rememberNavBackStack(routeSavedStateConfiguration, Route.Login)
        val accountState by accountRepository.state.collectAsStateWithLifecycle()
        val scope = rememberCoroutineScope()

        // Navigation follows the account: signing in and out (also outside the app) moves the back stack.
        val requiredStack = requiredBackStack(accountState, backStack)
        LaunchedEffect(requiredStack) {
            requiredStack?.let {
                backStack.clear()
                backStack.addAll(it)
            }
        }

        fun logout() {
            scope.launch {
                try {
                    authManager.signOut()
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Logger.withTag("App").e(e) { "Sign-out failed" }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.safeDrawingPadding()) {
                val state = accountState
                if (state is AccountState.Unavailable) {
                    AccountUnavailableScreen(
                        message = state.error.toUserMessage(),
                        onRetry = accountRepository::retry,
                        onLogout = ::logout,
                    )
                } else if (state == AccountState.Loading || requiredStack != null) {
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
                                    onAddReport = { backStack.add(Route.AddReport(route.clientId)) },
                                    onReportClick = { reportId -> backStack.add(Route.ReportDetail(route.clientId, reportId)) },
                                )
                            }
                        }
                        entry<Route.ReportDetail> { route ->
                            AuthenticatedScreen(onLogout = ::logout) {
                                ReportDetailsScreen(
                                    clientId = route.clientId,
                                    reportId = route.reportId,
                                    onBack = { backStack.removeLastOrNull() },
                                    onEditReport = { backStack.add(Route.EditReport(route.reportId)) },
                                    onDeleted = { backStack.removeLastOrNull() },
                                )
                            }
                        }
                        entry<Route.AddReport> { route ->
                            AuthenticatedScreen(onLogout = ::logout) {
                                AddReportScreen(
                                    clientId = route.clientId,
                                    onSaved = { backStack.removeLastOrNull() },
                                    onCancel = { backStack.removeLastOrNull() }
                                )
                            }
                        }
                        entry<Route.EditReport> { route ->
                            AuthenticatedScreen(onLogout = ::logout) {
                                EditReportScreen(
                                    reportId = route.reportId,
                                    onSaved = { backStack.removeLastOrNull() },
                                    onCancel = { backStack.removeLastOrNull() }
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
}