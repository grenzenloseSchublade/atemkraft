package app.atemkraft.ui.atemlehre

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import app.atemkraft.data.Atemlehre
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Sprung aus der Detailseite („… in der Atemlehre ›“): Die Karte steht aufgeklappt im Bild,
 * auch die letzte. Jede Karte einzeln, weil eine falsche Bezugsebene der Position (Karte in einer
 * eigenen Spalte) nur zu kurz scrollt: Die Karte wäre sichtbar, aber in der unteren Bildhälfte.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class AtemlehreFocusTest {

    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `Sprung oeffnet die Karte und scrollt sie ins Bild`() {
        val pattern = Atemlehre.patterns.last()
        rule.setContent {
            AtemkraftTheme { AtemlehreScreen(onBack = {}, onOpenExercise = {}, focus = pattern.id) }
        }
        rule.waitForIdle()
        // Die letzte Karte kann nicht ganz nach oben (darunter endet die Seite); sie muss nur
        // samt aufgeklapptem Inhalt im Bild stehen.
        rule.onNodeWithText(pattern.title).assertIsDisplayed()
        // Aufgeklappt: der Text „Lehre“ der Karte steht da und ist im Bild.
        rule.onNodeWithText(pattern.teaching).assertIsDisplayed()
    }

    @Test
    fun `Sprung zu einer Karte in der Mitte`() {
        val pattern = Atemlehre.pattern("halten-leer")!!
        rule.setContent {
            AtemkraftTheme { AtemlehreScreen(onBack = {}, onOpenExercise = {}, focus = pattern.id) }
        }
        rule.waitForIdle()
        rule.onNodeWithText(pattern.title).assertIsDisplayed()
        assertNearTop(pattern.title)
        rule.onNodeWithText(pattern.teaching).assertIsDisplayed()
    }

    /** Die Karte steht oben im Bild (oberes Viertel), nicht nur irgendwo sichtbar. */
    private fun assertNearTop(title: String) {
        val top = rule.onNodeWithText(title).fetchSemanticsNode().boundsInRoot.top
        val height = rule.onRoot().fetchSemanticsNode().boundsInRoot.height
        assertTrue("„$title“ steht bei $top von $height", top < height / 4)
    }
}
