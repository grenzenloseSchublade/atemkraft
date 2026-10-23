package app.atemkraft.ui.patterns

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.unit.Constraints
import app.atemkraft.R
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.ui.components.AdaptiveButtonRow
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppOutlinedButton
import app.atemkraft.ui.components.CompactStepper
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.flushEnd
import app.atemkraft.ui.components.inertWhen
import app.atemkraft.ui.theme.BreathExhale
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.TERTIARY
import app.atemkraft.ui.theme.WarnAmber

/**
 * Werkzeugleiste direkt unter der Kurve (Karte): oben die gewählte Phase (Nummer, Name in
 * Phasenfarbe, Stepper „− 4 s +“, Papierkorb), gelb gestrichelt getrennt darunter „Neu nach 2:“
 * mit + Ein / + Halten / + Aus. Es ist immer eine Phase gewählt; der Papierkorb ist aus, wenn die
 * Folge danach ungültig wäre, die Knöpfe bei 12 Phasen. So ist alles ohne Ziehen bedienbar
 * (TalkBack, Switch Access).
 *
 * Einfügen und Entfernen sagt die Zeile der gewählten Phase einmal an („Halten voll als Phase 3
 * eingefügt, 5 von 12 Phasen“, kippt die Intensität: „…, intensiv“), der Stepper der nun
 * gewählten Phase schweigt dabei. Wählt man danach eine andere Phase, gilt wieder „Gewählte
 * Phase: …“. Wird der fokussierte Knopf abgeschaltet, geht der Tastatur-Fokus auf den Gegenpart ([ToolbarFocus]).
 */
@Composable
internal fun Toolbar(state: PatternEditorState, focus: ToolbarFocus) {
    val pattern = state.pattern
    LaunchedEffect(pattern, state.selected) { focus.forgetUnless(pattern, state.selected) }
    LaunchedEffect(focus.pending) {
        focus.pending?.let {
            it.requestFocus()
            focus.pending = null
        }
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.GapSmall)
            .testTag(PATTERN_TOOLBAR_TAG),
    ) {
        Column(Modifier.padding(start = Dimens.ListGap, end = Dimens.ListGap, top = Dimens.GapTiny, bottom = Dimens.GapTiny)) {
            SelectedPhase(state, focus)
            AddRow(state, focus)
        }
    }
}

