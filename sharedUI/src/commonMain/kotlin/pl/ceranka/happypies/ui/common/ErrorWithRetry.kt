package pl.ceranka.happypies.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ErrorWithRetry(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(text = message, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = onRetry) {
            Text("Spróbuj ponownie")
        }
    }
}