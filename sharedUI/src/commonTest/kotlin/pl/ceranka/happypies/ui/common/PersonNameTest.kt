package pl.ceranka.happypies.ui.common

import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.trainer
import kotlin.test.Test
import kotlin.test.assertEquals

class PersonNameTest {

    @Test
    fun resolvesTheClientAndTheSignedInPersonThenFallsBackToTheRole() {
        assertEquals("Anna Kowalska", personName(anna.id, UserRole.CLIENT, anna, account = null))
        assertEquals("Jan Trener", personName(trainer.uid, UserRole.TRAINER, anna, trainer))
        assertEquals("Trener", personName("someone", UserRole.TRAINER, anna, account = null))
        assertEquals("Klient", personName("someone", UserRole.CLIENT, anna, account = null))
    }
}