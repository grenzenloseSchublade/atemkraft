package app.atemkraft.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.domain.BreathLevel
import app.atemkraft.domain.BreathRhythm
import app.atemkraft.domain.BreathStep
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.formatDecimal
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.BreathExhale
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.rememberAnimationsEnabled

private const val NANOS_PER_MILLI = 1_000_000L
private const val MILLIS_PER_SECOND = 1000.0
private const val NBSP = '\u00A0'

/** Atemzüge, die der Punkt läuft, bevor er verschwindet – begrenzt wie die Durchläufe der Laufschrift. */
private const val DOT_BREATHS = 3

/**
 * Rhythmuskurve: ein Atemzug einer Übung ([BreathRhythm], aus `Exercise.breathRhythm()`) als dünne
 * Linie über die volle Breite, darunter die Phasen als Zeile („Einatmen 4 s · Halten voll 7 s ·
 * Ausatmen 8 s“). x = Zeit, y = Lungenfüllung (leer unten, voll oben). Farbsprache der Atemkurve
 * des Muster-Editors (FARBE-04): Einatmen `secondary`, Ausatmen [BreathExhale], Halten gestrichelt
 * ([HoldDash]) in der Farbe seiner Höhe (ab halb voll `secondary`). Keine Füllung, keine Griffe,
 * keine Zeitleiste – die Werte trägt die Zeile.
 *
 * Ein kleiner Punkt ([Sizes.RhythmCurveDot], `onSurface`) läuft im echten Takt der Übung
 * [DOT_BREATHS] Atemzüge lang über die Kurve, ohne Ton, dann verschwindet er und die Kurve steht:
 * Neben dem Lesetext läuft nichts endlos (MOTION-02, WCAG 2.2.2), wie die Laufschrift
 * ([MarqueeText]) nur dreimal läuft. Ein neuer Rhythmus (Phasen angepasst) startet ihn neu. Der
 * Takt kommt aus den Daten, nicht aus einer Animationsdauer (MOTION-01); bei abgeschalteten
 * System-Animationen ([rememberAnimationsEnabled]) fehlt der Punkt ganz. In Tests mit
 * Endlos-Animationen-Sperre steht er am Anfang.
 *
 * [line] ersetzt die Zeile (Muster des Tages: dieselbe Musterzeile wie auf seiner Karte); ohne
 * sie entsteht sie aus den Phasen, gleiche Phasen hintereinander zusammengefasst („3 × Einatmen
 * 2 s“), bei [BreathRhythm.firstPartOnly] mit „Zu Beginn jeder Runde:“.
 *
 * TalkBack: Die Kurve ist ein Knoten mit den Phasen in Worten („Atemkurve: Einatmen 4 Sekunden,
 * …“), ohne Live-Region; die Zeile darunter ist für TalkBack unsichtbar (sonst doppelt), bleibt
 * aber für die Umbruch-Prüfung sichtbar. Die Bewegung ist dekorativ.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RhythmCurve(rhythm: BreathRhythm, modifier: Modifier = Modifier, line: String? = null) {
    val colors = MaterialTheme.colorScheme
    val inhale = colors.secondary
    val dotColor = colors.onSurface
    val groups = remember(rhythm) { rhythm.groups() }
    val parts = groups.map { (step, count) ->
        // Name samt Zahl als ein Stück: Die Zeile bricht nur am Trennpunkt um („Halten voll 7 s“ bleibt zusammen).
        val part = stringResource(R.string.rhythm_phase_seconds, stepName(step).replace(' ', NBSP), seconds(step))
        if (count > 1) stringResource(R.string.rhythm_repeat, count, part) else part
    }
    val spokenParts = groups.map { (step, count) ->
        val part = pluralStringResource(R.plurals.cd_rhythm_phase_seconds, if (step.millis == MILLIS_PER_SECOND.toLong()) 1 else 2, stepName(step), seconds(step))
        if (count > 1) stringResource(R.string.cd_rhythm_repeat, count, part) else part
    }
    val phasesLine = parts.joinToString(DOT_SEPARATOR)
    val shownLine = line ?: if (rhythm.firstPartOnly) stringResource(R.string.rhythm_first_part, phasesLine) else phasesLine
    val description = spokenParts.joinToString(", ").let { spoken ->
        if (rhythm.firstPartOnly) stringResource(R.string.cd_rhythm_first_part, spoken) else stringResource(R.string.cd_breath_curve, spoken)
    }

    val animate = rememberAnimationsEnabled()
    var time by remember(rhythm) { mutableLongStateOf(0L) }
    var running by remember(rhythm, animate) { mutableStateOf(animate) }
    if (running) {
        LaunchedEffect(rhythm) {
            // Lang laufend, aber begrenzt: über die Endlos-Variante, damit Tests nicht alle Atemzüge abwarten.
            val end = rhythm.cycleMillis * DOT_BREATHS
            val start = withInfiniteAnimationFrameNanos { it }
            while (time < end) {
                withInfiniteAnimationFrameNanos { now -> time = (now - start) / NANOS_PER_MILLI }
            }
            running = false
        }
    }

    Column(modifier = modifier) {
        // dekorativ: die Werte stehen in der Zeile darunter und im Namen des Knotens (FARBE-03).
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(Sizes.RhythmCurveHeight)
                .semantics { contentDescription = description },
        ) {
            val stroke = Sizes.RhythmCurveStroke.toPx()
            val inset = maxOf(Sizes.RhythmCurveDot.toPx(), stroke) / 2f
            val left = inset
            val width = size.width - 2 * inset
            val top = inset
            val bottom = size.height - inset
            val cycle = rhythm.cycleMillis.coerceAtLeast(1L).toFloat()
            fun x(millis: Long) = left + width * millis / cycle
            fun y(level: Float) = bottom - (bottom - top) * level
            var t = 0L
            rhythm.steps.forEach { step ->
                val from = Offset(x(t), y(step.level.from))
                t += step.millis
                val to = Offset(x(t), y(step.level.to))
                drawLine(
                    color = if (step.upper) inhale else BreathExhale,
                    start = from,
                    end = to,
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                    pathEffect = if (step.hold) HoldDash.pathEffect(this) else null,
                )
            }
            if (running) {
                val now = time.mod(rhythm.cycleMillis.coerceAtLeast(1L))
                drawCircle(dotColor, radius = Sizes.RhythmCurveDot.toPx() / 2f, center = Offset(x(now), y(rhythm.levelAt(now))))
            }
        }
        Spacer(Modifier.height(Dimens.GapTiny))
        // Für TalkBack unsichtbar: Die Kurve sagt dasselbe in Worten („4 Sekunden“ statt „4 s“).
        HintText(shownLine, modifier = Modifier.semantics { invisibleToUser() })
    }
}

/** Name eines Abschnitts: eigenes Label (Viloma: „Halten“), sonst nach Typ wie in der Session. */
@Composable
private fun stepName(step: BreathStep): String = step.label ?: phaseName(step.type)

/** Sekunden mit Komma („4“, „1,5“). */
private fun seconds(step: BreathStep): String = (step.millis / MILLIS_PER_SECOND).formatDecimal()

@Preview
@Composable
private fun RhythmCurvePreview() {
    AtemkraftTheme {
        RhythmCurve(
            BreathRhythm(
                listOf(
                    BreathStep(PhaseType.INHALE, null, 4000L, BreathLevel(0f, 1f)),
                    BreathStep(PhaseType.HOLD_FULL, null, 7000L, BreathLevel.at(1f)),
                    BreathStep(PhaseType.EXHALE, null, 8000L, BreathLevel(1f, 0f)),
                ),
            ),
        )
    }
}
