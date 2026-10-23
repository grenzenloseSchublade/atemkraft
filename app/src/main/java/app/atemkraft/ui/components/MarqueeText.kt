package app.atemkraft.ui.components

import android.animation.ValueAnimator
import androidx.compose.foundation.MarqueeDefaults
import androidx.compose.foundation.basicMarquee
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import kotlinx.coroutines.delay

/** Kennzeichnet den Text, damit der ScreenshotTest die erlaubte Ellipse nach dem Lauf nicht meldet. */
const val MARQUEE_TEXT_TAG = "marquee-text"

/**
 * Einzeiliger Text, der als Laufschrift läuft, wenn er nicht in die Breite passt – statt umzubrechen
 * (Nutzer 2026-10-23: Titel bleiben eine Zeile, die Kopfzeile behält überall ihre Höhe). Er läuft
 * dreimal, ruhig wie die App; danach steht er am Anfang und endet mit „…“, statt mitten im Wort
 * abgeschnitten zu stehen (Ausnahme in LAYOUT-03). Ein neuer Text startet von vorn. Ohne
 * System-Animationen (Animator-Skala 0) gibt es keinen Lauf, sondern gleich „…“ (MOTION-02).
 * TalkBack liest immer den ganzen Text.
 *
 * Wann der Lauf vorbei ist, meldet `basicMarquee` nicht; die Dauer folgt aus seinen Vorgaben
 * ([MarqueeDefaults]: je Durchlauf Pause plus Weg aus Textbreite und Abstand bei fester
 * Geschwindigkeit). Erst danach wechselt die Darstellung – in diesem Moment steht der Text ohnehin
 * am Anfang, die Ellipse ersetzt nur das abgeschnittene Ende.
 */
@Composable
fun MarqueeText(text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val animate = remember { ValueAnimator.areAnimatorsEnabled() }
    var finished by remember(text) { mutableStateOf(!animate) }
    var containerPx by remember { mutableIntStateOf(0) }
    var textPx by remember(text) { mutableIntStateOf(0) }
    val density = LocalDensity.current
    if (!finished) {
        LaunchedEffect(text, containerPx, textPx) {
            if (containerPx == 0 || textPx == 0) return@LaunchedEffect
            // Passt der Text, läuft nichts: gleich die ruhige Darstellung.
            if (textPx > containerPx) delay(marqueeMillis(density, textPx, containerPx))
            finished = true
        }
    }
    Text(
        text = text,
        style = style,
        color = color,
        maxLines = 1,
        overflow = if (finished) TextOverflow.Ellipsis else TextOverflow.Clip,
        // Unter basicMarquee misst der Text ohne Breitengrenze: Das ist seine volle Breite.
        onTextLayout = { if (!finished) textPx = it.size.width },
        modifier = modifier
            .testTag(MARQUEE_TEXT_TAG)
            .onSizeChanged { containerPx = it.width }
            .then(if (finished) Modifier else Modifier.basicMarquee(iterations = MarqueeDefaults.Iterations)),
    )
}

/** Dauer aller Durchläufe von `basicMarquee` mit seinen Vorgaben, plus etwas Reserve. */
private fun marqueeMillis(density: Density, textPx: Int, containerPx: Int): Long {
    val spacingPx = with(MarqueeDefaults.Spacing) { density.calculateSpacing(textPx, containerPx) }
    val pxPerSecond = with(density) { MarqueeDefaults.Velocity.toPx() }
    val lapMillis = MarqueeDefaults.RepeatDelayMillis + (textPx + spacingPx) / pxPerSecond * MILLIS_PER_SECOND
    return (MarqueeDefaults.Iterations * lapMillis).toLong() + END_MARGIN_MS
}

private const val MILLIS_PER_SECOND = 1000f

/** Reserve, damit die Ellipse nie mitten in den letzten Durchlauf springt. */
private const val END_MARGIN_MS = 300L
