package pl.ceranka.happypies.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ErrorWithRetry(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center)
        TextButton(onClick = onRetry) {
            Text("Spróbuj ponownie")
        }
    }
}