package app.atemkraft.ui.theme

import androidx.compose.material3.Typography
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * LAYOUT-04: Die Optik der Bedienelemente folgt der Schrift. Für jedes Größen-Token und jede
 * Systemschrift der Screenshot-Matrix bleibt das Verhältnis Optik zu Schrift nahe an der
 * Material-3-Vorgabe (Button 40 dp zu labelLarge 14 sp):
 * - Obergrenze: höchstens 10 % größer im Verhältnis zur Schrift als bei M3 (sonst wirken die
 *   Elemente wie vor der Umstellung „aufgebläht“),
 * - Untergrenze: nie mehr als 10 % kleiner als die Schrift es verlangt und nie unter 80 % von M3
 *   (sonst bei großer Systemschrift „verloren“ neben dem Text).
 */
class SizesTest {

    private val fontScales = listOf(1.0f, 1.1f, 1.3f, 2.0f)

    // Schriftfaktor gegenüber M3, gemessen an der Beschriftung der Buttons (labelLarge).
    private val textScale = AtemkraftTypography.labelLarge.fontSize.value / Typography().labelLarge.fontSize.value

    @Test
    fun `jedes Token steht im Material-Verhaeltnis zur Schrift`() {
        for (fs in fontScales) {
            val text = textScale * fs
            val k = controlScale(fs)
            for (token in ControlSize.entries) {
                val dp = token.at(k).value
                val relative = dp / token.m3 / text
                assertTrue("${token.name} bei fs $fs: $dp dp ist im Verhältnis zur Schrift zu groß ($relative)", relative <= 1.1f)
                val lower = token.m3 * minOf(1f, text) * 0.9f
                assertTrue("${token.name} bei fs $fs: $dp dp ist kleiner als $lower dp", dp >= lower)
                assertTrue("${token.name} bei fs $fs: $dp dp unter 80 % von M3", dp >= token.m3 * 0.8f - 0.5f)
            }
        }
    }

    @Test
    fun `der Faktor waechst mit der Systemschrift und endet bei Material-Groesse`() {
        val ks = fontScales.map { controlScale(it) }
        assertTrue("monoton: $ks", ks.zipWithNext().all { (a, b) -> a <= b })
        assertTrue("fs 2,0 = M3: $ks", ks.last() == 1f)
    }
}
