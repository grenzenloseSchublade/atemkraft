package app.atemkraft.ui.patterns

import app.atemkraft.data.PatternVersion
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.PatternSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Speichern im Muster-Editor (MUSTER-14): Ein geänderter Entwurf fragt beim Verlassen nach, ein
 * unveränderter (auch ein Tagesmuster) und ein gespeichertes Muster nicht; ein bestehendes Muster
 * schreibt jede Änderung und gibt nur beim ersten Schreiben einer Sitzung den Öffnungsstand als
 * vorherige Fassung mit.
 */
class PatternEditorSaveTest {

    private val box = FreePatternTemplates.all.single { it.label == "Box" }.pattern

    @Test
    fun `Zurueck fragt nur bei einem geaenderten Entwurf`() {
        val draft = PatternEditorState.new("Eigenes Muster")
        assertFalse(draft.unsavedChanges)
        draft.setSeconds(0, 5)
        assertTrue(draft.unsavedChanges)
        // Zurück auf den Öffnungsstand: keine Änderung mehr.
        draft.setSeconds(0, 4)
        assertFalse(draft.unsavedChanges)
        draft.minutes = 9
        assertTrue(draft.unsavedChanges)
        // Diskette getippt: bis das Schreiben zurückmeldet, fragt Zurück nicht; danach gespeichert.
        draft.markSaving()
        assertFalse(draft.unsavedChanges)
        draft.markSaved(1L, draft.draft())
        assertFalse(draft.unsavedChanges)
        assertTrue(draft.saved)
        assertTrue(draft.announceSaved)

        // Ein unverändertes Tagesmuster ist keine Änderung.
        val daily = PatternEditorState.fromDaily("Eigenes Muster", PatternSpec(4.0, 2.0, 6.0, null, false))
        assertFalse(daily.unsavedChanges)
        assertNull(daily.savedId)
    }

    @Test
    fun `bestehendes Muster schreibt jede Aenderung, die vorherige Fassung nur beim ersten Mal`() {
        val state = PatternEditorState.forEdit("Eigenes Muster", 7L, box, 5, "Abendwelle")
        val opened = PatternVersion(box, 5, "Abendwelle")
        assertFalse(state.unsavedChanges)
        assertFalse(state.needsWrite)
        assertTrue(state.saved)

        state.setSeconds(1, 6)
        assertTrue(state.needsWrite)
        assertFalse(state.saved)
        assertFalse(state.unsavedChanges)
        assertEquals(opened, state.previousToKeep())
        state.markSaved(7L, state.draft())
        assertTrue(state.announceSaved)

        state.reset()
        assertTrue(state.needsWrite)
        assertNull(state.previousToKeep())
        state.markSaved(7L, state.draft())
        // Nur das erste Schreiben einer Sitzung sagt TalkBack an.
        assertFalse(state.announceSaved)
        assertTrue(state.saved)

        // Ein in dieser Sitzung angelegtes Muster hat keine vorherige Fassung.
        val created = PatternEditorState.new("Eigenes Muster")
        created.markSaved(8L, created.draft())
        created.setSeconds(0, 6)
        assertNull(created.previousToKeep())
    }
}