/** Zeile der gewählten Phase in der Werkzeugleiste: Nummer und Name links, rechts Sekunden und Papierkorb. */
@Composable
private fun SelectedPhase(state: PatternEditorState, focus: ToolbarFocus) {
    val pattern = state.pattern
    val index = state.selected
    val phase = pattern.phases[index]
    val colors = MaterialTheme.colorScheme
    val label = pattern.labelAt(index)
    val notice = focus.noticeFor(pattern, index)
    val spokenPhase = stringResource(R.string.pattern_editor_phase_spoken, index + 1, label)
    val selectedText = stringResource(R.string.cd_pattern_selected, spokenPhase)
    val removedText = stringResource(R.string.cd_pattern_phase_removed, index + 1, pattern.phases.size - 1, FreePattern.MAX_PHASES)
    val intense = stringResource(R.string.detail_intense)
    val intensity = intensityChange()
    var trashFocused by remember { mutableStateOf(false) }
    // Der Wert sagt sich nur nach einem Tipp auf den eigenen Stepper an (A11Y-04): Wählt man eine
    // andere Phase, eine Vorlage oder zieht man an der Kurve, wechselt er still – auch wenn man
    // danach zu dieser Phase zurückkehrt (ein Wechsel der Auswahl vergisst den Tipp).
    var tapped by remember(index) { mutableStateOf<FreePattern?>(null) }
    LeadingAndEnd(
        leading = {
            PhaseTitle(
                index = index,
                label = label,
                color = phaseColor(pattern, index, inhale = colors.secondary),
                modifier = Modifier
                    // Ein Knoten: „Gewählte Phase: 2. Halten voll“; nach Einfügen oder Entfernen
                    // sagt er das einmal an (A11Y-04), bis sich Folge oder Auswahl wieder ändern.
                    .semantics(mergeDescendants = true) {
                        contentDescription = notice ?: selectedText
                        if (notice != null) liveRegion = LiveRegionMode.Polite
                    },
            )
        },
        end = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompactStepper(
                    value = phase.seconds,
                    range = FreePattern.SECONDS_RANGE,
                    onChange = {
                        state.setSeconds(index, it)
                        tapped = state.pattern
                    },
                    valueText = stringResource(R.string.pattern_editor_curve_seconds, phase.seconds),
                    spokenLabel = spokenPhase,
                    valueUnit = R.plurals.cd_duration_seconds,
                    // Kippt das Muster ins Intensive, hört man es mit dem Wert (eine Ansage je Tipp).
                    spokenSuffix = intense.takeIf { pattern.isIntense },
                    // Nach Einfügen oder Entfernen sagt die Zeile der Phase an, nicht der Stepper.
                    announce = notice == null && tapped == pattern,
                )
                val removable = state.canRemove(index)
                // Glyph an der Innenkante der Karte (LAYOUT-07).
                Box(Modifier.flushEnd(inCard = true)) {
                    AppIconButton(
                        onClick = {
                            val before = state.pattern
                            state.remove(index)
                            focus.notice(state.pattern, state.selected, removedText + intensity(before, state.pattern))
                            // Ist der Papierkorb der nun gewählten Phase aus, geht ein Tastatur-
                            // Fokus auf „+ Ein“ (A11Y-07).
                            if (trashFocused && !state.canRemove(state.selected)) focus.pending = focus.addIn
                        },
                        modifier = Modifier
                            .focusRequester(focus.trash)
                            .onFocusChanged { trashFocused = it.hasFocus },
                        enabled = removable,
                    ) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = stringResource(R.string.cd_pattern_phase_remove, index + 1),
                            tint = if (removable) colors.error else colors.onSurface.copy(alpha = TERTIARY),
                        )
                    }
                }
            }
        },
        // Ein- oder zweizeilig nach dem längsten Namen mit zweistelliger Nummer und „30 s“: Die
        // Werkzeugleiste behält ihre Höhe, wenn man eine andere Phase wählt.
        leadingReserve = {
            FreePattern.PHASE_LABELS.forEach { PhaseTitle(index = FreePattern.MAX_PHASES - 1, label = it, color = colors.onSurface) }
        },
        endReserve = {
            val most = FreePattern.SECONDS_RANGE.last
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompactStepper(
                    value = most,
                    range = FreePattern.SECONDS_RANGE,
                    onChange = {},
                    valueText = stringResource(R.string.pattern_editor_curve_seconds, most),
                    spokenLabel = "",
                )
                Box(Modifier.flushEnd(inCard = true)) {
                    AppIconButton(onClick = {}) {
                        Icon(Icons.Filled.Delete, contentDescription = null) // dekorativ: Platzhalter, nur gemessen
                    }
                }
            }
        },
    )
}

/** Nummer und Name einer Phase in [color], mindestens 48 dp hoch (Zeile der gewählten Phase). */
@Composable
private fun PhaseTitle(index: Int, label: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.heightIn(min = Dimens.MinTouchTarget),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
    ) {
        PhaseNumber(index, background = MaterialTheme.colorScheme.surface)
        WholeWordText(text = label, style = MaterialTheme.typography.titleSmall, color = color)
    }
}

/**
 * Zusatz zur Ansage nach Einfügen oder Entfernen, wenn das Muster dadurch ins Intensive kippt
 * („…, intensiv“) oder es nicht mehr ist („…, nicht mehr intensiv“) – wie beim Stepper
 * (`spokenSuffix`): Chip und Hinweiskarte erscheinen still, die Ansage trägt den Wechsel.
 */
@Composable
private fun intensityChange(): (FreePattern, FreePattern) -> String {
    val intense = stringResource(R.string.detail_intense)
    val notIntense = stringResource(R.string.cd_pattern_not_intense)
    return { before, after ->
        when {
            before.isIntense == after.isIntense -> ""
            after.isIntense -> ", $intense"
            else -> ", $notIntense"
        }
    }
}

