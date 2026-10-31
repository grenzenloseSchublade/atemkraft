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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import app.atemkraft.R
import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.DOT_SEPARATOR
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.ui.components.AppBottomSheet
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.CharacterChip
import app.atemkraft.ui.components.SessionAdjustControls
import app.atemkraft.ui.components.StartBar
import app.atemkraft.ui.components.flushEnd
import app.atemkraft.ui.components.flushStart
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
    val style = infoLineStyle()
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
        variants = listOf(
            line(label to phaseSpan, stringResource(R.string.pattern_editor_info_remaining, remaining) to quiet),
            line(label to phaseSpan),
        )
        reserves = previewReserveTexts().map { rememberWidestAnnotatedWidth(it, style) }
        spoken = label
    } else {
        val rate = stringResource(R.string.pattern_editor_rate_short, rateText)
        val seconds = stringResource(R.string.pattern_editor_info_seconds, total)
        variants = listOf(
            line(current to strong, stringResource(R.string.pattern_editor_info_breath, total) to quiet, rate to quiet),
            line(current to strong, seconds to quiet, rate to quiet),
            line(seconds to quiet, rate to quiet),
        )
        reserves = idleReserveTexts(total).map { rememberWidestAnnotatedWidth(it, style) }
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

/** Schriftrolle der [InfoLine]; auch [PatternEditorPreload] misst ihre Reserven in dieser Rolle. */
@Composable
@ReadOnlyComposable
internal fun infoLineStyle(): TextStyle = MaterialTheme.typography.bodyMedium

/** Test-Tag des Texts der [InfoLine] – für die gezeigte Fassung im `DeviceFitTest`. */
internal const val PATTERN_INFO_TAG = "pattern_editor_info"

/** Slots der [InfoLine]. */
private enum class InfoSlot { TEXT, CHIP }

/**
 * Reserve-Texte der [InfoLine] ohne Vorschau, je Fassung eine Liste (die breiteste Fassung zuerst):
 * jede Vorlage und „angepasst“ mit zweistelligen Werten, also mindestens [WIDEST_SECONDS]. Ohne
 * Farben – die Breite hängt nur am Gewicht des Startpunkts –, damit [PatternEditorPreload] genau
 * dieselben Texte vorab messen kann (ein Eintrag je Text in `TextWidths`).
 */
@Composable
internal fun idleReserveTexts(total: Int): List<List<AnnotatedString>> {
    val weight = MaterialTheme.typography.titleSmall.fontWeight
    val modified = stringResource(R.string.adjust_modified)
    val wide = maxOf(total, WIDEST_SECONDS)
    val wideRate = stringResource(R.string.pattern_editor_rate_short, WIDEST_RATE)
    val widePerBreath = stringResource(R.string.pattern_editor_info_breath, wide)
    val wideSeconds = stringResource(R.string.pattern_editor_info_seconds, wide)
    return remember(weight, modified, wideRate, widePerBreath, wideSeconds) {
        val labels = FreePatternTemplates.all.map { it.label } + modified
        val strong = SpanStyle(fontWeight = weight)
        listOf(
            labels.map { reserveLine(it to strong, widePerBreath to null, wideRate to null) },
            labels.map { reserveLine(it to strong, wideSeconds to null, wideRate to null) },
            listOf(reserveLine(wideSeconds to null, wideRate to null)),
        )
    }
}

/** Reserve-Texte der [InfoLine] während der Vorschau: jeder Phasenname mit und ohne „noch 88 s“ (wie [idleReserveTexts]). */
@Composable
internal fun previewReserveTexts(): List<List<AnnotatedString>> {
    val weight = MaterialTheme.typography.titleSmall.fontWeight
    val wideRemaining = stringResource(R.string.pattern_editor_info_remaining, WIDEST_REMAINING)
    return remember(weight, wideRemaining) {
        val strong = SpanStyle(fontWeight = weight)
        listOf(
            FreePattern.PHASE_LABELS.map { reserveLine(it to strong, wideRemaining to null) },
            FreePattern.PHASE_LABELS.map { reserveLine(it to strong) },
        )
    }
}

/** Eine Fassung der [InfoLine] nur für die Breite: Teile mit „·“ getrennt, ausgezeichnet nur, was ein Gewicht trägt. */
private fun reserveLine(vararg parts: Pair<String, SpanStyle?>): AnnotatedString = buildAnnotatedString {
    parts.forEachIndexed { i, (text, span) ->
        if (i > 0) append(DOT_SEPARATOR)
        if (span != null) withStyle(span) { append(text) } else append(text)
    }
}

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
 * Raster ([AppIconButton], `ic_grid_view`, `onBackground` `SECONDARY` wie das leere Speicherzeichen
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
 * Vorschau am unteren Rand der Kurvenkarte (Nutzer 2026-10-24, vorher eine Zeile mit „Vorschau“
 * rechtsbündig unter der Karte): links Play/Pause ([CurvePreviewControls]), rechts der Lautsprecher-
 * Schalter ([PreviewSoundSwitch]), ohne Beschriftung – die Knöpfe sagen „Vorschau abspielen“ /
 * „Vorschau anhalten“ und „Ton der Vorschau“. Die Glyphen stehen bündig an den Innenkanten der
 * Kurve: links am Beginn der Zeitachse (`CardPadding`, wo die „0“ beginnt), rechts am Ende ihrer
 * letzten Zahl (`GapSmall` vor dem Kartenrand), die Tippflächen ragen per [flushStart]/[flushEnd]
 * über diese Kanten. Kein eigener Abstand zur Zeitachse: Unter ihr liegt schon `GapTiny`, dazu der
 * unsichtbare Rand der Tippflächen – zusammen sichtbar `GapSmall`. Hoch genau [PREVIEW_ROW_HEIGHT];
 * bei abgeschalteten System-Animationen fehlt die Zeile ganz (MOTION-02), die Karte endet dann mit
 * der Zeitachse. TalkBack: nach der Kurve erst Play, dann Ton.
 */
@Composable
internal fun PreviewRow(preview: CurvePreviewState, sound: PreviewSoundState) {
    if (!rememberAnimationsEnabled()) return
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Dimens.CardPadding, end = Dimens.GapSmall),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.flushStart()) { CurvePreviewControls(preview) }
        Box(Modifier.flushEnd()) { PreviewSoundSwitch(sound) }
    }
}

