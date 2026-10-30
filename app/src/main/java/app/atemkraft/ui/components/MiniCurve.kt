package app.atemkraft.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.domain.BreathRhythm
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.breathRhythm
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.BreathExhale
import app.atemkraft.ui.theme.Sizes

/**
 * Die eine Farbregel der Atemkurven (FARBE-04): obere Seite ([upper]: Einatmen, Nachatmen, Halten
 * ab halb voll) [inhale] (`secondary`), sonst [BreathExhale]. Kurve und Griffe des Editors
 * (`phaseColor`), Rhythmuskurve samt Punkt ([RhythmCurve]) und Mini-Kurven ([MiniCurve]) lesen sie
 * hier, damit die Seiten überall gleich gefärbt sind.
 */
internal fun breathSideColor(upper: Boolean, inhale: Color): Color = if (upper) inhale else BreathExhale

/**
 * Zeichnet einen Atemzug ([rhythm]) als dünne Linie in [area]: x = Zeit über die ganze Breite,
 * y = Füllstand (leer unten, voll oben); jeder Abschnitt in seiner Farbe ([breathSideColor]),
 * Halten gestrichelt ([HoldDash]), runde Enden, ohne Füllung. Gemeinsam für [RhythmCurve] und
 * [MiniCurve]; die große Kurve des Editors zeichnet selbst (Griffe, gewählte Phase dicker,
 * Zeitleiste), nimmt aber dieselbe Farbregel.
 */
internal fun DrawScope.drawLevelSegments(rhythm: BreathRhythm, inhale: Color, stroke: Float, area: Rect) {
    val cycle = rhythm.cycleMillis.coerceAtLeast(1L).toFloat()
    fun x(millis: Long) = area.left + area.width * millis / cycle
    fun y(level: Float) = area.bottom - area.height * level
    var t = 0L
    rhythm.steps.forEach { step ->
        val from = Offset(x(t), y(step.level.from))
        t += step.millis
        val to = Offset(x(t), y(step.level.to))
        drawLine(
            color = breathSideColor(step.upper, inhale),
            start = from,
            end = to,
            strokeWidth = stroke,
            cap = StrokeCap.Round,
            pathEffect = if (step.hold) HoldDash.pathEffect(this) else null,
        )
    }
}

/**
 * Kleine Atemkurve ([Sizes.MiniCurveWidth] × [Sizes.MiniCurveHeight], Strich
 * [Sizes.RhythmCurveStroke]): ein Atemzug ([rhythm]) in der Farbsprache der großen Kurve, statisch,
 * ohne Zahlen, ohne Punkt. Steht je Vorlage im Startpunkt-Sheet des Editors und vor „Muster des
 * Tages ›“ im Atmen-Tab (die Kurve des heutigen Musters: Man sieht, dass es sich täglich ändert).
 *
 * Dekorativ, ohne Semantik: Daneben stehen Name bzw. Teaser. Keine Bewegung (MOTION-02).
 */
@Composable
internal fun MiniCurve(rhythm: BreathRhythm, modifier: Modifier = Modifier) {
    val inhale = MaterialTheme.colorScheme.secondary
    Canvas(modifier.size(width = Sizes.MiniCurveWidth, height = Sizes.MiniCurveHeight)) {
        val stroke = Sizes.RhythmCurveStroke.toPx()
        // Oben und unten ein Strich Luft, damit „voll“ und „leer“ nicht an der Kante kleben.
        drawLevelSegments(rhythm, inhale, stroke, Rect(0f, stroke, size.width, size.height - stroke))
    }
}

@Preview
@Composable
private fun MiniCurvePreview() {
    AtemkraftTheme {
        MiniCurve(FreePatternTemplates.all.first().pattern.breathRhythm())
    }
}
