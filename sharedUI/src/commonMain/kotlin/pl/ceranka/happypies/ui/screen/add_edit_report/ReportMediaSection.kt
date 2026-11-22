package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.VideoCall
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.common_media_label
import happypies.sharedui.generated.resources.common_photo_item
import happypies.sharedui.generated.resources.common_video_item
import happypies.sharedui.generated.resources.report_form_add_photos
import happypies.sharedui.generated.resources.report_form_add_videos
import happypies.sharedui.generated.resources.report_form_media_over_limit
import happypies.sharedui.generated.resources.report_form_photo_limit
import happypies.sharedui.generated.resources.report_form_remove_photo
import happypies.sharedui.generated.resources.report_form_remove_video
import happypies.sharedui.generated.resources.report_form_video_limit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pl.ceranka.happypies.data.report.MediaType
import pl.ceranka.happypies.ui.common.MediaGrid
import pl.ceranka.happypies.ui.common.MediaTileFrame
import pl.ceranka.happypies.ui.common.PhotoTileContent
import pl.ceranka.happypies.ui.common.VideoTileContent
import pl.ceranka.happypies.ui.common.formatMegabytes
import pl.ceranka.happypies.ui.media.PickedFile
import pl.ceranka.happypies.ui.media.toPickedFile

private val RemoveButtonBackground = Color.Black.copy(alpha = 0.5f)

