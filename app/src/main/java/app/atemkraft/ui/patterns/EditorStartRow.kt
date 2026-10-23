package app.atemkraft.ui.patterns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Constraints
import app.atemkraft.R
import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.ui.components.CompactStepper
import app.atemkraft.ui.components.InlineOrBelow
import app.atemkraft.ui.components.SlidingReset
import app.atemkraft.ui.components.StartPlayButton
import app.atemkraft.ui.components.TonalPillButton
import app.atemkraft.ui.components.flushEnd
import app.atemkraft.ui.components.rememberWidestTextWidth
import app.atemkraft.ui.components.widestOf
import app.atemkraft.ui.patterns.curve.phaseColor
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Startzeile über der Kurve (Nutzer 2026-10-23, statt des Kopfs „Atemzug“): links der Startpunkt
 * als tonale Pille „▦ Box“ bzw. „▦ angepasst“ ([TonalPillButton]; [onPickStart] öffnet die
 * Vorlagen, [TemplateSheet]; gesprochen „Startpunkt wählen, aktuell: Box“), daneben der Status –
 * Länge eines Durchlaufs und Atemzüge pro Minute („10 s · 6/min“, TalkBack in Worten), während der
 * Vorschau die Phase [phase] in ihrer Farbe (ohne Live-Region, sie wechselt im Takt) –, rechts die
 * Knöpfe der Vorschau ([controls]). So liest sich die Seite in der Reihenfolge der Arbeit: Vorlage
 * wählen, Kurve formen, starten.
 *
 * Ohne Springen: Gemessen wird mit der breitesten Beschriftung der Pille und dem breitesten Status.
 * Passt die Pille mit Beschriftung nicht daneben, zeigt sie nur das Symbol (nie ein Wortbruch);
 * passt auch das nicht (große Schrift), steht der Status in einer zweiten Zeile.
 *
 * Fokus (A11Y-07): Nach der Wahl einer Vorlage bekommt die Pille den Fokus und sagt einmal an, was
 * jetzt gilt („Startpunkt wählen, aktuell: Viloma I“) – bis sich die Folge ändert ([StartFocus]).
 */
@Composable
internal fun StartRow(
    state: PatternEditorState,
    phase: Int?,
    focus: StartFocus,
    onPickStart: () -> Unit,
    controls: @Composable () -> Unit,
) {
    val pattern = state.pattern
    val total = pattern.totalSeconds
    val rate = pattern.breathsPerMinuteText()
    // Genau „1“ in der Einzahl: „1 Atemzug pro Minute“ (wie die Texte der Domain).
    val spokenRate = if (rate == "1") stringResource(R.string.cd_pattern_editor_rate_one) else stringResource(R.string.cd_pattern_editor_rate, rate)
    val running = phase?.takeIf { it in pattern.phases.indices }
    val resting = stringResource(R.string.pattern_editor_cycle, total, stringResource(R.string.pattern_editor_rate_short, rate))
    // Platz für alles, was der Status zeigen kann: den ruhenden Status, einen breiten zweistelligen
    // (gleich breite Ziffern) und jeden Phasennamen der Vorschau – so springt die Zeile weder bei
    // Play/Pause noch beim Phasenwechsel oder beim Bearbeiten der Sekunden.
    val widest = stringResource(R.string.pattern_editor_cycle, WIDEST_CYCLE_SECONDS, stringResource(R.string.pattern_editor_rate_short, WIDEST_RATE))
    val reserve = listOf(resting, widest) + FreePattern.PHASE_LABELS
    val status = running?.let { pattern.labelAt(it) } ?: resting
    val statusDescription = running?.let { pattern.labelAt(it) } ?: pluralStringResource(R.plurals.cd_pattern_editor_cycle, total, total, spokenRate)
    val statusColor = running?.let { phaseColor(pattern, it, inhale = MaterialTheme.colorScheme.secondary) }
        ?: MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY)
    val statusStyle = MaterialTheme.typography.bodyMedium

    val current = state.template?.label ?: stringResource(R.string.adjust_modified)
    val startDescription = stringResource(R.string.cd_pattern_editor_start, current)
    val announce = focus.announces(state.pattern)
    LaunchedEffect(state.pattern) { focus.forgetUnless(state.pattern) }
    val grid = painterResource(R.drawable.ic_grid_view)
    // Alles, was die Pille zeigen kann: jede Vorlage und „angepasst“.
    val labels = FreePatternTemplates.all.map { it.label } + stringResource(R.string.adjust_modified)

    val statusReserve = rememberWidestTextWidth(reserve, statusStyle)
    // Eng unter dem Kopf und ohne Abstand zur Kurve (die hat über „voll“ eigenen Rand): So passen
    // Kurve und Werkzeugleiste auch auf 360 × 640 bei Schrift 1,3 samt Lasche der Tab-Leiste.
    SubcomposeLayout(Modifier.fillMaxWidth().padding(top = Dimens.GapTiny)) { constraints ->
        val width = constraints.maxWidth
        val gap = Dimens.GapSmall.roundToPx()
        val loose = Constraints(maxWidth = width)
        // Ohne System-Animationen fehlen die Vorschau-Knöpfe ganz (MOTION-02).
        val end = subcompose(RowSlot.CONTROLS) { Box(Modifier.flushEnd()) { controls() } }.map { it.measure(loose) }.filter { it.width > 0 }
        val endWidth = end.maxOfOrNull { it.width }?.plus(gap) ?: 0
        val statusWidth = statusReserve.roundToPx()
        val withLabel = widestOf(RowSlot.PILL_LABEL_RESERVE) { Box { labels.forEach { TonalPillButton(onClick = {}, icon = grid, label = it, description = "") } } }
        val showLabel = withLabel + gap + statusWidth + endWidth <= width
        val pillWidth = if (showLabel) withLabel else widestOf(RowSlot.PILL_ICON_RESERVE) { TonalPillButton(onClick = {}, icon = grid, label = null, description = "") }
        val pill = subcompose(RowSlot.PILL) {
            TonalPillButton(
                onClick = onPickStart,
                icon = grid,
                label = current.takeIf { showLabel },
                description = startDescription,
                modifier = Modifier
                    .focusRequester(focus.start)
                    .semantics { if (announce) liveRegion = LiveRegionMode.Polite },
            )
        }.single().measure(loose)
        val oneLine = pillWidth + gap + statusWidth + endWidth <= width
        val text = subcompose(RowSlot.STATUS) {
            Text(
                text = status,
                style = statusStyle,
                color = statusColor,
                modifier = Modifier.semantics { contentDescription = statusDescription },
            )
        }.single().measure(Constraints(maxWidth = if (oneLine) width - pillWidth - gap - endWidth else width))
        val firstLine = maxOf(pill.height, end.maxOfOrNull { it.height } ?: 0, if (oneLine) text.height else 0)
        layout(width, if (oneLine) firstLine else firstLine + text.height) {
            pill.placeRelative(0, (firstLine - pill.height) / 2)
            end.forEach { it.placeRelative(width - it.width, (firstLine - it.height) / 2) }
            if (oneLine) text.placeRelative(pillWidth + gap, (firstLine - text.height) / 2) else text.placeRelative(0, firstLine)
        }
    }
}

