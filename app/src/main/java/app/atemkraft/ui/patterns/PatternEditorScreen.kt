package app.atemkraft.ui.patterns

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.cue.PhaseAudio
import app.atemkraft.data.Atemlehre
import app.atemkraft.data.CueSettings
import app.atemkraft.data.SafetySettings
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.approxMinutes
import app.atemkraft.playback.AppPreviewSoundOutput
import app.atemkraft.playback.PreviewSoundOutput
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.ExpanderSection
import app.atemkraft.ui.components.FillViewportColumn
import app.atemkraft.ui.components.ForwardLinkText
import app.atemkraft.ui.components.PinnedActionBar
import app.atemkraft.ui.components.PushHeader
import app.atemkraft.ui.components.WarnText
import app.atemkraft.ui.components.accentBorder
import app.atemkraft.ui.components.rememberSafetyGate
import app.atemkraft.ui.patterns.curve.BreathCurveEditor
import app.atemkraft.ui.patterns.curve.rememberBreathCurveHeights
import app.atemkraft.ui.patterns.curve.rememberCurvePreviewState
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.WarnAmber
import app.atemkraft.ui.theme.rememberAnimationsEnabled

/** Was der Muster-Editor auslöst (CODE-02); Bedeutung siehe [PatternEditorScreen]. */
@Immutable
data class PatternEditorActions(
    val onSavedChange: (Boolean) -> Unit,
    val onStart: (Exercise, SessionConfig) -> Unit,
    val onOpenLore: (patternId: String) -> Unit,
    val onBack: () -> Unit,
    val onConfirmedSafety: (exerciseId: String) -> Unit = {},
)

/**
 * Muster-Editor „Eigenes Muster“ (MUSTER-10, Nachtrag 4 „Frei gestalten“, 5 „Kompakt“, 6 „Leiste unten“),
 * Push-Seite. Von oben: Kopf mit dem Namen als Seitentitel, rechts Startpunkt ([StartButton],
 * öffnet die Vorlagen) und Speicherzeichen · Werte ohne Tippfläche ([InfoLine]: „Box · 16 s je
 * Atemzug · 3,8/min“, ggf. „intensiv“; während der Vorschau die Phase) · die Kurvenkarte
 * ([CurveCard]: die Atemkurve und am unteren Rand derselben Karte die Vorschau, [PreviewRow]:
 * Play links, Ton-Schalter rechts, bündig mit den Innenkanten der Kurve; Nutzer 2026-10-23: über
 * der Kurve keine Knöpfe, 2026-10-24: in die Karte eingebunden) · die Werkzeugleiste (gewählte Phase mit Sekunden-Stepper und
 * Papierkorb, gelb gestrichelt getrennt „Nach Phase 2:“ mit + Ein / + Halten / + Aus). Ziel: Kurve
 * und Werkzeugleiste stehen ohne Scrollen ganz da – die Kurve hat ihre volle Höhe, wo Platz ist,
 * sonst so viel, dass alles passt, nie weniger als ihre Mindesthöhe ([FirstScreen]; die
 * Tab-Leiste ist in der Tiefe eingeklappt). Darunter Hinweis bei intensiven Mustern, „Ablauf“
 * (eingeklappt, Überblick und TalkBack), „Aus der Yoga-Lehre“; unten fest eine Leiste
 * ([ActionBar], Nachtrag 6): Dauer, Zurücksetzen und der große Play-Knopf.
 *
 * Die Seite liest und ändert nur [state] (ARC-7). Was gespeichert wird, entscheidet der Aufrufer:
 * [PatternEditorActions.onSavedChange] (true = speichern bzw. denselben Eintrag aktualisieren,
 * false = aus „Meine Muster“ entfernen) schreibt ins Repository und meldet das Ergebnis mit
 * [PatternEditorState.markSaved] bzw. [PatternEditorState.markRemoved] zurück. Ein Muster, das es
 * schon vor dem Öffnen gab, entfernt das Speicherzeichen nur zweistufig (MUSTER-05); ein in dieser
 * Sitzung angelegtes nimmt ein erneuter Tipp sofort wieder heraus (KOMP-02).
 *
 * [PatternEditorActions.onStart] probiert den Entwurf ohne Speichern aus (Übung und Dauer als
 * [SessionConfig]); die Seite bleibt dabei offen. Ein intensives Muster startet erst nach dem
 * Sicherheitsdialog, wenn [safety] es verlangt ([SafetySettings.requiresConfirm], dieselbe Regel
 * wie auf der Detailseite; [rememberSafetyGate]); die Bestätigung meldet
 * [PatternEditorActions.onConfirmedSafety]. [PatternEditorActions.onOpenLore] öffnet eine Karte
 * der Atemlehre.
 *
 * Ton der Vorschau ([rememberPreviewSound]): Klang-Einstellungen der App [cueSettings]; still,
 * solange eine Übung oder Meditation läuft ([otherAudioActive]). [soundOutput] tauschen nur Tests.
 */
