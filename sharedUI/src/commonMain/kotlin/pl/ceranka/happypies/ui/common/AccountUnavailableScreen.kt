package pl.ceranka.happypies.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.account_unavailable_title
import happypies.sharedui.generated.resources.common_logout
import happypies.sharedui.generated.resources.common_retry
import org.jetbrains.compose.resources.stringResource

/** Firebase remembers the user, but their account could not be loaded for now. */
@Composable
fun AccountUnavailableScreen(
    message: String,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = stringResource(Res.string.account_unavailable_title), style = MaterialTheme.typography.titleMedium)
        Text(text = message, color = MaterialTheme.colorScheme.error)
        Button(onClick = onRetry) {
            Text(stringResource(Res.string.common_retry))
        }
        TextButton(onClick = onLogout) {
            Text(stringResource(Res.string.common_logout))
        }
    }
}