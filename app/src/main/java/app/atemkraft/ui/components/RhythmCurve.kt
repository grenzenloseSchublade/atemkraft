package app.atemkraft.ui.components

import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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

/**
 * Rhythmuskurve: ein Atemzug einer Übung ([BreathRhythm], aus `Exercise.breathRhythm()`) als dünne
 * Linie über die volle Breite, darunter die Phasen als Zeile („Einatmen 4 s · Halten voll 7 s ·
 * Ausatmen 8 s“). x = Zeit, y = Lungenfüllung (leer unten, voll oben). Farbsprache der Atemkurve
 * des Muster-Editors (FARBE-04): Einatmen `secondary`, Ausatmen [BreathExhale], Halten gestrichelt
 * ([HoldDash]) in der Farbe seiner Höhe (ab halb voll `secondary`), alles aus [breathSideColor],
 * gezeichnet über [drawLevelSegments] wie die [MiniCurve]. Keine Füllung, keine Griffe, keine
 * Zeitleiste – die Werte trägt die Zeile.
 *
 * Ein kleiner Punkt ([Sizes.RhythmCurveDot]) läuft im echten Takt der Übung endlos über die Kurve,
 * ohne Ton (Eigentümer 2026-10-30, Ausnahme in MOTION-02): Er zeigt den Takt, den man mitatmen
 * kann. Er ist immer `primary` (Pink, Eigentümer 2026-10-31: eine App-Farbe, kein Farbwechsel);
 * Koralle schied aus, weil sie die Linie des Ausatmens ist. Kein
 * Pulsieren, keine Fläche. 8 dp bleiben: Zum 2-dp-Strich steht er 4 : 1, deutlicher als der
 * Vorschaupunkt des Editors ([Sizes.CurveDot] 14 zu 4); als Perle auf der Linie liest er
 * sich ohne Weiß. Ein neuer Rhythmus (Phasen angepasst) startet ihn neu. Der Takt kommt aus den
 * Daten, nicht aus einer Animationsdauer (MOTION-01); bei abgeschalteten System-Animationen
 * ([rememberAnimationsEnabled]) fehlt der Punkt ganz. Ohne [running] (die Seite liegt unter einer
 * Session oder Meditation im Vollbild) fehlt er ebenfalls und seine Bildschleife steht – ein
 * verdeckter Punkt hielte sonst während der ganzen Meditation die volle Bildrate; danach beginnt
 * er neu. In Tests mit Endlos-Animationen-Sperre steht er am Anfang.
 *
 * [line] ersetzt die Zeile (Muster des Tages: dieselbe Musterzeile wie auf seiner Karte); ohne
 * sie entsteht sie aus den Phasen, gleiche Phasen hintereinander zusammengefasst („3 × Einatmen
 * 2 s“), bei [BreathRhythm.firstPartOnly] mit „Zu Beginn jeder Runde:“. [lineGap]: Abstand Kurve →
 * Zeile – auf der Detailseite `GapTiny`, in den Karteikarten von „Eigene“ `SectionGap` (Grafik und Text
 * sind dort zwei Blöcke einer Karte, §4.1). [lineReserve]: so viele Zeilen hält die Zeile mindestens
 * frei (Karteikarten 2), damit Kurve und Dauer in jeder Karte an derselben Stelle stehen.
 *
 * TalkBack: Die Kurve ist ein Knoten mit den Phasen in Worten („Atemkurve: Einatmen 4 Sekunden,
 * …“), ohne Live-Region; die Zeile darunter ist für TalkBack unsichtbar (sonst doppelt), bleibt
 * aber für die Umbruch-Prüfung sichtbar. Die Bewegung ist dekorativ.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun RhythmCurve(
    rhythm: BreathRhythm,
    modifier: Modifier = Modifier,
    line: String? = null,
    running: Boolean = true,
    lineGap: Dp = Dimens.GapTiny,
    lineReserve: Int = 1,
) {
    val colors = MaterialTheme.colorScheme
    val inhale = colors.secondary
    val dotColor = colors.primary
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

    val animate = rememberAnimationsEnabled() && running
    var time by remember(rhythm, animate) { mutableLongStateOf(0L) }
    if (animate) {
        LaunchedEffect(rhythm) {
            // Endlos: über die Endlos-Variante, damit Tests mit Sperre nicht ewig auf Leerlauf warten.
            val start = withInfiniteAnimationFrameNanos { it }
            while (true) {
                withInfiniteAnimationFrameNanos { now -> time = (now - start) / NANOS_PER_MILLI }
            }
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
            val area = Rect(inset, inset, size.width - inset, size.height - inset)
            drawLevelSegments(rhythm, inhale, stroke, area)
            if (animate && rhythm.steps.isNotEmpty()) {
                val cycle = rhythm.cycleMillis.coerceAtLeast(1L)
                val now = time.mod(cycle)
                // Dieselbe Abbildung wie drawLevelSegments: x = Zeit, y = Füllstand.
                val center = Offset(area.left + area.width * now / cycle, area.bottom - area.height * rhythm.levelAt(now))
                drawCircle(dotColor, radius = Sizes.RhythmCurveDot.toPx() / 2f, center = center)
            }
        }
        Spacer(Modifier.height(lineGap))
        // Für TalkBack unsichtbar: Die Kurve sagt dasselbe in Worten („4 Sekunden“ statt „4 s“).
        // Mit lineReserve > 1 hält die Zeile mindestens so viele Zeilen Platz (Karteikarten: Kurve und Dauer stehen in jeder Karte gleich).
        val reserve = rememberFirstLineHeight(MaterialTheme.typography.bodySmall) * lineReserve
        HintText(shownLine, modifier = Modifier.heightIn(min = reserve).semantics { invisibleToUser() })
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
