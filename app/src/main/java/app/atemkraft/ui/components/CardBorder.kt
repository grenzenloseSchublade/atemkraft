package app.atemkraft.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import app.atemkraft.ui.theme.ALPHA_BORDER
import app.atemkraft.ui.theme.Dimens

/**
 * Rand navigierender Inhaltskarten (Übung, Muster des Tages, gespeichertes Muster): dünn, in der
 * Akzentfarbe [color] mit [ALPHA_BORDER] – an einer Stelle statt als Literal je Karte (KOMP-03).
 */
fun accentBorder(color: Color): BorderStroke = BorderStroke(Dimens.BorderThin, color.copy(alpha = ALPHA_BORDER))

/**
 * Gestrichelter Rand in der Form [shape] (Halten, Angebot zum Hinzufügen im Muster-Editor): Strich
 * [width], Striche und Lücken je [dash], nach innen versetzt, damit er so weit reicht wie ein
 * durchgezogener `border`. Gezeichnet hinter dem Inhalt, die Fläche bleibt frei.
 */
fun Modifier.dashedBorder(color: Color, shape: Shape, width: Dp = Dimens.BorderThin, dash: Dp = Dimens.GapTiny): Modifier = drawBehind {
    val stroke = width.toPx()
    val outline = shape.createOutline(Size(size.width - stroke, size.height - stroke), layoutDirection, this)
    val step = dash.toPx()
    translate(stroke / 2, stroke / 2) {
        drawOutline(outline, color, style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(step, step))))
    }
}
