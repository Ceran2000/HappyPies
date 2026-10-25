package pl.ceranka.happypies.screen.report_form

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import pl.ceranka.happypies.ui.common.ErrorWithRetry

@Composable
fun EditReportScreen(
    reportId: String,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    val viewModel = koinViewModel<EditReportViewModel> { parametersOf(reportId) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.form.isSaved) {
        if (uiState.form.isSaved) onSaved()
    }

    ReportFormLayout(title = "Edytuj raport", cancelButtonEnabled = uiState.form.cancelButtonEnabled, onCancel = onCancel) {
        if (uiState.showLoadingProgress) CircularProgressIndicator()
        uiState.loadError?.let { ErrorWithRetry(it, onRetry = viewModel::onRetry) }
        if (uiState.showForm) ReportFields(uiState.form, viewModel.titleState, viewModel.descriptionState, onSave = viewModel::onSave)
    }
}