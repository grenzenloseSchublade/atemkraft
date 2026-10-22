package app.atemkraft.ui.patterns

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import app.atemkraft.R
import app.atemkraft.cue.AppPreviewSoundOutput
import app.atemkraft.cue.PreviewSoundOutput
import app.atemkraft.data.Atemlehre
import app.atemkraft.data.CueSettings
import app.atemkraft.data.PatternBackup
import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.FreePattern
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.FreePhaseType
import app.atemkraft.domain.FreeTemplate
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.adjusted
import app.atemkraft.domain.estimatedTotalSeconds
import app.atemkraft.playback.PhaseAudio
import app.atemkraft.ui.components.AdaptiveButtonRow
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppIconToggle
import app.atemkraft.ui.components.AppOutlinedButton
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.BreathCurveEditor
import app.atemkraft.ui.components.CharacterChip
import app.atemkraft.ui.components.CollapsibleSection
import app.atemkraft.ui.components.CompactStepper
import app.atemkraft.ui.components.CurvePreviewControls
import app.atemkraft.ui.components.DisclosurePill
import app.atemkraft.ui.components.ExpanderSection
import app.atemkraft.ui.components.ForwardLinkText
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.NoPersonalizedLearning
import app.atemkraft.ui.components.PinnedActionBar
import app.atemkraft.ui.components.PushHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SegmentedChoiceRow
import app.atemkraft.ui.components.StartSplitButton
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.accentBorder
import app.atemkraft.ui.components.color
import app.atemkraft.ui.components.flushEnd
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.components.inertWhen
import app.atemkraft.ui.components.rememberArmedConfirm
import app.atemkraft.ui.components.rememberCurvePreviewState
import app.atemkraft.ui.detail.SafetyDialog
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.BreathExhale
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.SegmentSelected
import app.atemkraft.ui.theme.Sizes
import app.atemkraft.ui.theme.TERTIARY
import app.atemkraft.ui.theme.WarnAmber

/**
 * Muster-Editor „Eigenes Muster“ (MUSTER-10, Nachtrag 4 „Frei gestalten“, Nachtrag 5 „Kompakt“),
 * Push-Seite. Von oben: Kopf mit Lesezeichen · eine Zeile mit „Startpunkt · Box ▾“ (Vorlagen
 * klappen darunter auf) und rechts der Dauer „− 5 min +“ · „Atemzug“ mit kurzem Status (bzw. der
 * Phase der laufenden Vorschau), ggf. „intensiv“ und rechts Play/Pause und Ton-Schalter · die
 * Atemkurve (Höhe wie bisher, keine eigene Vorschau-Zeile) · direkt darunter die Werkzeugleiste
 * (gewählte Phase mit Sekunden-Stepper und Papierkorb, gelb gestrichelt getrennt „Neu nach 2:“ mit
 * + Ein / + Halten / + Aus). Ziel: Auf dem A54 stehen Kurve und Werkzeugleiste ohne Scrollen ganz
 * da. Darunter Hinweis bei intensiven Mustern, „Ablauf“ (eingeklappt, Überblick und TalkBack),
 * Name, „Aus der Yoga-Lehre“; unten fest „Starten · ca. N min“ mit Reset auf den Stand beim Öffnen.
 *
 * Die Seite liest und ändert nur [state] (ARC-7). Was gespeichert wird, entscheidet der Aufrufer:
 * [onSavedChange] (true = speichern bzw. denselben Eintrag aktualisieren, false = aus „Meine
 * Muster“ entfernen) schreibt ins Repository und meldet das Ergebnis mit
 * [PatternEditorState.markSaved] bzw. [PatternEditorState.markRemoved] zurück. Ein Muster, das es
 * schon vor dem Öffnen gab, entfernt das Lesezeichen nur zweistufig (MUSTER-05); ein in dieser
 * Sitzung angelegtes nimmt ein erneuter Tipp sofort wieder heraus (KOMP-02).
 *
 * [onStart] probiert den Entwurf ohne Speichern aus (Übung und Dauer als [SessionConfig]); die
 * Seite bleibt dabei offen. Ein intensives Muster startet erst nach dem Sicherheitsdialog, wenn
 * [requireSafetyConfirm] (wie auf der Detailseite: noch nie bestätigt oder Hinweis eingeschaltet);
 * die Bestätigung meldet [onConfirmedSafety]. [onOpenLore] öffnet eine Karte der Atemlehre.
 *
 * Ton der Vorschau ([rememberPreviewSound]): Klang-Einstellungen der App [cueSettings]; still,
 * solange eine Übung oder Meditation läuft ([otherAudioActive]). [soundOutput] tauschen nur Tests.
 */
