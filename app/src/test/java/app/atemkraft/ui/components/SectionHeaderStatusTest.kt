package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
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
 * Trennpunkt dahinter; sonst darunter und ohne Punkt – keine Zeile beginnt mit „·“.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = TestMatrix.A54_QUALIFIERS)
@GraphicsMode(GraphicsMode.Mode.NATIVE) // echte Textmaße
class SectionHeaderStatusTest {

    @get:Rule
    val compose = createComposeRule()

    private fun show(width: Int) {
        compose.setContent {
            AtemkraftTheme {
                Box(Modifier.width(width.dp)) {
                    SectionHeader(
                        title = "Genau einstellen",
                        color = headingColor(2),
                        status = "· angepasst",
                        statusDescription = "angepasst",
                        smallStatus = true,
                    )
                }
            }
        }
    }

    @Test
    fun `genug Platz - Status mit Trennpunkt hinter dem Titel`() {
        show(width = 360)
        compose.onNodeWithContentDescription("angepasst").assert(hasText("· angepasst"))
    }

    @Test
    fun `zu schmal - Status darunter ohne Trennpunkt am Zeilenanfang`() {
        show(width = 170)
        compose.onNodeWithContentDescription("angepasst").assert(hasText("angepasst"))
    }
}
