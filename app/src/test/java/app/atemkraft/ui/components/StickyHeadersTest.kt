package app.atemkraft.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Rechnung der klebenden Überschriften (Eigentümer 2026-10-31): Stehen bleiben, Unterordnen,
 * Hinausschieben und der Rückfall bei zu hohem Stapel – so, wie der Atmen-Tab sie nutzt
 * („Übungen“ über den Familien).
 */
class StickyHeadersTest {
    private fun header(name: String, level: Int, y: Int, height: Int = 40) = StickyHeader(name, level, order = 0L, y = y, height = height)

    /** Reihenfolge wie in der Liste: [y] von oben nach unten. */
    private fun page(vararg headers: StickyHeader<String>) = headers.mapIndexed { i, h -> StickyHeader(h.value, h.level, i.toLong(), h.y, h.height) }

    private fun pinned(viewport: Int = 2000, vararg headers: StickyHeader<String>) = pinHeaders(page(*headers), viewport).map { it.value to it.y }

    @Test
    fun headersStickAtTheirLineAndArePushedOut() {
        // Noch nichts oben angekommen: nichts klebt.
        assertEquals(emptyList<Pair<String, Int>>(), pinned(headers = arrayOf(header("Übungen", 1, 10), header("Beruhigen", 2, 120))))
        // „Übungen“ ist oben angekommen und steht bei 0; „Beruhigen“ hat seine Linie (40) noch nicht erreicht.
        assertEquals(listOf("Übungen" to 0), pinned(headers = arrayOf(header("Übungen", 1, -5), header("Beruhigen", 2, 60))))
        // Beide stehen: „Beruhigen“ hängt unter „Übungen“.
        assertEquals(
            listOf("Übungen" to 0, "Beruhigen" to 40),
            pinned(headers = arrayOf(header("Übungen", 1, -500), header("Beruhigen", 2, -300), header("Aktivieren", 2, 400))),
        )
        // „Aktivieren“ schiebt „Beruhigen“ hinaus: 60 − 40 = 20, Pixel für Pixel.
        assertEquals(
            listOf("Übungen" to 0, "Beruhigen" to 20),
            pinned(headers = arrayOf(header("Übungen", 1, -500), header("Beruhigen", 2, -300), header("Aktivieren", 2, 60))),
        )
        // Danach steht „Aktivieren“; eine Gruppe vor dem Abschnitt klebt nie unter ihm.
        assertEquals(
            listOf("Übungen" to 0, "Aktivieren" to 40),
            pinned(headers = arrayOf(header("Vorher", 2, -900), header("Übungen", 1, -500), header("Beruhigen", 2, -300), header("Aktivieren", 2, 30))),
        )
    }

    @Test
    fun tooTallStackKeepsOnlyTheDeepestLevel() {
        // Große Schrift: 2 × 200 von 1000 sind mehr als 30 % – nur die Gruppe klebt, am oberen Rand.
        assertEquals(
            listOf("Beruhigen" to 0),
            pinned(1000, header("Übungen", 1, -900, 200), header("Beruhigen", 2, -500, 200), header("Aktivieren", 2, 800, 200)),
        )
    }
}
