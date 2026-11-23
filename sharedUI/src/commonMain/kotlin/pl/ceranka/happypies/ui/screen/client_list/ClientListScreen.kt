package pl.ceranka.happypies.ui.screen.client_list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.client_list_cancel_invitation
import happypies.sharedui.generated.resources.client_list_cancel_invitation_message
import happypies.sharedui.generated.resources.client_list_cancel_invitation_title
import happypies.sharedui.generated.resources.client_list_empty
import happypies.sharedui.generated.resources.client_list_invitation_expired
import happypies.sharedui.generated.resources.client_list_invitation_expired_on
import happypies.sharedui.generated.resources.client_list_invitation_valid_until
import happypies.sharedui.generated.resources.client_list_invitation_valid_until_long
import happypies.sharedui.generated.resources.client_list_invitations_title
import happypies.sharedui.generated.resources.client_list_invite
import happypies.sharedui.generated.resources.client_list_keep_invitation
import happypies.sharedui.generated.resources.client_list_resend_invitation
import happypies.sharedui.generated.resources.client_list_resend_invitation_hint
import happypies.sharedui.generated.resources.client_list_title
import happypies.sharedui.generated.resources.common_cancel
import happypies.sharedui.generated.resources.common_greeting
import happypies.sharedui.generated.resources.common_logout
import happypies.sharedui.generated.resources.common_logout_description
import happypies.sharedui.generated.resources.common_logout_dialog_title
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import pl.ceranka.happypies.data.client.Client
import pl.ceranka.happypies.ui.common.CenteredProgress
import pl.ceranka.happypies.ui.common.ErrorWithRetry
import pl.ceranka.happypies.ui.common.HappyPiesAppBar
import pl.ceranka.happypies.ui.common.InitialsAvatar
import pl.ceranka.happypies.ui.common.dashedBorder
import pl.ceranka.happypies.ui.common.initialsOf

@Composable
fun ClientListScreen(
    onClientClick: (String) -> Unit,
    onInviteClick: () -> Unit,
    onLogout: () -> Unit
) {
    val viewModel = koinViewModel<ClientListViewModel>()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()

    ClientListContent(
        uiState = uiState,
        snackbarMessage = snackbarMessage,
        onClientClick = onClientClick,
        onInviteClick = onInviteClick,
        onRefresh = viewModel::onRefresh,
        onRetryClick = viewModel::onRetryClick,
        onResendInvitationClick = viewModel::onResendInvitationClick,
        onCancelInvitationClick = viewModel::onCancelInvitationClick,
        onSnackbarMessageShown = viewModel::onSnackbarMessageShown,
        onLogout = onLogout
    )
}

