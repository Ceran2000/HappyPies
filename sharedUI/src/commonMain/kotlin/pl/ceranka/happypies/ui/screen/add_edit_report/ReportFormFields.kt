package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.foundation.text.input.TextFieldState

const val TITLE_REQUIRED_MESSAGE = "Podaj tytuł raportu"
const val SESSION_EXPIRED_MESSAGE = "Sesja wygasła. Zaloguj się ponownie."

class ReportFormFields {
    val titleState = TextFieldState()
    val descriptionState = TextFieldState()

    val title: String get() = titleState.text.toString().trim()
    val description: String? get() = descriptionState.text.toString().trim().ifEmpty { null }
}