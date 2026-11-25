package app.atemkraft.domain

import app.atemkraft.data.Areas
import app.atemkraft.data.BuiltInExercises
import app.atemkraft.data.Pranayama
import app.atemkraft.data.Situations
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suche nach Namen (Konzeptprüfung 2026-11-25): Wer eine Übung beim Namen sucht, bekommt ihre Karte –
 * auch mit Ziffern oder der Übersetzung in Klammern; „Pranayama“ führt zu den Übungen aus dem Yoga.
 * Befindens-Wörter treffen weiter nur Anlässe, Programme ohne Start nie eine Karte.
 */
class SituationSearchNameTest {

    private val guided = BuiltInExercises.all.filter { it.guided }

    private fun search(query: String) = SituationSearch.search(query, Situations.all, Areas.all, guided, Pranayama.exerciseIds)

    private fun names(query: String) = search(query)?.exercises.orEmpty().map { it.id }

    @Test
    fun `Namen, Ziffern und Uebersetzungen treffen die Karte, Befinden bleibt bei den Anlaessen`() {
        assertEquals(listOf("box-4-4-4-4"), names("Box"))
        assertEquals(listOf("4-7-8"), names("4-7-8"))
        assertEquals(listOf("4-7-8"), names("478"))
        assertEquals(listOf("nadi-shodhana"), names("Wechselatmung"))
        assertEquals(listOf("feueratmung"), names("kapal"))
        assertEquals(listOf("ujjayi"), names("Ujjayi"))

        val yoga = search("Prāṇāyāma")!!
        assertTrue(yoga.yoga)
        assertEquals(guided.filter { it.id in Pranayama.exerciseIds }.map { it.id }, yoga.exercises.map { it.id })

        // Befinden: keine Karte über den Namen, der Anlass bleibt.
        val tired = search("müde")!!
        assertTrue(tired.exercises.isEmpty())
        assertFalse(tired.situations.isEmpty())
        // Programme haben keinen Start: Buteyko bleibt beim Anlass „Kurzatmig“.
        assertTrue(names("Buteyko").isEmpty())
    }
}
