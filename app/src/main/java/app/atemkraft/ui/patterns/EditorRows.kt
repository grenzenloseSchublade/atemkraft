package app.atemkraft.ui.patterns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.invisibleToUser
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import app.atemkraft.R
import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.CharacterChip
import app.atemkraft.ui.components.CompactStepper
import app.atemkraft.ui.components.InlineOrBelow
import app.atemkraft.ui.components.SlidingReset
import app.atemkraft.ui.components.StartPlayButton
import app.atemkraft.ui.components.flushEnd
import app.atemkraft.ui.components.rememberWidestAnnotatedWidth
import app.atemkraft.ui.patterns.curve.CurvePreviewControls
import app.atemkraft.ui.patterns.curve.CurvePreviewState
import app.atemkraft.ui.patterns.curve.phaseColor
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.TERTIARY
import app.atemkraft.ui.theme.rememberAnimationsEnabled
import kotlin.math.ceil

/**
 * Werte über der Kurve (Nutzer 2026-10-23: „über dem Graphen die Werte, keine Knöpfe“): eine
 * Zeile ohne Tippfläche „Box · 16 s je Atemzug · 3,8/min“ – der Startpunkt (die Vorlage, der die
 * Folge entspricht, sonst „angepasst“) in `onSurface` mit dem Gewicht von `titleSmall`, Länge
 * eines Atemzugs und Atemzüge pro Minute in `SECONDARY`, die Trenner „·“ in `TERTIARY`; rechts bei
 * intensiven Mustern „intensiv“ ([CharacterChip], nur dann). Läuft die Vorschau ([preview]), steht
 * statt der Werte die Phase in ihrer Farbe mit „noch 3 s“; nach der Pause wieder die Werte.
 *
 * Immer einzeilig, nie umbrochen: Passt die Zeile nicht, fällt zuerst „je Atemzug“ weg („Box ·
 * 16 s · 3,8/min“), dann der Startpunkt; in der Vorschau fällt „noch 3 s“ weg. Entschieden wird
 * mit Reserven (jede Vorlage und „angepasst“, zweistellige Werte, jeder Phasenname), damit die
 * Zeile beim Bearbeiten nicht zwischen den Fassungen springt. Hoch ist sie genau eine Textzeile;
 * „intensiv“ steht mittig darauf und ragt in die Abstände – kippt die Intensität, springt nichts.
 * Abstände: oben `SectionGap` wie jede Unterseite, zur Kurve `GapSmall` (Platz für die Kurve auf
 * 360 × 640 bei Schrift 1,3, `DeviceFitTest`).
 *
 * TalkBack: ein Knoten in Worten („Box, 16 Sekunden je Atemzug, 3,8 Atemzüge pro Minute“, dazu
 * „intensiv“), während der Vorschau nur die Phase – ohne Live-Region und ohne Restsekunden, damit
 * nichts im Sekundentakt spricht (A11Y-04).
 */