@Composable
private fun ClientListContent(
    uiState: ClientListUiState,
    snackbarMessage: StringResource?,
    onClientClick: (String) -> Unit,
    onInviteClick: () -> Unit,
    onRefresh: () -> Unit,
    onRetryClick: () -> Unit,
    onResendInvitationClick: (String) -> Unit,
    onCancelInvitationClick: (String) -> Unit,
    onSnackbarMessageShown: () -> Unit,
    onLogout: () -> Unit
) {
    var showLogoutDialog by rememberSaveable { mutableStateOf(false) }
    var selectedInvitation by remember { mutableStateOf<InvitationItem?>(null) }
    var invitationToCancelId by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    snackbarMessage?.let { message ->
        val text = stringResource(message)
        LaunchedEffect(message) {
            try {
                snackbarHostState.showSnackbar(text)
            } finally {
                onSnackbarMessageShown()
            }
        }
    }

    Scaffold(
        topBar = {
            HappyPiesAppBar(
                title = {},
                navigationIcon = null,
                actions = {
                    IconButton(onClick = { showLogoutDialog = true }) {
                        Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = stringResource(Res.string.common_logout_description))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onInviteClick,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(Res.string.client_list_invite)) }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val contentModifier = Modifier.fillMaxSize().padding(padding)
        when (uiState) {
            ClientListUiState.Loading -> Box(contentModifier) { CenteredProgress() }
            is ClientListUiState.Error -> Box(contentModifier) {
                ErrorWithRetry(
                    message = stringResource(uiState.message),
                    onRetry = onRetryClick,
                    modifier = Modifier.padding(24.dp)
                )
            }
            is ClientListUiState.Success -> {
                PullToRefreshBox(
                    isRefreshing = uiState.isRefreshing,
                    onRefresh = onRefresh,
                    modifier = contentModifier
                ) {
                    ClientListItems(
                        uiState = uiState,
                        onClientClick = onClientClick,
                        onInvitationClick = { selectedInvitation = it }
                    )
                }

                selectedInvitation?.let { invitation ->
                    InvitationActionsSheet(
                        invitation = invitation,
                        onDismiss = { selectedInvitation = null },
                        onResendClick = {
                            selectedInvitation = null
                            onResendInvitationClick(invitation.id)
                        },
                        onCancelClick = {
                            selectedInvitation = null
                            invitationToCancelId = invitation.id
                        }
                    )
                }
            }
        }
    }

    invitationToCancelId?.let { invitationId ->
        AlertDialog(
            onDismissRequest = { invitationToCancelId = null },
            title = { Text(stringResource(Res.string.client_list_cancel_invitation_title)) },
            text = { Text(stringResource(Res.string.client_list_cancel_invitation_message)) },
            confirmButton = {
                TextButton(onClick = {
                    invitationToCancelId = null
                    onCancelInvitationClick(invitationId)
                }) {
                    Text(stringResource(Res.string.client_list_cancel_invitation), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { invitationToCancelId = null }) {
                    Text(stringResource(Res.string.client_list_keep_invitation))
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text(stringResource(Res.string.common_logout_dialog_title)) },
            confirmButton = {
                TextButton(onClick = {
                    showLogoutDialog = false
                    onLogout()
                }) {
                    Text(stringResource(Res.string.common_logout))
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text(stringResource(Res.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun ClientListItems(
    uiState: ClientListUiState.Success,
    onClientClick: (String) -> Unit,
    onInvitationClick: (InvitationItem) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        uiState.trainerFirstName?.let { firstName ->
            item(
                key = "greeting",
                contentType = "greeting"
            ) {
                Greeting(firstName)
            }
        }

        item(
            key = "clientsTitle",
            contentType = "sectionTitle"
        ) {
            SectionTitle(stringResource(Res.string.client_list_title))
        }

        if (uiState.clients.isEmpty()) {
            item(key = "emptyClients") { EmptyClients() }
        }

        items(
            items = uiState.clients,
            key = { it.id },
            contentType = { "client" }
        ) { client ->
            ClientCard(client = client, onClick = { onClientClick(client.id) })
        }

        if (uiState.showInvitations) {
            item(
                key = "invitationsTitle",
                contentType = "sectionTitle"
            ) {
                SectionTitle(stringResource(Res.string.client_list_invitations_title))
            }

            items(
                items = uiState.invitations,
                key = { "invitation-${it.id}" },
                contentType = { "invitation" }
            ) { invitation ->
                InvitationCard(invitation = invitation, onClick = { onInvitationClick(invitation) })
            }
        }
    }
}

@Composable
private fun Greeting(firstName: String) {
    Text(text = stringResource(Res.string.common_greeting, firstName), style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(horizontal = 8.dp))
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)
    )
}

@Composable
private fun ClientCard(client: Client, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            InitialsAvatar(name = client.name, colorKey = client.id)
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = client.name, style = MaterialTheme.typography.titleMedium)
                if (client.dogs.isNotEmpty()) {
                    Text(
                        text = client.dogs.joinToString(" · ") { it.name },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun InvitationCard(invitation: InvitationItem, onClick: () -> Unit) {
    ElevatedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            InvitedAvatar(name = invitation.name)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(text = invitation.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = invitation.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            InvitationStatusChip(invitation)
        }
    }
}

@Composable
private fun InvitedAvatar(name: String) {
    Box(
        modifier = Modifier.size(48.dp).dashedBorder(MaterialTheme.colorScheme.outline, cornerRadius = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = initialsOf(name), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InvitationStatusChip(invitation: InvitationItem) {
    val colors = MaterialTheme.colorScheme
    Surface(
        shape = CircleShape,
        color = if (invitation.isExpired) colors.errorContainer else colors.surfaceVariant,
        contentColor = if (invitation.isExpired) colors.onErrorContainer else colors.onSurfaceVariant
    ) {
        Text(
            text = if (invitation.isExpired) {
                stringResource(Res.string.client_list_invitation_expired)
            } else {
                stringResource(Res.string.client_list_invitation_valid_until, invitation.expiryDate)
            },
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvitationActionsSheet(
    invitation: InvitationItem,
    onDismiss: () -> Unit,
    onResendClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                InvitedAvatar(name = invitation.name)
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(text = invitation.name, style = MaterialTheme.typography.titleMedium)
                    Text(text = invitation.email, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = if (invitation.isExpired) {
                            stringResource(Res.string.client_list_invitation_expired_on, invitation.expiryDate)
                        } else {
                            stringResource(Res.string.client_list_invitation_valid_until_long, invitation.expiryDate)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (invitation.isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            ElevatedCard(onClick = onResendClick, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                ListItem(
                    headlineContent = { Text(stringResource(Res.string.client_list_resend_invitation), style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text(stringResource(Res.string.client_list_resend_invitation_hint)) },
                    leadingContent = { Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
            ElevatedCard(onClick = onCancelClick, modifier = Modifier.fillMaxWidth()) {
                ListItem(
                    headlineContent = {
                        Text(stringResource(Res.string.client_list_cancel_invitation), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    },
                    leadingContent = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                    colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface)
                )
            }
        }
    }
}

@Composable
private fun EmptyClients() {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.client_list_empty),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 28.dp)
        )
    }
}