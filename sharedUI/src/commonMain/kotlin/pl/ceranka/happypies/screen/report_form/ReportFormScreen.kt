package pl.ceranka.happypies.screen.report_form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun ReportFormScreen(
    clientId: String,
    onSaved: () -> Unit,
    onCancel: () -> Unit,
) {
    val viewModel = koinViewModel<ReportFormViewModel> { parametersOf(clientId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState) {
        if (uiState == ReportFormUiState.Saved) onSaved()
    }

    ReportFormContent(
        uiState = uiState,
        titleState = viewModel.titleState,
        descriptionState = viewModel.descriptionState,
        onSave = viewModel::onSave,
        onCancel = onCancel,
    )
}

@Composable
private fun ReportFormContent(
    uiState: ReportFormUiState,
    titleState: TextFieldState,
    descriptionState: TextFieldState,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val isSaving = uiState !is ReportFormUiState.Editing
    val editing = uiState as? ReportFormUiState.Editing

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            TextButton(onClick = onCancel, enabled = !isSaving) {
                Text("Anuluj")
            }
            Text(text = "Nowy raport", style = MaterialTheme.typography.titleLarge)
        }

        OutlinedTextField(
            state = titleState,
            label = { Text("Tytuł (wymagany)") },
            isError = editing?.titleError != null,
            supportingText = editing?.titleError?.let { error -> { Text(error) } },
            lineLimits = TextFieldLineLimits.SingleLine,
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth(),
        )

        OutlinedTextField(
            state = descriptionState,
            label = { Text("Opis") },
            placeholder = { Text("Co ćwiczyliście i jak poszło?") },
            lineLimits = TextFieldLineLimits.MultiLine(minHeightInLines = 4),
            enabled = !isSaving,
            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
        )

        editing?.saveError?.let { error ->
            Text(text = error, color = MaterialTheme.colorScheme.error)
        }

        Button(onClick = onSave, enabled = !isSaving, modifier = Modifier.fillMaxWidth()) {
            if (isSaving) {
                CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.padding(end = 8.dp).size(18.dp))
            }
            Text(if (isSaving) "Zapisywanie…" else "Zapisz raport")
        }
    }
}