/**
 * „Neu nach 2:“ (`WarnAmber`, wie die gestrichelte Linie darüber) und + Ein (`secondary`),
 * + Halten (`onSurfaceVariant`, gestrichelt), + Aus (`BreathExhale`) als kompakte
 * `AppOutlinedButton` in einer `AdaptiveButtonRow`; gesprochen „Einatmen hinzufügen“ usw. Neue
 * Phasen (Ein/Aus 4 s, Halten 2 s) kommen hinter die gewählte und sind danach gewählt. Darüber die
 * gelb gestrichelte Linie; passen Beschriftung und Knöpfe nicht in eine Zeile (360 dp ab Schrift
 * 1,1), steht „Neu nach 2:“ links in dieser Linie statt in einer eigenen Zeile.
 */
@Composable
private fun AddRow(state: PatternEditorState, focus: ToolbarFocus) {
    val colors = MaterialTheme.colorScheme
    val selected = state.selected
    val where = stringResource(R.string.pattern_editor_add_after_short, selected + 1)
    val widest = stringResource(R.string.pattern_editor_add_after_short, FreePattern.MAX_PHASES)
    val spokenWhere = stringResource(R.string.cd_pattern_add_after, selected + 1, state.pattern.labelAt(selected))
    val enabled = state.canAdd
    val resources = LocalResources.current
    var buttonFocused by remember { mutableStateOf(false) }
    val intensity = intensityChange()
    // Einfügen: Die Zeile der gewählten Phase sagt an, was wohin kam. Schaltet die zwölfte Phase
    // die Knöpfe ab, geht ein Tastatur-Fokus auf den Papierkorb der neuen Phase (A11Y-07).
    val add = { type: FreePhaseType ->
        val before = state.pattern
        state.add(type)
        val at = state.selected
        val count = state.pattern.phases.size
        val added = resources.getString(R.string.cd_pattern_phase_added, state.pattern.labelAt(at), at + 1, count, FreePattern.MAX_PHASES)
        focus.notice(state.pattern, at, added + intensity(before, state.pattern))
        if (buttonFocused && !state.canAdd) focus.pending = focus.trash
    }
    val label: @Composable () -> Unit = {
        // Platz für „Neu nach 12:“: Mit zweistelliger Nummer rutschte die Beschriftung sonst
        // über die Knöpfe, und die Werkzeugleiste würde beim Wechsel der Auswahl höher.
        Box {
            WholeWordText(
                text = widest,
                modifier = Modifier.alpha(0f).inertWhen(true),
                style = MaterialTheme.typography.labelMedium,
            )
            WholeWordText(
                text = where,
                modifier = Modifier.semantics { contentDescription = spokenWhere },
                style = MaterialTheme.typography.labelMedium,
                color = WarnAmber,
            )
        }
    }
    val buttons: @Composable (Boolean) -> Unit = { live ->
        AddButton(R.string.pattern_editor_add_in, R.string.cd_pattern_add_in, colors.secondary, enabled, onClick = { add(FreePhaseType.IN) }, modifier = if (live) Modifier.focusRequester(focus.addIn) else Modifier)
        AddButton(R.string.pattern_editor_add_hold, R.string.cd_pattern_add_hold, colors.onSurfaceVariant, enabled, onClick = { add(FreePhaseType.HOLD) }, dashed = true)
        AddButton(R.string.pattern_editor_add_out, R.string.cd_pattern_add_out, BreathExhale, enabled, onClick = { add(FreePhaseType.OUT) })
    }
    val separator = Modifier.fillMaxWidth().padding(top = Dimens.GapTiny, bottom = Dimens.GapSmall)
    SubcomposeLayout(Modifier.fillMaxWidth()) { constraints ->
        val width = constraints.maxWidth
        val gap = Dimens.GapSmall.roundToPx()
        // Passt „Neu nach 12:“ mit den drei Knöpfen in ihrer natürlichen Breite in eine Zeile?
        // (Dieselbe Regel wie AdaptiveButtonRow; gemessen, nie gezeigt oder vorgelesen.)
        val probeLabel = subcompose(AddSlot.PROBE_LABEL) {
            Box(Modifier.inertWhen(true)) { WholeWordText(text = widest, style = MaterialTheme.typography.labelMedium) }
        }.single().maxIntrinsicWidth(Constraints.Infinity)
        val probeButtons = subcompose(AddSlot.PROBE_BUTTONS) { Box(Modifier.inertWhen(true)) { Row { buttons(false) } } }
            .single().maxIntrinsicWidth(Constraints.Infinity)
        // Abstand nach der Beschriftung und zwischen den Knöpfen: ADD_BUTTONS Lücken.
        val inline = probeLabel + probeButtons + gap * ADD_BUTTONS <= width
        val content = subcompose(if (inline) AddSlot.INLINE else AddSlot.WRAPPED) {
            Column(Modifier.fillMaxWidth()) {
                if (inline) {
                    DashedMark(separator)
                } else {
                    // Zu eng für eine Zeile: „Neu nach 2:“ steht in der gestrichelten Linie, statt
                    // eine eigene Zeile über den Knöpfen zu belegen.
                    Row(separator, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                        label()
                        DashedMark(Modifier.weight(1f))
                    }
                }
                AdaptiveButtonRow(
                    spacing = Dimens.GapSmall,
                    // Die drei Knöpfe sind gleichrangig: keiner steht allein in voller Breite über den anderen.
                    primaryFirst = false,
                    modifier = Modifier.fillMaxWidth().onFocusChanged { buttonFocused = it.hasFocus },
                    leading = if (inline) label else null,
                ) { buttons(true) }
            }
        }.map { it.measure(Constraints(maxWidth = width)) }
        layout(width, content.sumOf { it.height }) {
            var y = 0
            content.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
        }
    }
}

