package pl.ceranka.happypies

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.AccountState
import pl.ceranka.happypies.firebase.auth.AuthManager
import pl.ceranka.happypies.navigation.Route
import pl.ceranka.happypies.navigation.requiredBackStack
import pl.ceranka.happypies.navigation.routeSavedStateConfiguration
import pl.ceranka.happypies.ui.common.AccountUnavailableScreen
import pl.ceranka.happypies.ui.common.toUserMessage
import pl.ceranka.happypies.ui.screen.add_edit_report.AddReportScreen
import pl.ceranka.happypies.ui.screen.add_edit_report.EditReportScreen
import pl.ceranka.happypies.ui.screen.client_details.ClientDetailsScreen
import pl.ceranka.happypies.ui.screen.client_home.ClientHomeScreen
import pl.ceranka.happypies.ui.screen.client_list.ClientListScreen
import pl.ceranka.happypies.ui.screen.login.LoginScreen
import pl.ceranka.happypies.ui.screen.report_details.ReportDetailsScreen
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

        fun goBack() {
            backStack.removeLastOrNull()
        }

        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.safeDrawingPadding()) {
                val state = accountState
                if (state is AccountState.Unavailable) {
                    AccountUnavailableScreen(
                        message = stringResource(state.error.toUserMessage()),
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
                    transitionSpec = {
                        (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 4 } + fadeOut())
                    },
                    popTransitionSpec = { popTransition() },
                    predictivePopTransitionSpec = { popTransition() },
                    entryProvider = entryProvider {
                        entry<Route.Login> {
                            LoginScreen()
                        }
                        entry<Route.ClientList> {
                            ClientListScreen(
                                onClientClick = { clientId -> backStack.add(Route.ClientDetail(clientId)) },
                                onLogout = ::logout
                            )
                        }
                        entry<Route.ClientDetail> { route ->
                            ClientDetailsScreen(
                                clientId = route.clientId,
                                onBack = ::goBack,
                                onAddReport = { backStack.add(Route.AddReport(route.clientId)) },
                                onReportClick = { reportId -> backStack.add(Route.ReportDetail(route.clientId, reportId)) }
                            )
                        }
                        entry<Route.ReportDetail> { route ->
                            ReportDetailsScreen(
                                clientId = route.clientId,
                                reportId = route.reportId,
                                onBack = ::goBack,
                                onEditReport = { backStack.add(Route.EditReport(route.reportId)) },
                                onDeleted = ::goBack
                            )
                        }
                        entry<Route.AddReport> { route ->
                            AddReportScreen(
                                clientId = route.clientId,
                                onSaved = ::goBack,
                                onCancel = ::goBack
                            )
                        }
                        entry<Route.EditReport> { route ->
                            EditReportScreen(
                                reportId = route.reportId,
                                onSaved = ::goBack,
                                onCancel = ::goBack
                            )
                        }
                        entry<Route.ClientHome> { route ->
                            ClientHomeScreen(
                                clientId = route.clientId,
                                onAddReport = { backStack.add(Route.AddReport(route.clientId)) },
                                onReportClick = { reportId -> backStack.add(Route.ReportDetail(route.clientId, reportId)) },
                                onLogout = ::logout
                            )
                        }
                    }
                )
            }
        }
    }
}

private fun popTransition(): ContentTransform =
    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())