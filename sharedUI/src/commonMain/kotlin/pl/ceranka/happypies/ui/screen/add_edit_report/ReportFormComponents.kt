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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldLabelPosition
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import pl.ceranka.happypies.ui.common.ProgressButton
import pl.ceranka.happypies.ui.common.filledFieldColors

@OptIn(ExperimentalMaterial3Api::class)
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
            CenterAlignedTopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onCancel, enabled = cancelButtonEnabled) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Wstecz")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
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
    titleFieldErrorMessage: String?,
    saveErrorMessage: String?,
    enabled: Boolean
) {
    val colors = filledFieldColors()

    OutlinedTextField(
        state = titleState,
        label = { Text("Tytuł (wymagany)", fontWeight = FontWeight.Bold) },
        labelPosition = TextFieldLabelPosition.Above(),
        isError = titleFieldErrorMessage != null,
        supportingText = titleFieldErrorMessage?.let { error -> { Text(error) } },
        lineLimits = TextFieldLineLimits.SingleLine,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        enabled = enabled,
        colors = colors,
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        state = descriptionState,
        label = { Text("Opis", fontWeight = FontWeight.Bold) },
        labelPosition = TextFieldLabelPosition.Above(),
        placeholder = { Text("Co ćwiczyliście i jak poszło?") },
        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 4),
        enabled = enabled,
        colors = colors,
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
    )

    saveErrorMessage?.let { error ->
        Text(text = error, color = MaterialTheme.colorScheme.error)
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
            text = "Zapisz raport",
            isLoading = isSaving,
            enabled = enabled,
            onClick = onSaveClick
        )
    }
}