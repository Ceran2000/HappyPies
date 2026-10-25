package pl.ceranka.happypies.screen.report_form

import androidx.compose.foundation.text.input.TextFieldState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.account.AccountRepository
import pl.ceranka.happypies.data.account.accountOrNull
import pl.ceranka.happypies.ui.common.toUserMessage

abstract class ReportFormViewModel(private val accountRepository: AccountRepository) : ViewModel() {

    protected val log = Logger.withTag("ReportForm")

    val titleState = TextFieldState()
    val descriptionState = TextFieldState()

    protected val formState = MutableStateFlow(ReportFormUiState())

    /** Names the saved report in the error log, e.g. "report r1". */
    protected abstract val reportForLog: String

    fun onSave() {
        // Ignore taps while saving and after saving (no double submit).
        val form = formState.value
        if (!canSave() || form.isSaving || form.isSaved) return

        val title = titleState.text.toString().trim()
        if (title.isEmpty()) {
            formState.update { it.copy(titleError = "Podaj tytuł raportu", saveError = null) }
            return
        }
        val viewer = accountRepository.state.value.accountOrNull
        if (viewer == null) {
            formState.update { it.copy(titleError = null, saveError = "Sesja wygasła. Zaloguj się ponownie.") }
            return
        }
        val description = descriptionState.text.toString().trim().ifEmpty { null }

        if (!hasChanges(title, description)) {
            formState.update { it.copy(isSaved = true) }
            return
        }

        formState.update { it.copy(isSaving = true, titleError = null, saveError = null) }
        viewModelScope.launch {
            try {
                save(title, description, viewer)
                formState.update { it.copy(isSaving = false, isSaved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log.e(e) { "Saving $reportForLog failed" }
                formState.update { it.copy(isSaving = false, saveError = e.toUserMessage()) }
            }
        }
    }

    protected open fun canSave(): Boolean = true

    protected open fun hasChanges(title: String, description: String?): Boolean = true

    protected abstract suspend fun save(title: String, description: String?, viewer: Account)
}