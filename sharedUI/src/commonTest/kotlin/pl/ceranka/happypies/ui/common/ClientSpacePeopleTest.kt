package pl.ceranka.happypies.ui.common

import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.annasTrainer
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientSpacePeopleTest {

    private val people = ClientSpacePeople(anna, annasTrainer)

    @Test
    fun theClientIsNamedFromTheClient() {
        assertEquals("Anna Kowalska", people.nameOf(anna.id))
    }

    @Test
    fun theTrainerIsNamedFromTheTrainer() {
        assertEquals("Jan Trener", people.nameOf(annasTrainer.id))
    }
}