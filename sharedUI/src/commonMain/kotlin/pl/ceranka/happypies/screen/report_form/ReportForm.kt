package pl.ceranka.happypies.screen.report_form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** The frame shared by adding and editing a report: "Anuluj", the screen title and the content below. */
@Composable
internal fun ReportFormLayout(
    title: String,
    cancelButtonEnabled: Boolean,
    onCancel: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onCancel, enabled = cancelButtonEnabled) {
                Text("Anuluj")
            }
            Text(text = title, style = MaterialTheme.typography.titleLarge)
        }
        content()
    }
}

@Composable
internal fun ReportFields(
    form: ReportFormUiState,
    titleState: TextFieldState,
    descriptionState: TextFieldState,
    onSave: () -> Unit
) {
    OutlinedTextField(
        state = titleState,
        label = { Text("Tytuł (wymagany)") },
        isError = form.titleError != null,
        supportingText = form.titleError?.let { error -> { Text(error) } },
        lineLimits = TextFieldLineLimits.SingleLine,
        enabled = form.fieldsEnabled,
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        state = descriptionState,
        label = { Text("Opis") },
        placeholder = { Text("Co ćwiczyliście i jak poszło?") },
        lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 4),
        enabled = form.fieldsEnabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp)
    )

    form.saveError?.let { error ->
        Text(text = error, color = MaterialTheme.colorScheme.error)
    }

    Button(onClick = onSave, enabled = form.saveButtonEnabled, modifier = Modifier.fillMaxWidth()) {
        if (form.showSavingProgress) {
            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(end = 8.dp).size(18.dp))
        }
        Text(form.saveButtonText)
    }
}