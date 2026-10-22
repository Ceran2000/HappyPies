package pl.ceranka.happypies.ui.common

import pl.ceranka.happypies.data.account.Account
import pl.ceranka.happypies.data.user.UserRole
import pl.ceranka.happypies.fakes.anna
import pl.ceranka.happypies.fakes.trainer
import kotlin.test.Test
import kotlin.test.assertEquals

class ClientSpacePeopleTest {

    private val signedInClient = Account(anna.id, anna.email, anna.name, UserRole.CLIENT)

    @Test
    fun theClientIsNamedFromTheClient() {
        assertEquals("Anna Kowalska", ClientSpacePeople(anna, trainer).nameOf(anna.id))
        assertEquals("Anna Kowalska", ClientSpacePeople(anna, signedInClient).nameOf(anna.id))
    }

    @Test
    fun anyoneElseIsTheTrainerNamedWhenTheTrainerIsSignedIn() {
        assertEquals("Jan Trener", ClientSpacePeople(anna, trainer).nameOf(trainer.uid))
        assertEquals("Trener", ClientSpacePeople(anna, signedInClient).nameOf(trainer.uid))
        assertEquals("Trener", ClientSpacePeople(anna, account = null).nameOf(trainer.uid))
    }
}