@Composable
fun PatternEditorScreen(
    state: PatternEditorState,
    onSavedChange: (Boolean) -> Unit,
    onStart: (Exercise, SessionConfig) -> Unit,
    onOpenLore: (patternId: String) -> Unit,
    onBack: () -> Unit,
    cueSettings: CueSettings = CueSettings(),
    otherAudioActive: Boolean = false,
    requireSafetyConfirm: Boolean = true,
    onConfirmedSafety: () -> Unit = {},
    soundOutput: (Context) -> PreviewSoundOutput = ::AppPreviewSoundOutput,
) {
    val exercise = state.exercise()
    val config = state.config()
    val estimatedSeconds = remember(exercise, config) { exercise.adjusted(config).estimatedTotalSeconds() }
    val approx = stringResource(R.string.duration_approx, ((estimatedSeconds + HALF_MINUTE) / SECONDS_PER_MINUTE).coerceAtLeast(1))

    // Vorschau der Kurve und ihr Ton: hält an, wenn die Seite verdeckt wird oder die App in den
    // Hintergrund geht; der Ton folgt der Phase, in der der Punkt gerade steht.
    val preview = rememberCurvePreviewState()
    val sound = rememberPreviewSound(cueSettings, otherAudioActive, onHide = preview::pause, output = soundOutput)
    val pattern = state.pattern
    val phaseIndex by remember(pattern) { derivedStateOf { preview.phaseIndexIn(pattern) } }
    // Ändert sich die Folge, während die Vorschau läuft, folgt der Ton der Phase, die jetzt an
    // diesem Index steht (Art = Typ und Füllstand; eine andere Art gibt einen Wechselton).
    val phaseKind = exercise.segments.firstOrNull()?.phases?.getOrNull(phaseIndex)?.let { it.type to it.level }
    PreviewSoundEffect(sound, preview.playing, phaseIndex, phaseKind = phaseKind, revision = pattern) {
        phaseAudio(state.exercise(), phaseIndex, preview.remainingMillisIn(state.pattern))
    }

    // Offener Dialog überlebt Rotation und Prozess-Neustart (CODE-03).
    var showSafety by rememberSaveable { mutableStateOf(false) }
    val toolbarFocus = remember { ToolbarFocus() }
    val start = {
        preview.pause()
        onStart(exercise, config)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            PushHeader(
                onBack = onBack,
                title = stringResource(R.string.pattern_editor_title),
                action = { SaveToggle(state, onSavedChange) },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding),
            ) {
                StartAndDuration(state)
                Breath(state, phase = phaseIndex.takeIf { preview.playing }) {
                    CurvePreviewControls(preview) { PreviewSoundSwitch(sound) }
                }
                BreathCurveEditor(
                    pattern = pattern,
                    selected = state.selected,
                    onSelect = { it?.let(state::select) },
                    onChangeSeconds = state::setSeconds,
                    modifier = Modifier.fillMaxWidth(),
                    preview = preview,
                )
                Toolbar(state, toolbarFocus)
                pattern.intensityNote()?.let { IntenseNote(it) }
                Sequence(state)
                NameSection(state)
                Lore(exercise, onOpenLore)
                Spacer(Modifier.height(Dimens.ScreenBottom))
            }
            PinnedActionBar {
                StartSplitButton(
                    label = stringResource(R.string.action_start_duration, stringResource(R.string.action_start), approx),
                    resetVisible = state.modified,
                    onStart = { if (exercise.caution && requireSafetyConfirm) showSafety = true else start() },
                    onReset = state::reset,
                )
            }
        }
    }

    if (showSafety) {
        SafetyDialog(
            cautions = exercise.cautions,
            onConfirm = {
                showSafety = false
                onConfirmedSafety()
                start()
            },
            onDismiss = { showSafety = false },
        )
    }
}

