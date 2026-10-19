package app.atemkraft.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * TYPO-03: Überschriften und Titel brechen nur zwischen Wörtern, ohne Silbentrennung. Dazu §3:
 * Alle Rollen haben Material-3-Größe (kein eigener Faktor, Nutzerentscheidung 2026-10-19);
 * überschrieben ist nur das Gewicht der Überschriften.
 */
class TypographyTest {

    private val headingRoles: Map<String, TextStyle> = with(AtemkraftTypography) {
        mapOf(
            "headlineLarge" to headlineLarge,
            "headlineMedium" to headlineMedium,
            "headlineSmall" to headlineSmall,
            "titleLarge" to titleLarge,
            "titleMedium" to titleMedium,
            "titleSmall" to titleSmall,
        )
    }

    @Test
    fun `jede Ueberschrift-Rolle bricht nur zwischen Woertern`() {
        headingRoles.forEach { (name, style) ->
            assertEquals("$name: lineBreak", LineBreak.Heading, style.lineBreak)
            assertEquals("$name: hyphens", Hyphens.None, style.hyphens)
        }
    }

    @Test
    fun `keine Rolle schaltet Silbentrennung ein`() {
        with(AtemkraftTypography) {
            listOf(
                displayLarge, displayMedium, displaySmall, headlineLarge, headlineMedium, headlineSmall,
                titleLarge, titleMedium, titleSmall, bodyLarge, bodyMedium, bodySmall,
                labelLarge, labelMedium, labelSmall,
            )
        }.forEach { assertNotEquals(Hyphens.Auto, it.hyphens) }
    }

    @Test
    fun `alle Rollen haben Material-Groesse`() {
        val m3 = Typography()
        roles(m3).zip(roles(AtemkraftTypography)).forEach { (expected, actual) ->
            assertEquals("${expected.first}: fontSize", expected.second.fontSize, actual.second.fontSize)
            assertEquals("${expected.first}: lineHeight", expected.second.lineHeight, actual.second.lineHeight)
            assertEquals("${expected.first}: letterSpacing", expected.second.letterSpacing, actual.second.letterSpacing)
        }
    }

    @Test
    fun `Ueberschriften sind kraeftiger als Material`() {
        val expected = mapOf(
            "headlineLarge" to FontWeight.Bold,
            "headlineMedium" to FontWeight.Bold,
            "headlineSmall" to FontWeight.SemiBold,
            "titleLarge" to FontWeight.SemiBold,
            "titleMedium" to FontWeight.SemiBold,
            "titleSmall" to Typography().titleSmall.fontWeight,
        )
        expected.forEach { (name, weight) -> assertEquals("$name: fontWeight", weight, headingRoles.getValue(name).fontWeight) }
    }

    private fun roles(t: Typography): List<Pair<String, TextStyle>> = with(t) {
        listOf(
            "displayLarge" to displayLarge, "displayMedium" to displayMedium, "displaySmall" to displaySmall,
            "headlineLarge" to headlineLarge, "headlineMedium" to headlineMedium, "headlineSmall" to headlineSmall,
            "titleLarge" to titleLarge, "titleMedium" to titleMedium, "titleSmall" to titleSmall,
            "bodyLarge" to bodyLarge, "bodyMedium" to bodyMedium, "bodySmall" to bodySmall,
            "labelLarge" to labelLarge, "labelMedium" to labelMedium, "labelSmall" to labelSmall,
        )
    }
}
