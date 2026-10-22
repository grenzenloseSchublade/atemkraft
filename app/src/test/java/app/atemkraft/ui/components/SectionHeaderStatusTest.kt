package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.dp
import app.atemkraft.ui.TestMatrix
import app.atemkraft.ui.theme.AtemkraftTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Status hinter dem Titel eines [SectionHeader] (LAYOUT-03): Passt er in die Zeile, steht er mit
 * Trennpunkt dahinter; sonst darunter und ohne Punkt – keine Zeile beginnt mit „·“. Passt er
 * auch darunter nicht in eine Zeile, steht jeder Teil in einer eigenen Zeile.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE) // echte Textmaße
class SectionHeaderStatusTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `Status bricht nur zwischen Teilen um, nie mit Punkt am Zeilenanfang`() {
        var width by mutableIntStateOf(360)
        var status by mutableStateOf("· angepasst")
        var spoken by mutableStateOf("angepasst")
        compose.setContent {
            AtemkraftTheme {
                Box(Modifier.width(width.dp)) {
                    SectionHeader(
                        title = "Genau einstellen",
                        color = headingColor(2),
                        status = status,
                        statusDescription = spoken,
                        smallStatus = true,
                    )
                }
            }
        }
        // Genug Platz: Status mit Trennpunkt hinter dem Titel.
        compose.onNodeWithContentDescription("angepasst").assert(hasText("· angepasst"))

        // Zu schmal: Status darunter ohne Trennpunkt am Zeilenanfang.
        width = 170
        compose.onNodeWithContentDescription("angepasst").assert(hasText("angepasst"))

        // Darunter zu breit: jeder Teil in eigener Zeile, keine Zeile endet auf Punkt.
        width = 120
        status = "· 24\u00A0s\u00A0· 2,5\u00A0Atemzüge/min"
        spoken = "Atemzug"
        compose.onNodeWithContentDescription("Atemzug").assert(hasText("24\u00A0s\n2,5\u00A0Atemzüge/min"))
    }
}