/** Ton der Phase [index] von [exercise] (Typ und Füllstand wie in der Übung), Restdauer [remainingMs]. */
private fun phaseAudio(exercise: Exercise, index: Int, remainingMs: Long): PhaseAudio {
    val phase = exercise.segments.firstOrNull()?.phases?.getOrNull(index)
        ?: return PhaseAudio(PhaseType.INHALE, remainingMs, open = false)
    return PhaseAudio(phase.type, remainingMs, open = false, level = phase.level)
}

/**
 * Erste Zeile: links „Startpunkt · Box ▾“ ([DisclosurePill]; man braucht ihn nur zum Anfang),
 * rechts die Dauer „− 5 min +“ ([CompactStepper], 1–30 min, ändert nie die Folge). Passen beide
 * nicht nebeneinander (große Schrift), steht die Dauer rechtsbündig darunter ([LeadingAndEnd]) –
 * entschieden für die breiteste Pille („angepasst“ oder eine Vorlage), damit die Zeile nicht
 * umspringt, wenn man die Folge ändert und die Pille „angepasst“ zeigt. Für die Dauer gilt ihr
 * Wert: Mit „30 min“ reichte der Platz auf dem A54 bei Schrift 1,3 nicht mehr für eine Zeile
 * (MUSTER-10); sie ändert ihre Breite nur über den eigenen Stepper, beim Schritt 9 ↔ 10 min.
 * Aufgeklappt stehen die Vorlagen als Auswahlreihe im Raster darunter; die Pille nennt die
 * Vorlage, der die Folge gerade entspricht, sonst „angepasst“. Eine Vorlage setzt die Folge und
 * klappt wieder zu.
 */
@Composable
private fun StartAndDuration(state: PatternEditorState) {
    var open by rememberSaveable { mutableStateOf(false) }
    val template = state.template
    val modified = stringResource(R.string.adjust_modified)
    // Eine Wahl klappt zu und nimmt das gewählte Segment weg: Der Fokus geht auf die Pille
    // (Tastatur, D-Pad, A11Y-07), und sie sagt einmal an, was jetzt gilt („Startpunkt Viloma I,
    // eingeklappt“) – nur bis zur nächsten Änderung der Folge oder einem Tipp auf die Pille.
    val pillFocus = remember { FocusRequester() }
    var picked by remember { mutableStateOf<FreePattern?>(null) }
    var focusPill by remember { mutableStateOf(false) }
    val announce = picked != null && picked == state.pattern
    // Weicht die Folge ab, ist die Ansage verbraucht – auch wenn man später zu ihr zurückkehrt.
    LaunchedEffect(state.pattern) { if (picked != state.pattern) picked = null }
    LaunchedEffect(focusPill) {
        if (focusPill) {
            pillFocus.requestFocus()
            focusPill = false
        }
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.ScreenTopSub)
            .animateContentSize(),
        verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
    ) {
        LeadingAndEnd(
            leading = {
                DisclosurePill(
                    label = stringResource(R.string.pattern_editor_start),
                    value = template?.label ?: modified,
                    expanded = open,
                    onToggle = {
                        picked = null
                        open = !open
                    },
                    modifier = Modifier
                        .focusRequester(pillFocus)
                        .semantics { if (announce) liveRegion = LiveRegionMode.Polite },
                )
            },
            end = {
                CompactStepper(
                    value = state.minutes,
                    range = AdjustLimits.MINUTES,
                    onChange = { state.minutes = it },
                    valueText = stringResource(R.string.pattern_editor_minutes, state.minutes),
                    spokenLabel = stringResource(R.string.cd_adjust_duration),
                    valueUnit = R.plurals.cd_duration_minutes,
                    flushEnd = true,
                )
            },
            leadingReserve = {
                (FreePatternTemplates.all.map { it.label } + modified).forEach {
                    DisclosurePill(label = stringResource(R.string.pattern_editor_start), value = it, expanded = false, onToggle = {})
                }
            },
        )
        if (open) {
            SegmentedChoiceRow<FreeTemplate?>(
                items = FreePatternTemplates.all,
                selected = template,
                onSelect = { choice ->
                    choice?.let(state::applyTemplate)
                    picked = state.pattern
                    open = false
                    focusPill = true
                },
                // Ohne eigenen Gruppennamen: Die Pille „Startpunkt“ steht direkt darüber.
                columns = TEMPLATE_COLUMNS,
            ) { it?.label.orEmpty() }
        }
    }
}

