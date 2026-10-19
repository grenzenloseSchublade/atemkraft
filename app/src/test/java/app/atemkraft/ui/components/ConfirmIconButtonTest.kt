package app.atemkraft.ui.components

import android.os.Looper
import android.view.accessibility.AccessibilityManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import app.atemkraft.R
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * Zweistufiges Löschen im selben Element (MUSTER-05): erst der zweite Tipp löscht, der
 * Bestätigungszustand wird angesagt und fällt nach 4 s zurück – nicht, solange ein
 * Bedienungshilfe-Dienst läuft, auch wenn er erst während der Anzeige startet.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ConfirmIconButtonTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()
    private val manager = app.getSystemService(AccessibilityManager::class.java)
    private var confirmed = 0

    private val idle get() = compose.onNodeWithContentDescription(app.getString(R.string.cd_voice_delete, NAME))
    private val armed get() = compose.onNodeWithContentDescription(app.getString(R.string.cd_delete_confirm, NAME))

    @Before
    fun setUp() {
        compose.setContent {
            AtemkraftTheme {
                ConfirmIconButton(
                    name = NAME,
                    description = app.getString(R.string.cd_voice_delete, NAME),
                    onConfirm = { confirmed++ },
                )
            }
        }
    }

    @Test
    fun `erster Tipp schärft und wird angesagt, erst der zweite löscht`() {
        idle
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
            .performClick()
        assertEquals(0, confirmed)
        armed.assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)).performClick()
        assertEquals(1, confirmed)
        idle.assertExists()
    }

    @Test
    fun `ohne zweiten Tipp fällt der Zustand nach 4 s zurück`() {
        idle.performClick()
        compose.mainClock.advanceTimeBy(3_500)
        armed.assertExists()
        compose.mainClock.advanceTimeBy(1_000)
        idle.assertExists()
        assertEquals(0, confirmed)
    }

    @Test
    fun `mit Bedienungshilfe-Dienst kein Rückfall, auch wenn er erst später startet`() {
        idle.performClick()
        compose.mainClock.advanceTimeBy(1_000)
        shadowOf(manager).setEnabled(true)
        // Der Dienst meldet sich über den Main-Looper (Listener-Handler).
        shadowOf(Looper.getMainLooper()).idle()
        compose.mainClock.advanceTimeBy(10_000)
        armed.assertExists()
        armed.performClick()
        assertEquals(1, confirmed)
    }

    private companion object {
        const val NAME = "Thorsten"
    }
}
