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

    /** Kernregel: nie Wortumbruch – Überschriften brechen nur zwischen Wörtern, keine Rolle trennt Silben. */
    @Test
    fun `Ueberschriften brechen nur zwischen Woertern, keine Rolle trennt Silben`() {
        headingRoles.forEach { (name, style) ->
            assertEquals("$name: lineBreak", LineBreak.Heading, style.lineBreak)
            assertEquals("$name: hyphens", Hyphens.None, style.hyphens)
        }
        roles(AtemkraftTypography).forEach { (name, style) -> assertNotEquals("$name: hyphens", Hyphens.Auto, style.hyphens) }
    }

    /** Nutzerentscheidung 2026-10-19: Material-3-Größen ohne eigenen Faktor, nur Überschriften kräftiger. */
    @Test
    fun `Rollen sind Material 3, nur das Gewicht der Ueberschriften ist ueberschrieben`() {
        val m3 = Typography()
        roles(m3).zip(roles(AtemkraftTypography)).forEach { (expected, actual) ->
            assertEquals("${expected.first}: fontSize", expected.second.fontSize, actual.second.fontSize)
            assertEquals("${expected.first}: lineHeight", expected.second.lineHeight, actual.second.lineHeight)
            assertEquals("${expected.first}: letterSpacing", expected.second.letterSpacing, actual.second.letterSpacing)
        }
        val weights = mapOf(
            "headlineLarge" to FontWeight.Bold,
            "headlineMedium" to FontWeight.Bold,
            "headlineSmall" to FontWeight.SemiBold,
            "titleLarge" to FontWeight.SemiBold,
            "titleMedium" to FontWeight.SemiBold,
            "titleSmall" to m3.titleSmall.fontWeight,
        )
        weights.forEach { (name, weight) -> assertEquals("$name: fontWeight", weight, headingRoles.getValue(name).fontWeight) }
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