/**
 * Zwei Teile einer Zeile: [leading] links, [end] rechtsbündig. Passen beide in ihrer natürlichen
 * Breite nebeneinander (mit `GapTiny`, sichtbar `GapSmall` bis zum Kreis des Steppers), stehen
 * sie in einer Zeile, mittig zueinander, und [leading] bekommt den Rest der Breite; sonst steht [end] rechtsbündig unter [leading] – nichts
 * wird gekürzt oder zusammengedrückt (LAYOUT-03).
 *
 * [leadingReserve] und [endReserve] zeigen, was dort sonst noch stehen kann (andere Namen, längere
 * Werte), jeweils als Geschwister: Sie werden nur gemessen, nie gezeigt oder vorgelesen. Die
 * Entscheidung ein- oder zweizeilig gilt für die breitesten davon, damit die Zeile nicht
 * umspringt, wenn sich der Inhalt ändert, und was darunter steht, nicht mitrückt.
 */
@Composable
private fun LeadingAndEnd(
    leading: @Composable () -> Unit,
    end: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leadingReserve: @Composable () -> Unit = {},
    endReserve: @Composable () -> Unit = {},
) {
    // Nie platziert; zusätzlich ohne Semantik und Fokus, falls doch jemand den Baum durchsucht.
    val unseen = Modifier.inertWhen(true)
    Layout(
        contents = listOf(leading, end, { Box(unseen) { leadingReserve() } }, { Box(unseen) { endReserve() } }),
        modifier = modifier.fillMaxWidth(),
    ) { (leadingMeasurables, endMeasurables, leadingReserves, endReserves), constraints ->
        val width = constraints.maxWidth
        // [end] beginnt mit einem Stepper-Knopf, dessen Tippfläche 4 dp über den sichtbaren Kreis
        // hinausreicht: GapTiny sieht so aus wie GapSmall.
        val gap = Dimens.GapTiny.roundToPx()
        val first = leadingMeasurables.single()
        val second = endMeasurables.single()
        fun widest(measurable: Measurable, reserves: List<Measurable>) = (reserves + measurable).maxOf { it.maxIntrinsicWidth(Constraints.Infinity) }
        val oneLine = widest(first, leadingReserves) + gap + widest(second, endReserves) <= width
        val endPlaceable = second.measure(Constraints(maxWidth = width))
        if (oneLine) {
            val leadingPlaceable = first.measure(Constraints(maxWidth = width - gap - endPlaceable.width))
            val height = maxOf(leadingPlaceable.height, endPlaceable.height)
            layout(width, height) {
                leadingPlaceable.placeRelative(0, (height - leadingPlaceable.height) / 2)
                endPlaceable.placeRelative(width - endPlaceable.width, (height - endPlaceable.height) / 2)
            }
        } else {
            val leadingPlaceable = first.measure(Constraints(maxWidth = width))
            layout(width, leadingPlaceable.height + endPlaceable.height) {
                leadingPlaceable.placeRelative(0, 0)
                endPlaceable.placeRelative(width - endPlaceable.width, leadingPlaceable.height)
            }
        }
    }
}

/**
 * „Atemzug“: Länge eines Durchlaufs und Atemzüge pro Minute als kurzer Status am Kopf
 * („· 10 s · 6/min“, TalkBack in Worten), bei einem intensiven Muster dazu „intensiv“
 * (`CharacterChip`). Läuft die Vorschau, steht statt des Status die Phase [phase] in ihrer Farbe
 * (ohne Live-Region, sie wechselt im Takt). Rechts die Knöpfe der Vorschau ([controls]), mittig
 * auf der Titelzeile wie der Chip. Der Kopf behält seine Höhe: Lage des Status und Platz des Chips
 * sind für alle Texte und beide Zustände reserviert.
 */