@Composable
fun ReportMediaSection(
    media: List<ReportMedia>,
    showAddPhotosButton: Boolean,
    showAddVideosButton: Boolean,
    photoPickerMaxItems: Int,
    videoPickerMaxItems: Int,
    addButtonEnabled: Boolean,
    showRemoveButtons: Boolean,
    showLimitError: Boolean,
    errorMessage: StringResource?,
    onMediaPicked: (List<PickedFile>) -> Unit,
    onRemoveClick: (ReportMedia) -> Unit
) {
    val onFilesPicked: (List<PlatformFile>?) -> Unit = { files -> files?.let { onMediaPicked(it.map { file -> file.toPickedFile() }) } }
    val photoPicker = rememberFilePickerLauncher(type = FileKitType.Image, mode = FileKitMode.Multiple(maxItems = photoPickerMaxItems), onResult = onFilesPicked)
    val videoPicker = rememberFilePickerLauncher(type = FileKitType.Video, mode = FileKitMode.Multiple(maxItems = videoPickerMaxItems), onResult = onFilesPicked)

    val addButtonTypes = listOfNotNull(MediaType.PHOTO.takeIf { showAddPhotosButton }, MediaType.VIDEO.takeIf { showAddVideosButton })
    val tiles = formTiles(media, addButtonTypes)

    val onAddMediaClick: (MediaType) -> Unit = { type ->
        when (type) {
            MediaType.PHOTO -> photoPicker.launch()
            MediaType.VIDEO -> videoPicker.launch()
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            Text(stringResource(Res.string.common_media_label), fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MediaLimit(media, MediaType.PHOTO)
                MediaLimit(media, MediaType.VIDEO)
            }
        }

        MediaGrid(items = tiles) { tile, modifier ->
            when (tile) {
                is FormTile.Media -> MediaTile(
                    media = tile.media,
                    number = tile.number,
                    showRemoveButton = showRemoveButtons && tile.media !is ReportMedia.Compressing,
                    onRemoveClick = { onRemoveClick(tile.media) },
                    modifier = modifier
                )
                is FormTile.AddButton -> AddMediaTile(
                    type = tile.type,
                    enabled = addButtonEnabled,
                    onClick = onAddMediaClick,
                    modifier = modifier
                )
            }
        }

        if (showLimitError) {
            Text(
                text = stringResource(Res.string.report_form_media_over_limit, MediaType.PHOTO.maxReportCount(), MediaType.VIDEO.maxReportCount()),
                color = MaterialTheme.colorScheme.error
            )
        }
        errorMessage?.let { error ->
            Text(text = stringResource(error), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun MediaLimit(media: List<ReportMedia>, type: MediaType) {
    val label = when (type) {
        MediaType.PHOTO -> Res.string.report_form_photo_limit
        MediaType.VIDEO -> Res.string.report_form_video_limit
    }
    val exceeded = media.exceedsLimitOf(type)
    Text(
        text = stringResource(label, media.countOf(type), type.maxReportCount()),
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (exceeded) FontWeight.Bold else null,
        color = if (exceeded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

private sealed interface FormTile {
    data class Media(val media: ReportMedia, val number: Int) : FormTile
    data class AddButton(val type: MediaType) : FormTile
}

private fun formTiles(media: List<ReportMedia>, addButtonTypes: List<MediaType>): List<FormTile> =
    media.mapIndexed { index, item -> FormTile.Media(item, media.numberOf(index)) } + addButtonTypes.map { FormTile.AddButton(it) }

private fun List<ReportMedia>.numberOf(index: Int): Int = take(index).count { it.type == this[index].type } + 1

@Composable
private fun MediaTile(
    media: ReportMedia,
    number: Int,
    showRemoveButton: Boolean,
    onRemoveClick: () -> Unit,
    modifier: Modifier
) {
    Box(modifier = modifier) {
        MediaTileFrame(Modifier.fillMaxSize()) {
            when (media) {
                is ReportMedia.Compressing -> CompressingContent(media.percent)
                is ReportMedia.Compressed -> FileContent(media.type, media.localFileUri, media.sizeBytes, number)
                is ReportMedia.Uploaded -> FileContent(media.type, media.previewUri, media.item.sizeBytes, number)
            }
        }
        if (showRemoveButton) {
            RemoveMediaButton(
                contentDescription = stringResource(media.removeLabel(), number),
                onClick = onRemoveClick,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}

@Composable
private fun RemoveMediaButton(contentDescription: String, onClick: () -> Unit, modifier: Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(24.dp).background(RemoveButtonBackground, CircleShape)
        ) {
            Icon(Icons.Rounded.Close, contentDescription = contentDescription, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun AddMediaTile(type: MediaType, enabled: Boolean, onClick: (MediaType) -> Unit, modifier: Modifier) {
    val contentColor = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
    MediaTileFrame(
        modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(enabled = enabled, role = Role.Button, onClick = { onClick(type) })
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(type.addIcon(), contentDescription = null, tint = contentColor)
            Text(
                text = stringResource(type.addLabel()),
                color = contentColor,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

private fun MediaType.addIcon(): ImageVector = when (this) {
    MediaType.PHOTO -> Icons.Rounded.AddPhotoAlternate
    MediaType.VIDEO -> Icons.Rounded.VideoCall
}

private fun MediaType.addLabel(): StringResource = when (this) {
    MediaType.PHOTO -> Res.string.report_form_add_photos
    MediaType.VIDEO -> Res.string.report_form_add_videos
}

@Composable
private fun FileContent(type: MediaType, previewUri: String, sizeBytes: Long, number: Int) {
    when (type) {
        MediaType.PHOTO -> PhotoTileContent(
            model = previewUri,
            contentDescription = stringResource(Res.string.common_photo_item, number)
        )
        MediaType.VIDEO -> {
            val sizeText = formatMegabytes(sizeBytes)
            VideoTileContent(
                contentDescription = stringResource(Res.string.common_video_item, number, sizeText),
                sizeText = sizeText
            )
        }
    }
}

@Composable
private fun CompressingContent(percent: Int) {
    CircularProgressIndicator(progress = { percent / 100f })
    Text(text = "$percent%", style = MaterialTheme.typography.labelMedium)
}

private fun ReportMedia.removeLabel(): StringResource = when (type) {
    MediaType.PHOTO -> Res.string.report_form_remove_photo
    MediaType.VIDEO -> Res.string.report_form_remove_video
}