@Composable
fun PatternEditorScreen(
    state: PatternEditorState,
    actions: PatternEditorActions,
    cueSettings: CueSettings = CueSettings(),
    otherAudioActive: Boolean = false,
    safety: SafetySettings = SafetySettings(),
    soundOutput: (Context) -> PreviewSoundOutput = ::AppPreviewSoundOutput,
) {
    val exercise = state.exercise()
    val config = state.config()
    val approxMinutes = remember(exercise, config) { exercise.approxMinutes(config) }

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
    var showTemplates by rememberSaveable { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    val toolbarFocus = remember { ToolbarFocus() }
    val startFocus = remember { StartFocus() }
    val start = rememberSafetyGate(
        require = safety.requiresConfirm(exercise),
        cautions = exercise.cautions,
        // Bestätigt gilt für dieses Muster, nicht für alle intensiven Übungen (MUSTER-06).
        onConfirm = { actions.onConfirmedSafety(exercise.id) },
    ) {
        preview.pause()
        actions.onStart(exercise, config)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()
            PushHeader(
                onBack = actions.onBack,
                titleContent = { NameTitle(state, onRename = { showRename = true }) },
                // Fester Bereichsname statt des Musternamens: Umbenennen soll keine neue Seite ansagen.
                paneTitle = stringResource(R.string.pattern_editor_title),
                action = {
                    // Startpunkt links neben dem Speicherzeichen (Nutzer 2026-10-23): Über der Kurve stehen nur Werte.
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StartButton(state, startFocus) {
                            preview.pause()
                            showTemplates = true
                        }
                        SaveToggle(state, actions.onSavedChange)
                    }
                },
                scrolled = scrollState.canScrollBackward,
            )
            // Sichtbare Höhe der Scroll-Fläche: Danach richtet sich die Höhe der Kurve ([FirstScreen]).
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth()) {
                val viewport = constraints.maxHeight
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = Dimens.ScreenPadding),
                ) {
                    FirstScreen(
                        viewport = viewport,
                        header = { InfoLine(state, preview) },
                        curveCard = {
                            CurveCard(
                                curve = {
                                    BreathCurveEditor(
                                        pattern = pattern,
                                        selected = state.selected,
                                        onSelect = { it?.let(state::select) },
                                        onChangeSeconds = state::setSeconds,
                                        // Die Kurve nimmt in der Karte die Resthöhe über der Vorschau-Zeile.
                                        modifier = Modifier.fillMaxWidth().weight(1f),
                                        preview = preview,
                                    )
                                },
                                preview = { PreviewRow(preview, sound) },
                            )
                        },
                        toolbar = { Toolbar(state, toolbarFocus) },
                    )
                    pattern.intensityNote()?.let { IntenseNote(it) }
                    Sequence(state)
                    Lore(exercise, actions.onOpenLore)
                    Spacer(Modifier.height(Dimens.TabBarGripClearance))
                }
            }
            PinnedActionBar {
                ActionBar(
                    state = state,
                    approxMinutes = approxMinutes,
                    focus = startFocus,
                    onStart = start,
                )
            }
        }
    }

    if (showTemplates) {
        TemplateSheet(
            state = state,
            onPick = { startFocus.picked(state.pattern) },
            onDismiss = { showTemplates = false },
        )
    }

    if (showRename) RenameDialog(state, onDismiss = { showRename = false })
}

/**
 * Ton der Phase [index] von [exercise] (Typ und Füllstand wie in der Übung), Restdauer [remainingMs];
 * mitten in der Phase geht der Dauerton an der Tonhöhe dieser Stelle weiter (S-19).
 */
private fun phaseAudio(exercise: Exercise, index: Int, remainingMs: Long): PhaseAudio {
    val phase = exercise.segments.firstOrNull()?.phases?.getOrNull(index)
        ?: return PhaseAudio(PhaseType.INHALE, remainingMs, open = false)
    val elapsedMs = (phase.duration as? PhaseDuration.Fixed)?.let { (it.millis - remainingMs).coerceAtLeast(0L) } ?: 0L
    return PhaseAudio(phase.type, remainingMs, open = false, level = phase.level, elapsedMs = elapsedMs)
}

/**
 * Der erste Bildschirm des Editors (Nachtrag 5 und 6): Werte [header], die Kurvenkarte [curveCard]
 * (Atemkurve und am unteren Rand die Vorschau, [CurveCard]; Nutzer 2026-10-24) und die
 * Werkzeugleiste [toolbar]. Die Kurve bekommt die Höhe, mit der alles zusammen in die sichtbare
 * Höhe [viewport] (px) der Scroll-Fläche passt – bevorzugt ihre volle, auf kleinen Bildschirmen
 * weniger, nie unter ihre Mindesthöhe ([BreathCurveHeights.fit]; Vorschau und Werkzeugleiste zählen
 * beide mit: Die Karte ist um [PREVIEW_ROW_HEIGHT] höher als die Kurve, solange die Vorschau da ist);
 * reicht auch die nicht, scrollt die Seite. Die Tab-Leiste ist in der Tiefe eingeklappt (Nutzer
 * 2026-10-23), ihre Höhe gehört der Kurve; klappt der Nutzer sie auf, wird die Kurve flacher.
 *
 * Werte, Vorschau und Werkzeugleiste behalten beim Bearbeiten ihre Höhe (Platz für die längsten
 * Texte reserviert), also auch die Kurve – nichts springt beim Auswählen, Hinzufügen oder Entfernen.
 */