@Composable
private fun Breath(state: PatternEditorState, phase: Int?, controls: @Composable () -> Unit) {
    val pattern = state.pattern
    val total = pattern.totalSeconds
    val rate = pattern.breathsPerMinuteText()
    // Genau „1“ in der Einzahl: „1 Atemzug pro Minute“ (wie die Texte der Domain).
    val spokenRate = if (rate == "1") stringResource(R.string.cd_pattern_editor_rate_one) else stringResource(R.string.cd_pattern_editor_rate, rate)
    val running = phase?.takeIf { it in pattern.phases.indices }
    val resting = stringResource(R.string.pattern_editor_cycle, total, stringResource(R.string.pattern_editor_rate_short, rate))
    // Platz für alles, was der Status zeigen kann: den ruhenden Status, einen breiten zweistelligen
    // (gleich breite Ziffern) und jeden Phasennamen der Vorschau – so ändert der Kopf seine Höhe
    // weder bei Play/Pause noch beim Phasenwechsel oder beim Bearbeiten der Sekunden.
    val widest = stringResource(R.string.pattern_editor_cycle, WIDEST_CYCLE_SECONDS, stringResource(R.string.pattern_editor_rate_short, WIDEST_RATE))
    val reserve = listOf(resting, widest) + FreePattern.PHASE_LABELS.map { "· $it" }
    val status: String
    val statusDescription: String
    if (running != null) {
        statusDescription = pattern.labelAt(running)
        status = "· $statusDescription"
    } else {
        status = resting
        statusDescription = pluralStringResource(R.plurals.cd_pattern_editor_cycle, total, total, spokenRate)
    }
    SectionHeader(
        title = stringResource(R.string.pattern_editor_breath),
        color = headingColor(0),
        status = status,
        statusDescription = statusDescription,
        smallStatus = true,
        statusColor = running?.let { phaseColor(pattern, it, inhale = MaterialTheme.colorScheme.secondary) },
        // Der Chip steht neben der Titelzeile, mit Abstand zum Status (auch wenn der umbricht).
        // Sein Platz bleibt auch ohne ihn frei (unsichtbar, nicht vorgelesen): Kippt das Muster
        // ins Intensive, rutscht der Status nicht unter den Titel und Kurve und Werkzeugleiste
        // rücken nicht unter dem Finger weg.
        chip = {
            val intense = pattern.isIntense
            Box(Modifier.alpha(if (intense) 1f else 0f).inertWhen(!intense)) {
                CharacterChip(intense = true, activating = false)
            }
        },
        // Eng unter der Startzeile: Kurve und Werkzeugleiste sollen ohne Scrollen ganz dastehen (Nachtrag 5).
        top = Dimens.GapSmall,
        statusReserve = reserve,
        // Play und Ton mittig auf der Titelzeile, auf einer Linie mit dem Chip.
        trailingOnTitle = true,
        trailingAction = controls,
    )
}

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
private fun Toolbar(state: PatternEditorState, focus: ToolbarFocus) {
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
            DashedMark(Modifier.fillMaxWidth().padding(top = Dimens.GapTiny, bottom = Dimens.GapSmall))
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
 * Phasen (Ein/Aus 4 s, Halten 2 s) kommen hinter die gewählte und sind danach gewählt.
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
    AdaptiveButtonRow(
        spacing = Dimens.GapSmall,
        // Die drei Knöpfe sind gleichrangig: keiner steht allein in voller Breite über den anderen.
        primaryFirst = false,
        modifier = Modifier.fillMaxWidth().onFocusChanged { buttonFocused = it.hasFocus },
        leading = {
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
        },
    ) {
        AddButton(R.string.pattern_editor_add_in, R.string.cd_pattern_add_in, colors.secondary, enabled, onClick = { add(FreePhaseType.IN) }, modifier = Modifier.focusRequester(focus.addIn))
        AddButton(R.string.pattern_editor_add_hold, R.string.cd_pattern_add_hold, colors.onSurfaceVariant, enabled, onClick = { add(FreePhaseType.HOLD) }, dashed = true)
        AddButton(R.string.pattern_editor_add_out, R.string.cd_pattern_add_out, BreathExhale, enabled, onClick = { add(FreePhaseType.OUT) })
    }
}

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

/** Nummer einer Phase im Kreis ([background]); bei großer Schrift wächst die Fläche mit, statt Glyphen zu beschneiden (LAYOUT-03). */
@Composable
private fun PhaseNumber(index: Int, background: Color) {
    Box(
        modifier = Modifier
            .defaultMinSize(minWidth = Sizes.IconDefault, minHeight = Sizes.IconDefault)
            .background(background, CircleShape)
            .padding(horizontal = Dimens.GapHairline),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${index + 1}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
        )
    }
}

