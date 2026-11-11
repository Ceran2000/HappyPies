package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import happypies.sharedui.generated.resources.Res
import happypies.sharedui.generated.resources.common_new_report
import org.jetbrains.compose.resources.stringResource
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

    ReportFormLayout(
        title = stringResource(Res.string.common_new_report),
        cancelButtonEnabled = uiState.cancelButtonEnabled,
        onCancel = onCancel,
        bottomBar = {
            SaveReportBar(saveStatus = uiState.saveStatus, enabled = uiState.saveButtonEnabled, onSaveClick = viewModel::onSaveClick)
        }
    ) {
        ReportFields(
            titleState = viewModel.titleState,
            descriptionState = viewModel.descriptionState,
            titleFieldErrorMessage = uiState.titleFieldErrorMessage,
            saveErrorMessage = uiState.saveErrorMessage,
            enabled = uiState.fieldsEnabled
        )
        ReportVideos(
            videos = uiState.videos,
            addButtonEnabled = uiState.addVideoButtonEnabled,
            removeEnabled = uiState.fieldsEnabled,
            pickerMaxItems = uiState.videoPickerMaxItems,
            errorMessage = uiState.videoErrorMessage,
            onVideosPicked = viewModel::onVideosPicked,
            onRemoveClick = viewModel::onRemoveVideoClick
        )
    }
}