/** Slots der [StartRow]. */
private enum class RowSlot { PILL, STATUS, CONTROLS, PILL_LABEL_RESERVE, PILL_ICON_RESERVE }

/**
 * Breitester ruhender Status „· 88 s · 88,8/min“ für den Platz am Kopf „Atemzug“: Die Ziffern der
 * Schrift sind gleich breit, also deckt er jede zweistellige Länge und jede Rate ab.
 */
private const val WIDEST_CYCLE_SECONDS = 88
private const val WIDEST_RATE = "88,8"

/**
 * Fokus und Ansage der Leiste (A11Y-04, A11Y-07): [start] (Startpunkt-Pille) und [play] sind
 * Fokusziele, [pending] fokussiert nach der nächsten Komposition. Nach der Wahl einer Vorlage
 * ([picked]) bekommt das Startpunkt-Pille den Fokus und sagt einmal an, was gilt – nur solange
 * die Folge so bleibt ([announces]).
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
 * Feste Leiste unten (Nachtrag 6 und 7; der Startpunkt steht seit 2026-10-23 oben in der
 * [StartRow]), alle Bedienelemente sichtbar gleich hoch ([Sizes.IconButtonSize], ein Größentoken je
 * Leiste, LAYOUT-04): links die Dauer „− 5 min +“ ([CompactStepper], 1–30 min, ändert nie die
 * Folge); rechts, nur wenn geändert, Zurücksetzen auf den Stand beim Öffnen (Kreispfeil, tonal) und
 * Play ([StartPlayButton], hervorgehoben nur durch die Füllung in `primary`): startet die Übung
 * ([onStart], gesprochen „Übung starten, ca. 5 Minuten“). Eine Zeile, solange beides nebeneinander
 * passt (gemessen mit „30 min“ und mit Zurücksetzen, damit nichts springt), sonst Play rechts
 * darunter ([InlineOrBelow]).
 *
 * Fokus (A11Y-07): Löst man Zurücksetzen per Tastatur aus, verschwindet der Knopf, der Fokus geht
 * auf Play; [StartFocus.pending] fokussiert auch die Startpunkt-Pille nach der Wahl einer Vorlage.
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
