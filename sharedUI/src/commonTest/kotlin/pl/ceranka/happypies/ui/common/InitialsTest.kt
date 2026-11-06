package pl.ceranka.happypies.ui.common

import kotlin.test.Test
import kotlin.test.assertEquals

class InitialsTest {

    @Test
    fun takesTheFirstLettersOfTwoWords() {
        assertEquals("AK", initialsOf("Anna Kowalska"))
    }

    @Test
    fun ignoresWordsAfterTheSecond() {
        assertEquals("MA", initialsOf("Maria Anna Nowak"))
    }

    @Test
    fun aSingleWordGivesOneLetter() {
        assertEquals("A", initialsOf("Anna"))
    }

    @Test
    fun trimsSpacesAndCapitalizes() {
        assertEquals("AK", initialsOf("  anna   kowalska "))
    }
}