/** Teile von [AddRow]: Messproben und die beiden Anordnungen. */
private enum class AddSlot { PROBE_LABEL, PROBE_BUTTONS, INLINE, WRAPPED }

/** Knöpfe + Ein, + Halten, + Aus. */
private const val ADD_BUTTONS = 3

/** Ein Knopf „+ Ein“ usw.: sichtbar kurz, gesprochen „Einatmen hinzufügen“. */
@Composable
private fun AddButton(
    label: Int,
    spoken: Int,
    color: Color,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    dashed: Boolean = false,
) {
    val description = stringResource(spoken)
    AppOutlinedButton(
        onClick = onClick,
        modifier = modifier.semantics { contentDescription = description },
        enabled = enabled,
        color = color,
        borderColor = color,
        dashed = dashed,
        compact = true,
    ) {
        Text(stringResource(label), maxLines = 1, softWrap = false)
    }
}

/**
 * Fokus und Ansagen der Werkzeugleiste (A11Y-04, A11Y-07): [trash] und [addIn] sind die Ziele,
 * wenn der fokussierte Knopf abgeschaltet wird (Papierkorb nach dem Entfernen → „+ Ein“; die
 * zwölfte Phase schaltet die Hinzufügen-Knöpfe ab → Papierkorb); [pending] fokussiert nach der
 * nächsten Komposition. [notice] merkt sich die Ansage zu Einfügen bzw. Entfernen samt der Folge
 * und der gewählten Phase danach; sie gilt nur, solange beide so bleiben ([noticeFor]).
 */
@Stable
internal class ToolbarFocus {
    val trash = FocusRequester()
    val addIn = FocusRequester()
    var pending by mutableStateOf<FocusRequester?>(null)
    private var lastNotice by mutableStateOf<Notice?>(null)

    fun notice(after: FreePattern, selected: Int, text: String) {
        lastNotice = Notice(after, selected, text)
    }

    fun noticeFor(pattern: FreePattern, selected: Int): String? = lastNotice?.takeIf { it.matches(pattern, selected) }?.text

    /**
     * Weicht die Folge oder die Auswahl ab, ist die Ansage verbraucht – auch wenn man später zu
     * beiden zurückkehrt.
     */
    fun forgetUnless(pattern: FreePattern, selected: Int) {
        if (lastNotice?.matches(pattern, selected) == false) lastNotice = null
    }

    private data class Notice(val after: FreePattern, val selected: Int, val text: String) {
        fun matches(pattern: FreePattern, selected: Int) = after == pattern && this.selected == selected
    }
}