/**
 * Höhe der [PreviewRow], solange sie da ist (Tippfläche der Icon-Knöpfe, wächst nicht mit der
 * Schrift): `FirstScreen` zieht sie innerhalb der Kurvenkarte vom Platz der Kurve ab.
 */
internal val PREVIEW_ROW_HEIGHT = Dimens.MinTouchTarget

/**
 * Fokus und Ansage (A11Y-04, A11Y-07): [start] (Startpunkt im Kopf, [StartButton]) ist Fokusziel,
 * [pending] fokussiert nach der nächsten Komposition. Nach der Wahl einer Vorlage
 * ([picked]) bekommt der Startpunkt den Fokus und sagt einmal an, was gilt – nur solange die Folge
 * so bleibt ([announces]).
 */
@Stable
internal class StartFocus {
    val start = FocusRequester()
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
 * Feste Leiste unten: dieselbe Startleiste wie auf der Detailseite ([StartBar], Eigentümer
 * 2026-10-31 – vorher „− 5 min +“ und ein runder Play-Knopf, der wie ein FAB wirkte). Oben
 * „Dauer · 5 min ›“, öffnet das Blatt mit dem Stepper ([DurationSheet], 1–30 min, ändert nie die
 * Folge); darunter „Starten · ca. 5 min“ ([onStart]) und, nur wenn geändert, Zurücksetzen auf den
 * Stand beim Öffnen (der Split-Button gibt den Fokus dann an „Starten“, A11Y-07).
 *
 * Fokus (A11Y-07): [StartFocus.pending] fokussiert den Startpunkt im Kopf nach der Wahl einer Vorlage.
 */
@Composable
internal fun ActionBar(
    state: PatternEditorState,
    approxMinutes: Int,
    focus: StartFocus,
    onOpenDuration: () -> Unit,
    onStart: () -> Unit,
) {
    LaunchedEffect(focus.pending) {
        focus.pending?.let {
            it.requestFocus()
            focus.pending = null
        }
    }
    StartBar(
        adjustTitle = stringResource(R.string.pattern_editor_duration),
        adjustStatus = stringResource(R.string.adjust_status_minutes, state.minutes),
        adjustStatusDescription = pluralStringResource(R.plurals.cd_duration_minutes, state.minutes, state.minutes),
        onOpenAdjust = onOpenDuration,
        adjustOpenLabel = stringResource(R.string.cd_pattern_editor_duration_open),
        startLabel = stringResource(
            R.string.action_start_duration,
            stringResource(R.string.action_start),
            stringResource(R.string.duration_approx, approxMinutes),
        ),
        resetVisible = state.modified,
        onStart = onStart,
        onReset = state::reset,
    )
}

/**
 * Blatt „Dauer“ des Muster-Editors ([AppBottomSheet]): derselbe Dauer-Stepper wie im Blatt
 * „Anpassen“ der Detailseite ([SessionAdjustControls] ohne Phasen – die formt die Kurve), 1–30 min.
 */
@Composable
internal fun DurationSheet(state: PatternEditorState, onDismiss: () -> Unit) {
    val title = stringResource(R.string.pattern_editor_duration)
    AppBottomSheet(title = title, onDismiss = onDismiss) {
        SessionAdjustControls(
            durationLabel = title,
            duration = state.minutes,
            durationDefault = state.minutes,
            durationRange = AdjustLimits.MINUTES,
            onDuration = { state.minutes = it },
            inhale = null,
            hold = null,
            exhale = null,
            intervalsExpanded = false,
            onToggleIntervals = {},
            durationSpokenLabel = stringResource(R.string.cd_adjust_duration),
            durationUnit = R.plurals.cd_duration_minutes,
            durationValue = R.string.adjust_value_minutes,
        )
    }
}