@Composable
private fun FirstScreen(
    viewport: Int,
    header: @Composable () -> Unit,
    curveCard: @Composable () -> Unit,
    toolbar: @Composable () -> Unit,
) {
    val heights = rememberBreathCurveHeights()
    // Ohne System-Animationen fehlt die Vorschau-Zeile (MOTION-02), die Karte endet mit der Kurve.
    val previewRow = if (rememberAnimationsEnabled()) PREVIEW_ROW_HEIGHT else 0.dp
    // Unter der Werkzeugleiste bleibt die Tippfläche des Griffs der Tab-Leiste frei (`TabBarToggle`
    // schwebt über der Startleiste, sonst läge sie über „+ Halten“) – solange die Kurve dann noch
    // ihre Mindesthöhe hat; sonst geht MUSTER-10 vor.
    val gripClearance = with(LocalDensity.current) { Dimens.TabBarGripClearance.roundToPx() }
    FillViewportColumn(
        viewportHeight = viewport,
        centerHeight = { free ->
            val row = previewRow.roundToPx()
            if (free == Constraints.Infinity) heights.preferred.roundToPx() + row else heights.fit((free - row).toDp()).roundToPx() + row
        },
        top = header,
        center = curveCard,
        bottom = toolbar,
        bottomReserve = gripClearance,
    )
}

/**
 * Kurvenkarte des Editors: eine Fläche (`surface`, `shapes.medium`) um die Atemkurve [curve] und
 * darunter, am unteren Rand derselben Karte, die Vorschau [preview] ([PreviewRow]: Play links, Ton
 * rechts, bündig mit den Innenkanten der Kurve) – Knöpfe und Kurve lesen sich als ein Element
 * (Nutzer 2026-10-24: „sauber optisch in die Graph-Karte eingebunden“). Kein Polster: Die Kurve
 * trägt ihre Innenränder selbst, die Vorschau ihre Tippflächen.
 */
@Composable
private fun CurveCard(curve: @Composable ColumnScope.() -> Unit, preview: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium),
    ) {
        curve()
        preview()
    }
}

/**
 * Hinweiskarte bei intensiven Mustern: Gründe und Sicherheitssatz ([FreePattern.intensityNote]) –
 * eine Karte wie jede andere (KOMP-03: Fläche, `shapes.medium`, `CardPadding`), Rand
 * `accentBorder(WarnAmber)`, der Text als ⚠-Zeile in `WarnAmber` wie auf der Detailseite ([WarnText],
 * FARBE-04).
 */
@Composable
private fun IntenseNote(text: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.ListGap),
        border = accentBorder(WarnAmber),
    ) {
        WarnText(text, modifier = Modifier.padding(Dimens.CardPadding))
    }
}

/**
 * „Aus der Yoga-Lehre“: Teaser immer ganz und ohne Live-Region – die eine Ansage je Änderung ist
 * der Stepper-Wert (A11Y-04), TalkBack liest die Einordnung beim Weiterwischen. Aufgeklappt das
 * Detail und je Karte ein Verweis „… in der Atemlehre ›“ in `secondary` – Links im Editor tragen
 * keine Familienfarbe (MUSTER-10).
 */
@Composable
private fun Lore(exercise: Exercise, onOpenLore: (String) -> Unit) {
    val lore = exercise.lore ?: return
    val linkColor = MaterialTheme.colorScheme.secondary
    val patterns = (listOf(lore.patternId) + lore.morePatternIds).mapNotNull(Atemlehre::pattern)
    Column(Modifier.fillMaxWidth()) {
        // Der Expander polstert seinen Kopf selbst um ListGap; zusammen steht er so weit vom Namen wie ein Abschnittskopf.
        Spacer(Modifier.height(Dimens.SectionHeaderTop - Dimens.ListGap))
        ExpanderSection(
            title = stringResource(R.string.detail_lore),
            appetizer = lore.teaser,
            divider = false,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
                BodyText(lore.detail)
                patterns.forEach { pattern ->
                    AppTextButton(onClick = { onOpenLore(pattern.id) }) {
                        ForwardLinkText(text = stringResource(R.string.detail_lore_link, pattern.linkName), color = linkColor)
                    }
                }
            }
        }
    }
}

/** Test-Tag der Werkzeugleiste (gewählte Phase und Hinzufügen) – für die Messung „ohne Scrollen sichtbar“. */
internal const val PATTERN_TOOLBAR_TAG = "pattern_editor_toolbar"

@Preview(heightDp = 1600)
@Composable
private fun PatternEditorScreenPreview() {
    AtemkraftTheme {
        val state = rememberPatternEditorState { PatternEditorState.new(it, FreePatternTemplates.all[2].pattern) }
        PatternEditorScreen(state = state, actions = PatternEditorActions(onSavedChange = {}, onStart = { _, _ -> }, onOpenLore = {}, onBack = {}))
    }
}
