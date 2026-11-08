package pl.ceranka.happypies.ui.screen.client_home

import pl.ceranka.happypies.data.client.Dog
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.annasTrainer
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientHomeUiStateTest {

    @Test
    fun namesAllDogs() {
        val client = anna.copy(dogs = listOf(Dog("Bala", "border collie"), Dog("Rex", "mieszaniec"), Dog("Fuks", "jamnik")))

        assertEquals(listOf("Bala", "Rex", "Fuks"), ClientHomeUiState.ShowContent(client, annasTrainer).dogNames)
    }

    @Test
    fun takesFirstNameAndInitialsFromTwoWords() {
        val state = ClientHomeUiState.ShowContent(anna.copy(name = "  maria anna nowak "), annasTrainer)

        assertEquals("maria", state.firstName)
        assertEquals("MA", state.initials)
    }
}