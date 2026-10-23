package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Density
import app.atemkraft.ui.theme.Dimens

/**
 * Marke einer Atemphase in [color] (FARBE-04): Ein- und Ausatmen gefüllt, Halten ([dashed]) als
 * gestrichelter Umriss ([HoldDash]), nach innen versetzt, damit er so breit ist wie eine gefüllte
 * Marke. Die Größe gibt [modifier] vor (Ablauf-Liste des Editors: schmaler Strich; Atemlehre:
 * Abschnitt des Rhythmusbilds). Rein visuell – den Wert trägt der Text daneben.
 */
@Composable
fun PhaseMark(color: Color, dashed: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.drawBehind {
            val radius = CornerRadius(Dimens.GapHairline.toPx())
            if (!dashed) {
                drawRoundRect(color = color, cornerRadius = radius)
            } else {
                val stroke = Dimens.GapHairline.toPx()
                drawRoundRect(
                    color = color,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = Size(size.width - stroke, size.height - stroke),
                    cornerRadius = radius,
                    style = Stroke(width = stroke, pathEffect = HoldDash.pathEffect(this)),
                )
            }
        },
    )
}

/**
 * Strichmuster des Haltens in kleinen Phasenbildern ([PhaseMark], Mini-Kurve der Vorlagen):
 * Strich `GapTiny`, Lücke halb so lang. Die große Atemkurve hat ihr eigenes, gröberes Muster
 * (`Sizes.CurveDash`).
 */
object HoldDash {
    /** Das Muster in Pixeln für [density] (in einem `DrawScope`: `HoldDash.pathEffect(this)`). */
    fun pathEffect(density: Density): PathEffect {
        val dash = with(density) { Dimens.GapTiny.toPx() }
        return PathEffect.dashPathEffect(floatArrayOf(dash, dash / 2))
    }
}