/**
 * „Ablauf · N von 12 Phasen“, eingeklappt (Zustand per `rememberSaveable`): der Überblick über
 * alle Phasen, für TalkBack und Switch Access auch die Auswahl. Jede Zeile (Nummer, Strich in
 * Phasenfarbe – Halten gestrichelt –, Name, Sekunden) wählt per Tipp ihre Phase, synchron mit Kurve
 * und Werkzeugleiste; darunter der Hinweis zu den Grenzen.
 */
@Composable
private fun Sequence(state: PatternEditorState) {
    var open by rememberSaveable { mutableStateOf(false) }
    val count = state.pattern.phases.size
    CollapsibleSection(
        title = stringResource(R.string.pattern_editor_sequence),
        color = headingColor(1),
        expanded = open,
        onExpandedChange = { open = it },
        status = stringResource(R.string.pattern_editor_sequence_count, count, FreePattern.MAX_PHASES),
        statusDescription = stringResource(R.string.cd_pattern_editor_sequence_count, count, FreePattern.MAX_PHASES),
        smallStatus = true,
        contentSpacing = Dimens.GapSmall,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(Dimens.GapTiny),
            verticalArrangement = Arrangement.spacedBy(Dimens.GapHairline),
        ) {
            state.pattern.phases.indices.forEach { index -> PhaseRow(state, index) }
        }
        HintText(stringResource(R.string.pattern_editor_add_hint))
    }
}

/** Eine Zeile der Ablauf-Liste; die gewählte mit Fläche und Rand. */
@Composable
private fun PhaseRow(state: PatternEditorState, index: Int) {
    val pattern = state.pattern
    val phase = pattern.phases[index]
    val selected = state.selected == index
    val colors = MaterialTheme.colorScheme
    val color = phaseColor(pattern, index, inhale = colors.secondary)
    val shape = MaterialTheme.shapes.small
    val spokenSeconds = pluralStringResource(R.plurals.cd_duration_seconds, phase.seconds, phase.seconds)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (selected) Modifier.background(colors.secondaryContainer).border(Dimens.BorderThin, SegmentSelected, shape) else Modifier)
            .heightIn(min = Dimens.MinTouchTarget)
            .selectable(selected = selected, onClick = { state.select(index) })
            .padding(horizontal = Dimens.GapSmall),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
    ) {
        PhaseNumber(index, background = colors.surfaceVariant)
        PhaseMark(color = color, dashed = phase.type == FreePhaseType.HOLD)
        WholeWordText(
            text = pattern.labelAt(index),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
        )
        // Sichtbar „4 s“, gesprochen „4 Sekunden“ (A11Y-06).
        Text(
            text = stringResource(R.string.pattern_editor_curve_seconds, phase.seconds),
            style = MaterialTheme.typography.titleSmall,
            color = color,
            modifier = Modifier.semantics { text = AnnotatedString(spokenSeconds) },
        )
    }
}

/** Farbe der Phase [index] wie in der Kurve: Ein [inhale], Aus [BreathExhale], Halten nach seiner Höhe. */
private fun phaseColor(pattern: FreePattern, index: Int, inhale: Color): Color = when (pattern.phases[index].type) {
    FreePhaseType.IN -> inhale
    FreePhaseType.OUT -> BreathExhale
    FreePhaseType.HOLD -> if (pattern.levels()[index].from >= HALF) inhale else BreathExhale
}

/** Strich in Phasenfarbe: Ein und Aus gefüllt, Halten als gestrichelter Umriss (FARBE-04, wie die Atemlehre). */
@Composable
private fun PhaseMark(color: Color, dashed: Boolean) {
    Box(
        modifier = Modifier
            .size(width = Dimens.GapTiny, height = Sizes.IconDefault)
            .drawBehind {
                val radius = CornerRadius(Dimens.GapHairline.toPx())
                if (!dashed) {
                    drawRoundRect(color = color, cornerRadius = radius)
                } else {
                    val stroke = Dimens.GapHairline.toPx()
                    val dash = Dimens.GapTiny.toPx()
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(size.width - stroke, size.height - stroke),
                        cornerRadius = radius,
                        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash / 2))),
                    )
                }
            },
    )
}

