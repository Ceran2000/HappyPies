package pl.ceranka.happypies.ui.screen.add_edit_report

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ReportFormFieldsTest {

    private val reportFormFields = ReportFormFields()

    private fun fill(title: String, description: String = "") {
        reportFormFields.titleState.setTextAndPlaceCursorAtEnd(title)
        reportFormFields.descriptionState.setTextAndPlaceCursorAtEnd(description)
    }

    @Test
    fun valuesAreTrimmed() {
        fill(title = "  Przywołanie ", description = " Trzy próby  ")

        assertEquals("Przywołanie", reportFormFields.title)
        assertEquals("Trzy próby", reportFormFields.description)
    }

    @Test
    fun blankTitleBecomesEmpty() {
        fill(title = "   ", description = "Trzy próby")

        assertEquals("", reportFormFields.title)
    }

    @Test
    fun blankDescriptionBecomesNull() {
        fill(title = "Przywołanie", description = "   ")

        assertNull(reportFormFields.description)
    }
}