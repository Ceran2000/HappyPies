package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.report_form_add_video
import happypies.sharedui.generated.resources.report_form_description_label
import happypies.sharedui.generated.resources.report_form_description_placeholder
import happypies.sharedui.generated.resources.report_form_remove_video
import happypies.sharedui.generated.resources.report_form_save
import happypies.sharedui.generated.resources.report_form_title_label
import happypies.sharedui.generated.resources.report_form_uploading
import happypies.sharedui.generated.resources.report_form_video_item
import happypies.sharedui.generated.resources.report_form_videos_label
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.compose.rememberFilePickerLauncher
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pl.ceranka.happypies.ui.common.HappyPiesAppBar
import pl.ceranka.happypies.ui.common.ProgressButton
import pl.ceranka.happypies.ui.common.filledFieldColors
import pl.ceranka.happypies.ui.common.formatMegabytes

@Composable
fun ReportFormLayout(
    title: String,
    cancelButtonEnabled: Boolean,
    onCancel: () -> Unit,
    bottomBar: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Scaffold(
        topBar = {
            HappyPiesAppBar(
                title = { Text(title) },
                onBackClick = onCancel,
                backButtonEnabled = cancelButtonEnabled
            )
        },
        bottomBar = bottomBar
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            content = content
        )
    }
}

@Composable
fun ReportFields(
    titleState: TextFieldState,
    descriptionState: TextFieldState,
    titleFieldErrorMessage: StringResource?,
    saveErrorMessage: StringResource?,
    enabled: Boolean
) {
    val colors = filledFieldColors()

    OutlinedTextField(
        state = titleState,
        label = { Text(stringResource(Res.string.report_form_title_label), fontWeight = FontWeight.Bold) },
        labelPosition = TextFieldLabelPosition.Above(),
        isError = titleFieldErrorMessage != null,
        supportingText = titleFieldErrorMessage?.let { error -> { Text(stringResource(error)) } },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        enabled = enabled,
        colors = colors,
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        state = descriptionState,
        label = { Text(stringResource(Res.string.report_form_description_label), fontWeight = FontWeight.Bold) },
        labelPosition = TextFieldLabelPosition.Above(),
        placeholder = { Text(stringResource(Res.string.report_form_description_placeholder)) },
        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 4),
        enabled = enabled,
        colors = colors,
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
    )

    saveErrorMessage?.let { error ->
        Text(text = stringResource(error), color = MaterialTheme.colorScheme.error)
    }
}

@Composable
fun ReportVideos(
    videos: List<ReportVideo>,
    addButtonEnabled: Boolean,
    removeEnabled: Boolean,
    pickerMaxItems: Int,
    errorMessage: StringResource?,
    onVideosPicked: (List<ReportVideo.Picked>) -> Unit,
    onRemoveClick: (ReportVideo) -> Unit
) {
    val launcher = rememberFilePickerLauncher(
        type = FileKitType.Video,
        mode = FileKitMode.Multiple(maxItems = pickerMaxItems)
    ) { files ->
        files?.let { onVideosPicked(it.map { file -> file.toPickedVideo() }) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(Res.string.report_form_videos_label, MAX_REPORT_VIDEOS), fontWeight = FontWeight.Bold)

        videos.forEachIndexed { index, video ->
            VideoRow(number = index + 1, sizeBytes = video.sizeBytes(), removeEnabled = removeEnabled, onRemoveClick = { onRemoveClick(video) })
        }

        OutlinedButton(onClick = { launcher.launch() }, enabled = addButtonEnabled) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Text(stringResource(Res.string.report_form_add_video), modifier = Modifier.padding(start = 8.dp))
        }

        errorMessage?.let { error ->
            Text(text = stringResource(error), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun VideoRow(number: Int, sizeBytes: Long, removeEnabled: Boolean, onRemoveClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.Movie, contentDescription = null)
        Text(
            text = stringResource(Res.string.report_form_video_item, number, formatMegabytes(sizeBytes)),
            modifier = Modifier.weight(1f).padding(start = 12.dp)
        )
        IconButton(onClick = onRemoveClick, enabled = removeEnabled) {
            Icon(Icons.Rounded.Close, contentDescription = stringResource(Res.string.report_form_remove_video))
        }
    }
}

private fun ReportVideo.sizeBytes(): Long = when (this) {
    is ReportVideo.Uploaded -> item.sizeBytes
    is ReportVideo.Picked -> sizeBytes
}

@Composable
fun SaveReportBar(saveStatus: SaveStatus, enabled: Boolean, onSaveClick: () -> Unit) {
    Box(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        when (saveStatus) {
            is SaveStatus.Uploading -> ProgressButton(
                text = stringResource(Res.string.report_form_uploading, saveStatus.percent),
                isLoading = false,
                enabled = false,
                onClick = onSaveClick
            )
            SaveStatus.Saving -> ProgressButton(
                text = stringResource(Res.string.report_form_save),
                isLoading = true,
                enabled = false,
                onClick = onSaveClick
            )
            SaveStatus.Idle, SaveStatus.Saved -> ProgressButton(
                text = stringResource(Res.string.report_form_save),
                isLoading = false,
                enabled = enabled,
                onClick = onSaveClick
            )
        }
    }
}