/** Gelb gestrichelte Linie zwischen gewählter Phase und „Neu nach …“: Dort kommt die neue Phase hin. */
@Composable
private fun DashedMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(Dimens.BorderThin)
            .drawBehind {
                val dash = Dimens.GapTiny.toPx()
                drawLine(
                    color = WarnAmber.copy(alpha = TERTIARY),
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                )
            },
    )
}

/** Hinweiskarte bei intensiven Mustern: Gründe und Sicherheitssatz ([FreePattern.intensityNote]). */
@Composable
private fun IntenseNote(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.ListGap)
            .border(accentBorder(WarnAmber), MaterialTheme.shapes.small)
            .padding(Dimens.CardPadding),
    ) {
        BodyText("⚠ $text", style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Lesezeichen im Kopf (MUSTER-02): leer → gefüllt in `secondary`, Zustand per `stateDescription`
 * und Live-Region (nur für den eigenen Tipp, nicht wenn eine Wertänderung es leert). Bei einem
 * Muster, das es vor dem Öffnen schon gab, schärft der erste Tipp auf das gefüllte Lesezeichen nur
 * (Papierkorb mit Kreuz in `error`, „‚Name‘ wirklich löschen?“), erst der zweite entfernt es
 * (MUSTER-05).
 */
@Composable
private fun SaveToggle(state: PatternEditorState, onSavedChange: (Boolean) -> Unit) {
    val saved = state.saved
    val confirm = rememberArmedConfirm()
    // Geänderte Werte leeren das Lesezeichen: Eine halb bestätigte Löschung gilt dann nicht mehr.
    LaunchedEffect(saved) { if (!saved) confirm.disarm() }
    val armed = confirm.armed && saved
    // Eine Ansage je Tipp (A11Y-04): Die Live-Region gilt nur ab dem eigenen Tipp und nur, bis sich
    // Phasen, Dauer oder Name ändern. Leert oder füllt eine solche Änderung das Lesezeichen, sagt
    // also nur der geänderte Wert etwas.
    val draft = PatternSnapshot(state.pattern, state.minutes, state.savedName)
    var announce by remember(draft) { mutableStateOf(false) }
    AppIconToggle(
        checked = saved,
        announce = announce,
        onCheckedChange = { checked ->
            announce = true
            when {
                checked -> onSavedChange(true)
                state.existedBefore -> confirm.tap { onSavedChange(false) }
                else -> onSavedChange(false)
            }
        },
        stateDescription = stringResource(if (saved) R.string.state_saved else R.string.state_not_saved),
        modifier = confirm.focusModifier,
    ) {
        when {
            armed -> Icon(
                painterResource(R.drawable.ic_delete_forever),
                contentDescription = stringResource(R.string.cd_delete_confirm, state.snapshot?.name ?: state.savedName),
                tint = MaterialTheme.colorScheme.error,
            )

            else -> Icon(
                painterResource(if (saved) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark),
                contentDescription = stringResource(R.string.cd_pattern_save),
                tint = if (saved) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
            )
        }
    }
}

/**
 * Name: einzeilig, scrollt waagrecht, höchstens [PatternEditorState.NAME_MAX] Zeichen. Leer steht
 * der Vorschlagsname als Platzhalter da – so wird auch gespeichert. Zeichen, die der Import nicht
 * annimmt (Steuer- und Formatzeichen, Emoji außerhalb der Grundebene), nimmt das Feld gar nicht
 * erst an; Zeilenumbrüche aus der Zwischenablage werden Leerzeichen ([nameInput]).
 */
@Composable
private fun NameSection(state: PatternEditorState) {
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.pattern_editor_name), color = headingColor(2))
        val keyboard = LocalSoftwareKeyboardController.current
        var focused by remember { mutableStateOf(false) }
        val colors = MaterialTheme.colorScheme
        // Wie das Suchfeld: Die Tastatur lernt den Namen nicht (SEC-PRIV-06).
        NoPersonalizedLearning {
            BasicTextField(
                value = state.name,
                onValueChange = { state.updateName(nameInput(it)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focused = it.hasFocus },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onBackground),
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    autoCorrectEnabled = false,
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done,
                ),
                // Wie das Suchfeld: nur die Tastatur ausblenden, der Fokus bleibt im Feld – Tastatur-
                // und D-Pad-Nutzer machen dort weiter, wo sie waren (A11Y-07).
                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                decorationBox = { inner ->
                    // Label im Feld: So liest TalkBack es mit dem Inhalt als einen Knoten.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Dimens.MinTouchTarget)
                            .border(Dimens.BorderThin, if (focused) colors.primary else colors.outline, MaterialTheme.shapes.extraSmall)
                            .padding(horizontal = Dimens.CardPadding, vertical = Dimens.GapSmall),
                        verticalArrangement = Arrangement.spacedBy(Dimens.GapHairline),
                    ) {
                        Text(
                            text = stringResource(R.string.pattern_editor_name_label),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.onBackground.copy(alpha = SECONDARY),
                        )
                        Box {
                            if (state.name.isEmpty()) {
                                Text(
                                    text = state.suggestedName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onBackground.copy(alpha = SECONDARY),
                                    maxLines = 1,
                                )
                            }
                            inner()
                        }
                    }
                },
            )
        }
    }
}

