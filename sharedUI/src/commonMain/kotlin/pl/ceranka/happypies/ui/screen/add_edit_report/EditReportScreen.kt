package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.common_edit_report
import org.jetbrains.compose.resources.stringResource
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

    when (val state = uiState) {
        EditReportUiState.Loading -> LoadingContent(onCancel)
        is EditReportUiState.LoadError -> ErrorContent(state, onCancel, viewModel::onRetryClick)
        is EditReportUiState.Editing -> EditingContent(state, viewModel, onSaved, onCancel)
    }
}

@Composable
private fun LoadingContent(onCancel: () -> Unit) {
    ReportFormLayout(title = stringResource(Res.string.common_edit_report), cancelButtonEnabled = true, onCancel = onCancel, bottomBar = {}) {
        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
private fun ErrorContent(state: EditReportUiState.LoadError, onCancel: () -> Unit, onRetryClick: () -> Unit) {
    ReportFormLayout(title = stringResource(Res.string.common_edit_report), cancelButtonEnabled = true, onCancel = onCancel, bottomBar = {}) {
        ErrorWithRetry(stringResource(state.message), onRetry = onRetryClick)
    }
}

@Composable
private fun EditingContent(
    state: EditReportUiState.Editing,
    viewModel: EditReportViewModel,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    ReportFormLayout(
        title = stringResource(Res.string.common_edit_report),
        cancelButtonEnabled = state.cancelButtonEnabled,
        onCancel = onCancel,
        bottomBar = {
            SaveReportBar(saveStatus = state.saveStatus, enabled = state.saveButtonEnabled, onSaveClick = viewModel::onSaveClick)
        }
    ) {
        ReportFields(
            titleState = viewModel.titleState,
            descriptionState = viewModel.descriptionState,
            titleFieldErrorMessage = state.titleFieldErrorMessage,
            saveErrorMessage = state.saveErrorMessage,
            enabled = state.fieldsEnabled
        )
        ReportVideos(
            videos = state.videos,
            addButtonEnabled = state.addVideoButtonEnabled,
            removeEnabled = state.fieldsEnabled,
            pickerMaxItems = state.videoPickerMaxItems,
            errorMessage = state.videoErrorMessage,
            onVideosPicked = viewModel::onVideosPicked,
            onRemoveClick = viewModel::onRemoveVideoClick
        )
    }
}