@Composable
internal fun InfoLine(state: PatternEditorState, preview: CurvePreviewState) {
    val pattern = state.pattern
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.bodyMedium
    val weight = MaterialTheme.typography.titleSmall.fontWeight
    val strong = SpanStyle(color = colors.onSurface, fontWeight = weight)
    val quiet = SpanStyle(color = colors.onSurface.copy(alpha = SECONDARY))
    val dim = SpanStyle(color = colors.onSurface.copy(alpha = TERTIARY))
    fun line(vararg parts: Pair<String, SpanStyle>): AnnotatedString = buildAnnotatedString {
        parts.forEachIndexed { i, (text, span) ->
            if (i > 0) withStyle(dim) { append(DOT_SEPARATOR) }
            withStyle(span) { append(text) }
        }
    }

    // Nur Phase und volle Sekunden sind Zustand: Die Zeile setzt sich je Sekunde neu, nicht je Bild.
    val running by remember(pattern) {
        derivedStateOf { preview.phaseIndexIn(pattern).takeIf { preview.playing && it in pattern.phases.indices } }
    }
    val remaining by remember(pattern) {
        derivedStateOf { ceil(preview.remainingMillisIn(pattern) / MILLIS_PER_SECOND).toInt().coerceAtLeast(1) }
    }

    val total = pattern.totalSeconds
    val rateText = pattern.breathsPerMinuteText()
    val current = state.template?.label ?: stringResource(R.string.adjust_modified)

    val phase = running
    val variants: List<AnnotatedString>
    val reserves: List<Dp>
    val spoken: String
    if (phase != null) {
        val label = pattern.labelAt(phase)
        val phaseSpan = SpanStyle(color = phaseColor(pattern, phase, inhale = colors.secondary), fontWeight = weight)
        val wideRemaining = stringResource(R.string.pattern_editor_info_remaining, WIDEST_REMAINING)
        variants = listOf(
            line(label to phaseSpan, stringResource(R.string.pattern_editor_info_remaining, remaining) to quiet),
            line(label to phaseSpan),
        )
        reserves = listOf(
            rememberWidestAnnotatedWidth(FreePattern.PHASE_LABELS.map { line(it to phaseSpan, wideRemaining to quiet) }, style),
            rememberWidestAnnotatedWidth(FreePattern.PHASE_LABELS.map { line(it to phaseSpan) }, style),
        )
        spoken = label
    } else {
        val labels = FreePatternTemplates.all.map { it.label } + stringResource(R.string.adjust_modified)
        val wide = maxOf(total, WIDEST_SECONDS)
        val wideRate = stringResource(R.string.pattern_editor_rate_short, WIDEST_RATE)
        val widePerBreath = stringResource(R.string.pattern_editor_info_breath, wide)
        val wideSeconds = stringResource(R.string.pattern_editor_info_seconds, wide)
        val rate = stringResource(R.string.pattern_editor_rate_short, rateText)
        val seconds = stringResource(R.string.pattern_editor_info_seconds, total)
        variants = listOf(
            line(current to strong, stringResource(R.string.pattern_editor_info_breath, total) to quiet, rate to quiet),
            line(current to strong, seconds to quiet, rate to quiet),
            line(seconds to quiet, rate to quiet),
        )
        reserves = listOf(
            rememberWidestAnnotatedWidth(labels.map { line(it to strong, widePerBreath to quiet, wideRate to quiet) }, style),
            rememberWidestAnnotatedWidth(labels.map { line(it to strong, wideSeconds to quiet, wideRate to quiet) }, style),
            rememberWidestAnnotatedWidth(listOf(line(wideSeconds to quiet, wideRate to quiet)), style),
        )
        // Genau „1“ in der Einzahl: „1 Atemzug pro Minute“ (wie die Texte der Domain).
        val spokenRate = if (rateText == "1") stringResource(R.string.cd_pattern_editor_rate_one) else stringResource(R.string.cd_pattern_editor_rate, rateText)
        spoken = stringResource(R.string.cd_pattern_editor_info, current, pluralStringResource(R.plurals.cd_pattern_editor_cycle, total, total, spokenRate))
    }
    val intense = pattern.isIntense

    SubcomposeLayout(
        Modifier
            .fillMaxWidth()
            .padding(top = Dimens.SectionGap, bottom = Dimens.GapSmall)
            .semantics(mergeDescendants = true) {},
    ) { constraints ->
        val width = constraints.maxWidth
        val loose = Constraints(maxWidth = width)
        val gap = Dimens.GapSmall.roundToPx()
        val chip = if (intense) subcompose(InfoSlot.CHIP) { CharacterChip(intense = true, activating = false) }.map { it.measure(loose) } else emptyList()
        val available = (width - (chip.maxOfOrNull { it.width + gap } ?: 0)).coerceAtLeast(0)
        val choice = reserves.indexOfFirst { it.roundToPx() <= available }.takeIf { it >= 0 } ?: variants.lastIndex
        val text = subcompose(InfoSlot.TEXT) {
            Text(
                text = variants[choice],
                style = style,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .testTag(PATTERN_INFO_TAG)
                    .semantics { contentDescription = spoken },
            )
        }.single().measure(Constraints(maxWidth = available))
        // Eine Textzeile hoch, mit oder ohne „intensiv“ (es ragt mittig in die Abstände).
        val height = text.height
        layout(width, height) {
            text.placeRelative(0, 0)
            chip.forEach { it.placeRelative(width - it.width, (height - it.height) / 2) }
        }
    }
}

