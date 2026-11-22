package pl.ceranka.happypies.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BrokenImage
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.AsyncImagePainter
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.common_photo_load_failed
import org.jetbrains.compose.resources.stringResource

private const val MEDIA_GRID_COLUMNS = 3

@Composable
fun <T> MediaGrid(items: List<T>, tile: @Composable (item: T, modifier: Modifier) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(MEDIA_GRID_COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { item -> tile(item, Modifier.weight(1f).aspectRatio(1f)) }
                repeat(MEDIA_GRID_COLUMNS - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
fun MediaTileFrame(modifier: Modifier, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        content()
    }
}

@Composable
fun PhotoTileContent(model: String, contentDescription: String) {
    PhotoImage(model, contentDescription, ContentScale.Crop, errorIconTint = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
fun PhotoImage(model: String, contentDescription: String, contentScale: ContentScale, errorIconTint: Color) {
    var loadFailed by remember(model) { mutableStateOf(false) }
    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = model,
            contentDescription = contentDescription,
            contentScale = contentScale,
            onState = { loadFailed = it is AsyncImagePainter.State.Error },
            modifier = Modifier.fillMaxSize()
        )
        if (loadFailed) {
            Icon(
                Icons.Rounded.BrokenImage,
                contentDescription = stringResource(Res.string.common_photo_load_failed),
                tint = errorIconTint
            )
        }
    }
}

@Composable
fun VideoTileContent(contentDescription: String, sizeText: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(Icons.Rounded.PlayArrow, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.primary)
        Text(
            text = sizeText,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp).clearAndSetSemantics {}
        )
    }
}