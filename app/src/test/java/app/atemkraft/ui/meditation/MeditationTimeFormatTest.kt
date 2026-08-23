package app.atemkraft.ui.meditation

import org.junit.Assert.assertEquals
import org.junit.Test

/** m:ss-Formatierung der Meditations-Zeiten (geteilt von Screen, Mini-Leiste und Service). */
class MeditationTimeFormatTest {

    @Test
    fun `formatiert Minuten und Sekunden zweistellig`() {
        assertEquals("0:00", formatMeditationTime(0))
        assertEquals("0:05", formatMeditationTime(5_400)) // abgerundet auf volle Sekunden
        assertEquals("1:00", formatMeditationTime(60_000))
        assertEquals("90:00", formatMeditationTime(90 * 60_000L))
    }

    @Test
    fun `negative Werte werden als Null-Zeit angezeigt`() {
        assertEquals("0:00", formatMeditationTime(-1_000))
    }
}
