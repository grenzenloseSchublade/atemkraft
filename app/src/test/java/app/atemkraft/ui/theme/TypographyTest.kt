package app.atemkraft.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.Hyphens
import androidx.compose.ui.text.style.LineBreak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/** TYPO-03: Überschriften und Titel brechen nur zwischen Wörtern, ohne Silbentrennung. */
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
}
