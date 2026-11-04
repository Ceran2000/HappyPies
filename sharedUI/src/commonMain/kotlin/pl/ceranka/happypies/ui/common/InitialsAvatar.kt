package pl.ceranka.happypies.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.ceranka.happypies.ui.theme.LocalAvatarColors

@Composable
fun InitialsAvatar(name: String, colorKey: String, modifier: Modifier = Modifier) {
    val colors = LocalAvatarColors.current
    Surface(
        shape = CircleShape,
        color = colors.backgroundFor(colorKey),
        contentColor = colors.content,
        modifier = modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(text = initialsOf(name), style = MaterialTheme.typography.titleMedium)
        }
    }
}