/** Test-Tag des Texts der [InfoLine] – für die gezeigte Fassung im `DeviceFitTest`. */
internal const val PATTERN_INFO_TAG = "pattern_editor_info"

/** Slots der [InfoLine]. */
private enum class InfoSlot { TEXT, CHIP }

/**
 * Reserven der [InfoLine]: Die Ziffern der Schrift sind gleich breit, „88 s“ und „88,8/min“
 * decken also jede zweistellige Länge und jede Rate ab, „noch 88 s“ jede Restdauer (höchstens 30 s).
 */
private const val WIDEST_SECONDS = 88
private const val WIDEST_RATE = "88,8"
private const val WIDEST_REMAINING = 88
private const val MILLIS_PER_SECOND = 1000.0

/**
 * Startpunkt im Kopf (Nutzer 2026-10-23, vorher eine Pille über der Kurve): Icon-Knopf mit dem
 * Raster ([AppIconButton], `ic_grid_view`, `onBackground` `SECONDARY` wie das leere Lesezeichen
 * daneben), gesprochen „Startpunkt wählen, derzeit Box“ bzw. „… angepasst“. [onClick] öffnet die
 * Vorlagen ([TemplateSheet]).
 *
 * Fokus (A11Y-07): Nach der Wahl einer Vorlage bekommt der Knopf den Fokus und sagt einmal an, was
 * jetzt gilt („Startpunkt wählen, derzeit Viloma I“) – bis sich die Folge ändert ([StartFocus]).
 */
@Composable
internal fun StartButton(state: PatternEditorState, focus: StartFocus, onClick: () -> Unit) {
    val current = state.template?.label ?: stringResource(R.string.adjust_modified)
    val announce = focus.announces(state.pattern)
    LaunchedEffect(state.pattern) { focus.forgetUnless(state.pattern) }
    AppIconButton(
        onClick = onClick,
        modifier = Modifier
            .focusRequester(focus.start)
            .semantics { if (announce) liveRegion = LiveRegionMode.Polite },
    ) {
        Icon(
            painterResource(R.drawable.ic_grid_view),
            contentDescription = stringResource(R.string.cd_pattern_editor_start, current),
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
        )
    }
}

/**
 * Vorschau unter der Kurve (Nutzer 2026-10-23): rechtsbündig Play/Pause und der Lautsprecher-
 * Schalter ([CurvePreviewControls] mit [PreviewSoundSwitch], Größen wie überall), links davon klein
 * „Vorschau“ (`labelMedium` `SECONDARY`; für TalkBack unsichtbar, die Knöpfe sagen „Vorschau
 * abspielen“ und „Ton der Vorschau“). Bei abgeschalteten System-Animationen fehlt die ganze Zeile
 * samt Beschriftung (MOTION-02).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun PreviewRow(preview: CurvePreviewState, sound: PreviewSoundState) {
    if (!rememberAnimationsEnabled()) return
    Row(
        // Kein eigener Abstand zur Kurve: Unter ihrer Zeitachse liegt schon GapTiny, dazu der
        // unsichtbare Rand der 48-dp-Tippflächen – zusammen sichtbar etwa GapSmall.
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.pattern_editor_preview),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
            maxLines = 1,
            // Nicht clearAndSetSemantics: Die Umbruch- und Zuschnittprüfung braucht das Text-Layout.
            modifier = Modifier.semantics { invisibleToUser() },
        )
        Box(Modifier.flushEnd()) { CurvePreviewControls(preview) { PreviewSoundSwitch(sound) } }
    }
}

/**
 * Fokus und Ansage (A11Y-04, A11Y-07): [start] (Startpunkt im Kopf, [StartButton]) und [play] sind
 * Fokusziele, [pending] fokussiert nach der nächsten Komposition. Nach der Wahl einer Vorlage
 * ([picked]) bekommt der Startpunkt den Fokus und sagt einmal an, was gilt – nur solange die Folge
 * so bleibt ([announces]).
 */