/**
 * „Aus der Yoga-Lehre“: Teaser immer ganz und ohne Live-Region – die eine Ansage je Änderung ist
 * der Stepper-Wert (A11Y-04), TalkBack liest die Einordnung beim Weiterwischen. Aufgeklappt das
 * Detail und je Karte ein Verweis „… in der Atemlehre ›“.
 */
@Composable
private fun Lore(exercise: Exercise, onOpenLore: (String) -> Unit) {
    val lore = exercise.lore ?: return
    val accent = exercise.family.color()
    val patterns = (listOf(lore.patternId) + lore.morePatternIds).mapNotNull(Atemlehre::pattern)
    Column(Modifier.fillMaxWidth()) {
        // Der Expander polstert seinen Kopf selbst um ListGap; zusammen steht er so weit vom Namen wie ein Abschnittskopf.
        Spacer(Modifier.height(Dimens.SectionHeaderTop - Dimens.ListGap))
        ExpanderSection(
            title = stringResource(R.string.detail_lore),
            appetizer = lore.teaser,
            accent = headingColor(3),
            divider = false,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
                BodyText(lore.detail)
                patterns.forEach { pattern ->
                    AppTextButton(onClick = { onOpenLore(pattern.id) }) {
                        ForwardLinkText(text = stringResource(R.string.detail_lore_link, pattern.linkName), color = accent)
                    }
                }
            }
        }
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
private class ToolbarFocus {
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

/**
 * Nimmt nur, was auch der Import als Namen annimmt ([PatternBackup.cleanName]): Leerraum wird zum
 * Leerzeichen, verborgene oder trennende Zeichen (auch die Hälften von Emoji außerhalb der
 * Grundebene) fallen weg, statt erst beim Speichern still zu Leerzeichen zu werden.
 */
internal fun nameInput(raw: String): String = buildString {
    raw.forEach { char ->
        when {
            char.isWhitespace() -> append(' ')
            PatternBackup.cleanName(char.toString()).isNotEmpty() -> append(char)
        }
    }
}

/** Spalten der Vorlagen-Auswahl: sechs Vorlagen in drei Reihen, auch bei großer Schrift einzeilig. */
private const val TEMPLATE_COLUMNS = 2

/** Test-Tag der Werkzeugleiste (gewählte Phase und Hinzufügen) – für die Messung „ohne Scrollen sichtbar“. */
internal const val PATTERN_TOOLBAR_TAG = "pattern_editor_toolbar"

/**
 * Breitester ruhender Status „· 88 s · 88,8/min“ für den Platz am Kopf „Atemzug“: Die Ziffern der
 * Schrift sind gleich breit, also deckt er jede zweistellige Länge und jede Rate ab.
 */
private const val WIDEST_CYCLE_SECONDS = 88
private const val WIDEST_RATE = "88,8"

/** Ab halb voll gehört ein Halten zur Einatmen-Seite (wie die Kurve). */
private const val HALF = 0.5f

private const val SECONDS_PER_MINUTE = 60
private const val HALF_MINUTE = 30

@Preview(heightDp = 1600)
@Composable
private fun PatternEditorScreenPreview() {
    AtemkraftTheme {
        val state = rememberPatternEditorState { PatternEditorState.new(it, FreePatternTemplates.all[2].pattern) }
        PatternEditorScreen(state = state, onSavedChange = {}, onStart = { _, _ -> }, onOpenLore = {}, onBack = {})
    }
}
