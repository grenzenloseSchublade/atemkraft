package app.atemkraft.ui.patterns

import android.content.Context
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import app.atemkraft.R
import app.atemkraft.cue.AppPreviewSoundOutput
import app.atemkraft.cue.PreviewSoundOutput
import app.atemkraft.data.Atemlehre
import app.atemkraft.data.CueSettings
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.FreePatternTemplates
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.approxMinutes
import app.atemkraft.playback.PhaseAudio
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.ExpanderSection
import app.atemkraft.ui.components.FillViewportColumn
import app.atemkraft.ui.components.ForwardLinkText
import app.atemkraft.ui.components.PinnedActionBar
import app.atemkraft.ui.components.PushHeader
import app.atemkraft.ui.components.SafetyDialog
import app.atemkraft.ui.components.accentBorder
import app.atemkraft.ui.components.color
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.patterns.curve.BreathCurveEditor
import app.atemkraft.ui.patterns.curve.CurvePreviewControls
import app.atemkraft.ui.patterns.curve.rememberBreathCurveHeights
import app.atemkraft.ui.patterns.curve.rememberCurvePreviewState
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.WarnAmber

/**
 * Muster-Editor „Eigenes Muster“ (MUSTER-10, Nachtrag 4 „Frei gestalten“, 5 „Kompakt“, 6 „Leiste unten“),
 * Push-Seite. Von oben: Kopf mit Lesezeichen · „Atemzug“ mit kurzem Status (bzw. der
 * Phase der laufenden Vorschau), ggf. „intensiv“ und rechts Play/Pause und Ton-Schalter · die
 * Atemkurve (keine eigene Vorschau-Zeile) · direkt darunter die Werkzeugleiste (gewählte Phase mit
 * Sekunden-Stepper und Papierkorb, gelb gestrichelt getrennt „Neu nach 2:“ mit + Ein / + Halten /
 * + Aus). Ziel: Kurve und Werkzeugleiste stehen ohne Scrollen ganz da – die Kurve hat ihre volle
 * Höhe, wo Platz ist, sonst so viel, dass alles passt, nie weniger als ihre Mindesthöhe
 * ([FirstScreen]; reicht der Platz dafür mit der Tab-Leiste nicht, blendet der Editor sie aus).
 * Darunter Hinweis bei intensiven Mustern, „Ablauf“ (eingeklappt, Überblick und TalkBack),
 * Name, „Aus der Yoga-Lehre“; unten fest eine Leiste ([ActionBar], Nachtrag 6): Startpunkt (Vorlagen
 * als Bottom Sheet), Dauer, Zurücksetzen und der große Play-Knopf.
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
    var showSafety by rememberSaveable { mutableStateOf(false) }
    var showTemplates by rememberSaveable { mutableStateOf(false) }
    var showRename by rememberSaveable { mutableStateOf(false) }
    val toolbarFocus = remember { ToolbarFocus() }
    val startFocus = remember { StartFocus() }
    val start = {
        preview.pause()
        onStart(exercise, config)
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            val scrollState = rememberScrollState()
            PushHeader(
                onBack = onBack,
                titleContent = { NameTitle(state, onRename = { showRename = true }) },
                action = { SaveToggle(state, onSavedChange) },
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
                        header = {
                            StartRow(
                                state = state,
                                phase = phaseIndex.takeIf { preview.playing },
                                focus = startFocus,
                                onPickStart = {
                                    preview.pause()
                                    showTemplates = true
                                },
                            ) {
                                CurvePreviewControls(preview) { PreviewSoundSwitch(sound) }
                            }
                        },
                        curve = {
                            BreathCurveEditor(
                                pattern = pattern,
                                selected = state.selected,
                                onSelect = { it?.let(state::select) },
                                onChangeSeconds = state::setSeconds,
                                modifier = Modifier.fillMaxWidth(),
                                preview = preview,
                            )
                        },
                        toolbar = { Toolbar(state, toolbarFocus) },
                    )
                    pattern.intensityNote()?.let { IntenseNote(it) }
                    Sequence(state)
                    Lore(exercise, onOpenLore)
                    Spacer(Modifier.height(Dimens.ScreenBottom))
                }
            }
            PinnedActionBar {
                ActionBar(
                    state = state,
                    approxMinutes = approxMinutes,
                    focus = startFocus,
                    onStart = { if (exercise.caution && requireSafetyConfirm) showSafety = true else start() },
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
 * Der erste Bildschirm des Editors (Nachtrag 5 und 6): Startzeile [header], Atemkurve [curve]
 * und Werkzeugleiste [toolbar] untereinander. Die Kurve bekommt die Höhe, mit der alles zusammen in
 * die sichtbare Höhe [viewport] (px) der Scroll-Fläche passt – bevorzugt ihre volle, auf kleinen
 * Bildschirmen weniger, nie unter ihre Mindesthöhe ([BreathCurveHeights.fit]); reicht auch die
 * nicht, scrollt die Seite. Die Tab-Leiste steht im Editor nie (Tiefe, Nutzer 2026-10-23), ihre
 * Höhe gehört der Kurve.
 *
 * Kopf und Werkzeugleiste behalten beim Bearbeiten ihre Höhe (Platz für die längsten Texte
 * reserviert), also auch die Kurve – nichts springt beim Auswählen, Hinzufügen oder Entfernen.
 */
@Composable
private fun FirstScreen(
    viewport: Int,
    header: @Composable () -> Unit,
    curve: @Composable () -> Unit,
    toolbar: @Composable () -> Unit,
) {
    val heights = rememberBreathCurveHeights()
    FillViewportColumn(
        viewportHeight = viewport,
        centerHeight = { free -> if (free == Constraints.Infinity) heights.preferred.roundToPx() else heights.fit(free.toDp()).roundToPx() },
        top = header,
        center = curve,
        bottom = toolbar,
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
            accent = headingColor(1),
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

/** Test-Tag der Werkzeugleiste (gewählte Phase und Hinzufügen) – für die Messung „ohne Scrollen sichtbar“. */
internal const val PATTERN_TOOLBAR_TAG = "pattern_editor_toolbar"

@Preview(heightDp = 1600)
@Composable
private fun PatternEditorScreenPreview() {
    AtemkraftTheme {
        val state = rememberPatternEditorState { PatternEditorState.new(it, FreePatternTemplates.all[2].pattern) }
        PatternEditorScreen(state = state, onSavedChange = {}, onStart = { _, _ -> }, onOpenLore = {}, onBack = {})
    }
}
