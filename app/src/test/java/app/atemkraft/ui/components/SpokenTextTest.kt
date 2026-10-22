package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performSemanticsAction
import app.atemkraft.R
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * A11Y-06/-01: Was TalkBack statt der sichtbaren Kurzform hört – Dauer ausgeschrieben
 * („6 Minuten 5 Sekunden“ statt „6:05“), Stepper-Wert mit Einheit („Einatmen: 4 Sekunden“ statt
 * „Einatmen (s): 4“), Schalterzeile mit Rolle und Zustand – und dass Kartentitel Überschriften sind.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
class SpokenTextTest {

    @get:Rule
    val compose = createComposeRule()

    private val app = RuntimeEnvironment.getApplication()

    /** Je Dauer die Ansage; ein setContent pro Test. */
    private fun spoken(vararg durationsMs: Long): List<String> {
        val result = mutableListOf<String>()
        compose.setContent { durationsMs.forEach { result += spokenDuration(it) } }
        compose.waitForIdle()
        return result
    }

    @Test
    fun `Dauer wird mit Einheiten ausgeschrieben, ohne Nullteil`() {
        assertEquals(
            listOf("6 Minuten 5 Sekunden", "1 Minute 1 Sekunde", "10 Minuten"),
            spoken(365_000L, 61_000L, 600_000L),
        )
    }

    /**
     * Was TalkBack an Stepper, Schalterzeile und Kartentitel liest (zusammengeführter Baum): Knöpfe
     * mit Name, Rolle und Disabled am Anschlag, der Wert mit Einheit ohne Klammer als Live-Region;
     * die Schalterzeile als Switch mit Name, Zustand und Hinweis; der Kartentitel als Überschrift.
     */
    @Test
    fun `Komponenten-Semantik - Stepper, Schalterzeile und Kartentitel`() {
        var value by mutableIntStateOf(1)
        var on by mutableStateOf(false)
        compose.setContent {
            AtemkraftTheme {
                Column {
                    CardTitle("Ton")
                    Stepper(
                        label = app.getString(R.string.adjust_inhale),
                        value = value,
                        range = 1..10,
                        onChange = { value = it },
                        spokenLabel = app.getString(R.string.phase_inhale),
                        valueUnit = R.plurals.cd_duration_seconds,
                    )
                    ToggleRow(label = "Vibration", checked = on, onCheckedChange = { on = it }, hint = "Hinweis")
                    ToggleRow(label = "Sprachanleitung", checked = false, onCheckedChange = {}, enabled = false)
                }
            }
        }
        compose.onNodeWithText("Ton").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))

        val minus = "Einatmen, ${app.getString(R.string.adjust_decrease)}"
        val plus = "Einatmen, ${app.getString(R.string.adjust_increase)}"
        compose.onNodeWithContentDescription(minus)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assertIsNotEnabled()
        compose.onNodeWithContentDescription(plus).assertIsEnabled().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNodeWithContentDescription("Einatmen: 2 Sekunden")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite))
        compose.onNodeWithContentDescription(minus).assertIsEnabled()
        // Sichtbar bleibt das kurze Label mit Einheit.
        compose.onNodeWithText(app.getString(R.string.adjust_inhale)).assertExists()

        val switch = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch)
        val row = compose.onNode(switch and hasText("Vibration"))
        row.assertIsOff().assert(hasText("Hinweis"))
        row.performSemanticsAction(SemanticsActions.OnClick)
        row.assertIsOn()
        compose.onNode(switch and hasText("Sprachanleitung")).assertIsNotEnabled()
    }
}
