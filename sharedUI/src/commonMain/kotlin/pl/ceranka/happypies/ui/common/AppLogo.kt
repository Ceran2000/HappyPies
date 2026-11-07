package pl.ceranka.happypies.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

@Composable
fun AppLogo(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.semantics(mergeDescendants = true) {}
    ) {
        Surface(shape = CircleShape, color = colors.primary, contentColor = colors.onPrimary, modifier = Modifier.size(88.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Pets, contentDescription = null, modifier = Modifier.size(48.dp))
            }
        }
        Text(
            text = buildAnnotatedString {
                append("Happy")
                withStyle(SpanStyle(color = colors.secondary)) { append("Pies") }
            },
            style = MaterialTheme.typography.headlineLarge
        )
    }
}