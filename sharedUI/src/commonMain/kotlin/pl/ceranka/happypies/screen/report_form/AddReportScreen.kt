package pl.ceranka.happypies.screen.report_form

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AddReportScreen(
    clientId: String,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val viewModel = koinViewModel<AddReportViewModel> { parametersOf(clientId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isSaved) {
        if (uiState.isSaved) onSaved()
    }

    ReportFormLayout(title = "Nowy raport", cancelButtonEnabled = uiState.cancelButtonEnabled, onCancel = onCancel) {
        ReportFields(uiState, viewModel.titleState, viewModel.descriptionState, onSave = viewModel::onSave)
    }
}