package pl.ceranka.happypies.ui.screen.client_home

import pl.ceranka.happypies.data.client.Dog
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.annasTrainer
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientHomeUiStateTest {

    @Test
    fun namesAllDogsInTheSubtitle() {
        val client = anna.copy(dogs = listOf(Dog("Bala", "border collie"), Dog("Rex", "mieszaniec"), Dog("Fuks", "jamnik")))

        assertEquals("Bala · Rex · Fuks · trener Jan Trener", ClientHomeUiState.ShowContent(client, annasTrainer).subtitle)
    }

    @Test
    fun withoutDogsTheSubtitleNamesOnlyTheTrainer() {
        assertEquals("trener Jan Trener", ClientHomeUiState.ShowContent(anna.copy(dogs = emptyList()), annasTrainer).subtitle)
    }

    @Test
    fun greetsByFirstNameAndTakesInitialsFromTwoWords() {
        val state = ClientHomeUiState.ShowContent(anna.copy(name = "  maria anna nowak "), annasTrainer)

        assertEquals("Cześć, maria", state.greeting)
        assertEquals("MA", state.initials)
    }
}