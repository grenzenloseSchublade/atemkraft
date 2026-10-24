package app.atemkraft.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import app.atemkraft.R
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.screenshots.BreatheTab
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Befindens-Suche im Atmen-Tab (MUSTER-09, Nutzer 2026-10-24): Die Eingabe steht im Feld, auch
 * wenn die Tastatur ohne „Suchen“ schließt oder das Feld den Fokus verliert – Feld und Treffer
 * zeigen denselben Text. Zurück lässt mit Fokus im Feld die Eingabe stehen (es nimmt nur den
 * Fokus), erst ohne Fokus leert es Wahl und Suche.
 *
 * Fokus weg heißt hier `moveFocus`: Robolectric läuft nicht im Touch-Modus, dort holt sich die
 * View nach `clearFocus()` den Fokus sofort zurück und Compose stellt ihn im Feld wieder her.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class HomeSearchTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `Eingabe bleibt im Feld ohne Tastatur und Fokus, Zurück leert erst ohne Fokus`() {
        lateinit var focusManager: FocusManager
        lateinit var keyboard: SoftwareKeyboardController
        compose.setContent {
            AtemkraftTheme {
                focusManager = LocalFocusManager.current
                keyboard = checkNotNull(LocalSoftwareKeyboardController.current)
                BreatheTab()
            }
        }
        val activity = compose.activity
        val field = compose.onNode(hasSetTextAction())
        // Das X gibt es nur mit Eingabe: Es zeigt, dass die Suche den Text hält.
        val clear = compose.onNodeWithContentDescription(activity.getString(R.string.cd_search_clear))

        field.performClick()
        field.performTextInput("Prüfung")
        field.assertIsFocused()
        clear.assertExists()

        // Tastatur schließen ohne „Suchen“ und Fokus verlieren: Text und Treffer bleiben.
        compose.runOnIdle { keyboard.hide() }
        field.assert(hasText("Prüfung"))
        compose.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
        field.assertIsNotFocused()
        field.assert(hasText("Prüfung"))
        clear.assertExists()

        // Zurück mit Fokus im Feld lässt die Eingabe stehen.
        field.performClick()
        field.assertIsFocused()
        compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        field.assert(hasText("Prüfung"))
        clear.assertExists()

        // Zurück ohne Fokus leert Wahl und Suche wie bisher.
        compose.runOnIdle { focusManager.moveFocus(FocusDirection.Next) }
        field.assertIsNotFocused()
        compose.runOnIdle { activity.onBackPressedDispatcher.onBackPressed() }
        clear.assertDoesNotExist()
        field.assert(!hasText("Prüfung"))
    }
}
