package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.report_form_description_label
import happypies.sharedui.generated.resources.report_form_description_placeholder
import happypies.sharedui.generated.resources.report_form_save
import happypies.sharedui.generated.resources.report_form_title_label
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import pl.ceranka.happypies.ui.common.HappyPiesAppBar
import pl.ceranka.happypies.ui.common.ProgressButton
import pl.ceranka.happypies.ui.common.filledFieldColors

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
fun SaveReportBar(isSaving: Boolean, enabled: Boolean, onSaveClick: () -> Unit) {
    Box(
        modifier = Modifier
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        ProgressButton(
            text = stringResource(Res.string.report_form_save),
            isLoading = isSaving,
            enabled = enabled,
            onClick = onSaveClick
        )
    }
}