@Stable
internal class StartFocus {
    val start = FocusRequester()
    val play = FocusRequester()
    var pending by mutableStateOf<FocusRequester?>(null)
    private var pickedPattern by mutableStateOf<FreePattern?>(null)

    fun picked(pattern: FreePattern) {
        pickedPattern = pattern
        pending = start
    }

    fun announces(pattern: FreePattern): Boolean = pickedPattern != null && pickedPattern == pattern

    /** Weicht die Folge ab, ist die Ansage verbraucht – auch wenn man später zu ihr zurückkehrt. */
    fun forgetUnless(pattern: FreePattern) {
        if (pickedPattern != null && pickedPattern != pattern) pickedPattern = null
    }
}

/**
 * Feste Leiste unten (Nachtrag 6 und 7; der Startpunkt steht seit 2026-10-23 im Kopf,
 * [StartButton]), alle Bedienelemente sichtbar gleich hoch ([Sizes.IconButtonSize], ein Größentoken je
 * Leiste, LAYOUT-04): links die Dauer „− 5 min +“ ([CompactStepper], 1–30 min, ändert nie die
 * Folge); rechts, nur wenn geändert, Zurücksetzen auf den Stand beim Öffnen (Kreispfeil, tonal) und
 * Play ([StartPlayButton], hervorgehoben nur durch die Füllung in `primary`): startet die Übung
 * ([onStart], gesprochen „Übung starten, ca. 5 Minuten“). Eine Zeile, solange beides nebeneinander
 * passt (gemessen mit „30 min“ und mit Zurücksetzen, damit nichts springt), sonst Play rechts
 * darunter ([InlineOrBelow]).
 *
 * Fokus (A11Y-07): Löst man Zurücksetzen per Tastatur aus, verschwindet der Knopf, der Fokus geht
 * auf Play; [StartFocus.pending] fokussiert auch den Startpunkt im Kopf nach der Wahl einer Vorlage.
 */
@Composable
internal fun ActionBar(
    state: PatternEditorState,
    approxMinutes: Int,
    focus: StartFocus,
    onStart: () -> Unit,
) {
    LaunchedEffect(focus.pending) {
        focus.pending?.let {
            it.requestFocus()
            focus.pending = null
        }
    }
    val playDescription = stringResource(R.string.cd_pattern_editor_play, pluralStringResource(R.plurals.cd_duration_minutes, approxMinutes, approxMinutes))
    InlineOrBelow(
        leading = { DurationStepper(state.minutes) { state.minutes = it } },
        end = {
            Row(Modifier.flushEnd(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                SlidingReset(
                    visible = state.modified,
                    onReset = state::reset,
                    onFocusHandoff = { focus.pending = focus.play },
                    slideFrom = Alignment.End,
                    compact = true,
                )
                StartPlayButton(onClick = onStart, description = playDescription, modifier = Modifier.focusRequester(focus.play))
            }
        },
        leadingReserve = { DurationStepper(AdjustLimits.MINUTES.last) {} },
        endReserve = {
            Row(Modifier.flushEnd(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                SlidingReset(visible = true, onReset = {}, onFocusHandoff = {}, compact = true)
                StartPlayButton(onClick = {}, description = "")
            }
        },
    )
}

/** Dauer „− 5 min +“ der Leiste (1–30 min, gesprochen „Dauer, 5 Minuten“). */
@Composable
private fun DurationStepper(minutes: Int, onChange: (Int) -> Unit) {
    CompactStepper(
        value = minutes,
        range = AdjustLimits.MINUTES,
        onChange = onChange,
        valueText = stringResource(R.string.pattern_editor_minutes, minutes),
        spokenLabel = stringResource(R.string.cd_adjust_duration),
        valueUnit = R.plurals.cd_duration_minutes,
    )
}
