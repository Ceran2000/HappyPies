package pl.ceranka.happypies.ui.screen.report_details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.common_photo_item
import happypies.sharedui.generated.resources.report_details_close_photo
import org.jetbrains.compose.resources.stringResource
import pl.ceranka.happypies.ui.common.PhotoImage

@Composable
fun PhotoViewer(photoUrls: List<String>, initialIndex: Int, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val pagerState = rememberPagerState(initialPage = initialIndex) { photoUrls.size }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
                PhotoImage(
                    model = photoUrls[page],
                    contentDescription = stringResource(Res.string.common_photo_item, page + 1),
                    contentScale = ContentScale.Fit,
                    errorIconTint = Color.White
                )
            }
            FilledTonalIconButton(
                onClick = onClose,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .windowInsetsPadding(WindowInsets.safeDrawing)
                    .padding(8.dp)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.report_details_close_photo))
            }
        }
    }
}