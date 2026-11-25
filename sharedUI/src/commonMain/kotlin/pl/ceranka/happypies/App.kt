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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pl.ceranka.happypies.data.account.AccountState
import pl.ceranka.happypies.navigation.Route
import pl.ceranka.happypies.navigation.requiredBackStack
import pl.ceranka.happypies.navigation.routeSavedStateConfiguration
import pl.ceranka.happypies.ui.common.AccountUnavailableScreen
import pl.ceranka.happypies.ui.common.toUserMessage
import pl.ceranka.happypies.ui.screen.accept_invitation.AcceptInvitationScreen
import pl.ceranka.happypies.ui.screen.add_edit_report.AddReportScreen
import pl.ceranka.happypies.ui.screen.add_edit_report.EditReportScreen
import pl.ceranka.happypies.ui.screen.client_details.ClientDetailsScreen
import pl.ceranka.happypies.ui.screen.client_home.ClientHomeScreen
import pl.ceranka.happypies.ui.screen.client_list.ClientListScreen
import pl.ceranka.happypies.ui.screen.invitation_code.InvitationCodeScreen
import pl.ceranka.happypies.ui.screen.invite_client.InviteClientScreen
import pl.ceranka.happypies.ui.screen.login.LoginScreen
import pl.ceranka.happypies.ui.screen.report_details.ReportDetailsScreen
import pl.ceranka.happypies.ui.theme.HappyPiesTheme

@Composable
fun App() {
    val viewModel = koinViewModel<AppViewModel>()

    HappyPiesTheme {
        // Restored after process death; kept as long as the account allows it.
        val backStack = rememberNavBackStack(routeSavedStateConfiguration, Route.Login)
        val accountState by viewModel.accountState.collectAsStateWithLifecycle()
        val pendingInvitationCode by viewModel.pendingInvitationCode.collectAsStateWithLifecycle()

        // Navigation follows the account: signing in and out (also outside the app) moves the back stack.
        val requiredStack = requiredBackStack(accountState, backStack)
        LaunchedEffect(requiredStack) {
            requiredStack?.let {
                backStack.clear()
                backStack.addAll(it)
            }
        }

        Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.safeDrawingPadding()) {
                val state = accountState
                when {
                    state is AccountState.Unavailable -> AccountUnavailableScreen(
                        message = stringResource(state.error.toUserMessage()),
                        onRetry = viewModel::onRetryClick,
                        onLogout = viewModel::onLogoutClick
                    )
                    state == AccountState.Loading || requiredStack != null -> LoadingScreen()
                    else -> AppNavDisplay(
                        backStack = backStack,
                        pendingInvitationCode = pendingInvitationCode,
                        onInvitationOpened = viewModel::onInvitationOpened,
                        onLogout = viewModel::onLogoutClick
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AppNavDisplay(
    backStack: NavBackStack<NavKey>,
    pendingInvitationCode: String?,
    onInvitationOpened: () -> Unit,
    onLogout: () -> Unit
) {
    // Only a signed-out screen opens an invitation link; the code waits until one is shown.
    if (backStack.lastOrNull() is Route.SignedOut) {
        LaunchedEffect(pendingInvitationCode) {
            pendingInvitationCode?.let { code ->
                backStack.clear()
                backStack.addAll(listOf(Route.Login, Route.AcceptInvitation(code)))
                onInvitationOpened()
            }
        }
    }

    val onNavigateBack: () -> Unit = {
        backStack.removeLastOrNull()
    }

    val onNavigateBackToLogin: () -> Unit = {
        backStack.clear()
        backStack.add(Route.Login)
    }

    NavDisplay(
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
                LoginScreen(onHaveInvitationClick = { backStack.add(Route.InvitationCode) })
            }
            entry<Route.InvitationCode> {
                InvitationCodeScreen(
                    onFound = { code -> backStack.add(Route.AcceptInvitation(code)) },
                    onBack = onNavigateBack
                )
            }
            entry<Route.AcceptInvitation> { route ->
                AcceptInvitationScreen(
                    code = route.code,
                    onBack = onNavigateBack,
                    onBackToLogin = onNavigateBackToLogin
                )
            }
            entry<Route.ClientList> {
                ClientListScreen(
                    onClientClick = { clientId -> backStack.add(Route.ClientDetail(clientId)) },
                    onInviteClick = { backStack.add(Route.InviteClient) },
                    onLogout = onLogout
                )
            }
            entry<Route.InviteClient> {
                InviteClientScreen(
                    onSent = onNavigateBack,
                    onBack = onNavigateBack
                )
            }
            entry<Route.ClientDetail> { route ->
                ClientDetailsScreen(
                    clientId = route.clientId,
                    onBack = onNavigateBack,
                    onAddReport = { backStack.add(Route.AddReport(route.clientId)) },
                    onReportClick = { reportId -> backStack.add(Route.ReportDetail(route.clientId, reportId)) }
                )
            }
            entry<Route.ReportDetail> { route ->
                ReportDetailsScreen(
                    clientId = route.clientId,
                    reportId = route.reportId,
                    onBack = onNavigateBack,
                    onEditReport = { backStack.add(Route.EditReport(route.reportId)) },
                    onDeleted = onNavigateBack
                )
            }
            entry<Route.AddReport> { route ->
                AddReportScreen(
                    clientId = route.clientId,
                    onSaved = onNavigateBack,
                    onCancel = onNavigateBack
                )
            }
            entry<Route.EditReport> { route ->
                EditReportScreen(
                    reportId = route.reportId,
                    onSaved = onNavigateBack,
                    onCancel = onNavigateBack
                )
            }
            entry<Route.ClientHome> { route ->
                ClientHomeScreen(
                    clientId = route.clientId,
                    onAddReport = { backStack.add(Route.AddReport(route.clientId)) },
                    onReportClick = { reportId -> backStack.add(Route.ReportDetail(route.clientId, reportId)) },
                    onLogout = onLogout
                )
            }
        }
    )
}

private fun popTransition(): ContentTransform =
    (slideInHorizontally { -it / 4 } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())