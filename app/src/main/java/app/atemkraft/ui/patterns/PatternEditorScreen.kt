package app.atemkraft.ui.patterns

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
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
import app.atemkraft.ui.components.ExpanderSection
import app.atemkraft.ui.components.ForwardLinkText
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.NoPersonalizedLearning
import app.atemkraft.ui.components.PinnedActionBar
import app.atemkraft.ui.components.PushHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SegmentedChoiceRow
import app.atemkraft.ui.components.StartSplitButton
import app.atemkraft.ui.components.Stepper
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.accentBorder
import app.atemkraft.ui.components.color
import app.atemkraft.ui.components.dashedBorder
import app.atemkraft.ui.components.headingColor
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
 * Muster-Editor „Eigenes Muster“ (MUSTER-10, Nachtrag 4 „Frei gestalten“), Push-Seite. Von oben:
 * Kopf mit Lesezeichen, „Startpunkt“ (eingeklappt, Vorlagen), Dauer, „Atemzug“ mit Status und
 * ggf. „intensiv“ samt Atemkurve und Vorschau (Play/Pause, Phasenname, Ton-Schalter), „Ablauf“
 * als Liste der Phasen (Auswahl, Sekunden, Entfernen, Einfügemarke), „Phase hinzufügen“,
 * Hinweis bei intensiven Mustern, Name, „Aus der Yoga-Lehre“; unten fest „Starten · ca. N min“
 * mit Reset auf den Stand beim Öffnen.
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

    // Vorschau unter der Kurve und ihr Ton: hält an, wenn die Seite verdeckt wird oder die App in
    // den Hintergrund geht; der Ton folgt der Phase, in der der Punkt gerade steht.
    val preview = rememberCurvePreviewState()
    val sound = rememberPreviewSound(cueSettings, otherAudioActive, onHide = preview::pause, output = soundOutput)
    val pattern = state.pattern
    val phaseIndex by remember(pattern) { derivedStateOf { preview.phaseIndexIn(pattern) } }
    PreviewSoundEffect(sound, preview.playing, phaseIndex) {
        phaseAudio(state.exercise(), phaseIndex, preview.remainingMillisIn(state.pattern))
    }

    // Offener Dialog überlebt Rotation und Prozess-Neustart (CODE-03).
    var showSafety by rememberSaveable { mutableStateOf(false) }
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
                StartingPoint(state)
                Duration(state)
                Breath(state) {
                    BreathCurveEditor(
                        pattern = pattern,
                        selected = state.selected,
                        onSelect = state::select,
                        onChangeSeconds = state::setSeconds,
                        modifier = Modifier.fillMaxWidth(),
                        preview = preview,
                        previewTrailing = { PreviewSoundSwitch(sound) },
                    )
                }
                Sequence(state)
                AddPhase(state)
                pattern.intensityNote()?.let { IntenseNote(it) }
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
 * „Startpunkt“: eingeklappt, man braucht ihn nur zum Anfang. Der Kopf nennt die Vorlage, der die
 * Folge gerade entspricht, sonst „angepasst“; aufgeklappt die Vorlagen als Auswahlreihe im
 * Raster. Eine Vorlage setzt die Folge und klappt den Abschnitt wieder zu.
 */
@Composable
private fun StartingPoint(state: PatternEditorState) {
    var open by rememberSaveable { mutableStateOf(false) }
    val template = state.template
    val modified = stringResource(R.string.adjust_modified)
    CollapsibleSection(
        title = stringResource(R.string.pattern_editor_start),
        color = headingColor(0),
        expanded = open,
        onExpandedChange = { open = it },
        status = "· ${template?.label ?: modified}",
        statusDescription = template?.label ?: modified,
        smallStatus = true,
        contentSpacing = Dimens.GapSmall,
    ) {
        SegmentedChoiceRow<FreeTemplate?>(
            items = FreePatternTemplates.all,
            selected = template,
            onSelect = { picked ->
                picked?.let(state::applyTemplate)
                open = false
            },
            // Ohne eigenen Gruppennamen: Der aufklappbare Kopf „Startpunkt“ steht direkt darüber.
            columns = TEMPLATE_COLUMNS,
        ) { it?.label.orEmpty() }
    }
}

/** Dauer in Minuten (1–30); sie ändert nie die Folge. */
@Composable
private fun Duration(state: PatternEditorState) {
    Card(modifier = Modifier.fillMaxWidth().padding(top = Dimens.GapSmall)) {
        Stepper(
            label = stringResource(R.string.adjust_minutes),
            value = state.minutes,
            range = AdjustLimits.MINUTES,
            onChange = { state.minutes = it },
            vertical = Dimens.GapSmall,
            spokenLabel = stringResource(R.string.cd_adjust_duration),
            valueUnit = R.plurals.cd_duration_minutes,
        )
    }
}

/**
 * „Atemzug“: Länge eines Durchlaufs und Atemzüge pro Minute als Status am Kopf, bei einem
 * intensiven Muster dazu „intensiv“ (`CharacterChip`); darunter die Kurve ([curve]).
 */
@Composable
private fun Breath(state: PatternEditorState, curve: @Composable () -> Unit) {
    val pattern = state.pattern
    val total = pattern.totalSeconds
    val rate = pattern.breathsPerMinuteText()
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.pattern_editor_breath),
            color = headingColor(1),
            status = stringResource(R.string.pattern_editor_cycle, total, rate),
            statusDescription = pluralStringResource(R.plurals.cd_pattern_editor_cycle, total, total, rate),
            smallStatus = true,
            trailingAction = if (pattern.isIntense) {
                {
                    // Die Hülle des Kopfs ragt für Icon-Buttons um TouchOverhang über die Kante;
                    // der Chip steht bündig an der Inhaltskante und auf der Grundlinie des Titels.
                    Box(Modifier.padding(end = Sizes.TouchOverhang, bottom = Dimens.GapSmall)) {
                        CharacterChip(intense = true, activating = false)
                    }
                }
            } else {
                null
            },
        )
        curve()
    }
}

/**
 * „Ablauf“: jede Phase eine Zeile (Nummer, Strich in Phasenfarbe – Halten gestrichelt –, Name,
 * Sekunden). Ein Tipp wählt sie, ein zweiter hebt die Auswahl auf. Die gewählte Zeile trägt den
 * Papierkorb und einen Stepper „Sekunden“ – so ist alles ohne Ziehen bedienbar (TalkBack, Switch
 * Access). Unter der gewählten Zeile, ohne Auswahl unter der letzten, zeigt eine gelb
 * gestrichelte Marke, wo eine neue Phase hinkommt.
 */
@Composable
private fun Sequence(state: PatternEditorState) {
    val pattern = state.pattern
    val count = pattern.phases.size
    val insertAt = state.selected ?: pattern.phases.lastIndex
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.pattern_editor_sequence),
            color = headingColor(2),
            status = stringResource(R.string.pattern_editor_sequence_count, count, FreePattern.MAX_PHASES),
            statusDescription = stringResource(R.string.cd_pattern_editor_sequence_count, count, FreePattern.MAX_PHASES),
            smallStatus = true,
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
                .padding(Dimens.GapTiny),
            verticalArrangement = Arrangement.spacedBy(Dimens.GapHairline),
        ) {
            pattern.phases.indices.forEach { index ->
                PhaseRow(state, index)
                if (index == insertAt) InsertMarker()
            }
        }
    }
}

/** Eine Zeile der Ablauf-Liste; gewählt mit Fläche, Rand, Papierkorb und Stepper. */
@Composable
private fun PhaseRow(state: PatternEditorState, index: Int) {
    val pattern = state.pattern
    val phase = pattern.phases[index]
    val selected = state.selected == index
    val colors = MaterialTheme.colorScheme
    val color = phaseColor(pattern, index, inhale = colors.secondary)
    val label = pattern.labelAt(index)
    val shape = MaterialTheme.shapes.small
    val spokenSeconds = pluralStringResource(R.plurals.cd_duration_seconds, phase.seconds, phase.seconds)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .then(if (selected) Modifier.background(colors.secondaryContainer).border(Dimens.BorderThin, SegmentSelected, shape) else Modifier),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = Dimens.MinTouchTarget)
                    .selectable(selected = selected, onClick = { state.toggleSelection(index) })
                    .padding(horizontal = Dimens.GapSmall),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
            ) {
                // Fläche statt Zuschnitt: Bei großer Schrift ragt eine zweistellige Nummer sonst
                // in die runden Ecken und verlöre Glyphen (LAYOUT-03).
                Box(
                    modifier = Modifier
                        .defaultMinSize(minWidth = Sizes.IconDefault, minHeight = Sizes.IconDefault)
                        .background(colors.surfaceVariant, CircleShape)
                        .padding(horizontal = Dimens.GapHairline),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "${index + 1}",
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurface.copy(alpha = SECONDARY),
                    )
                }
                PhaseMark(color = color, dashed = phase.type == FreePhaseType.HOLD)
                WholeWordText(
                    text = label,
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
            if (selected) {
                val removable = state.canRemove(index)
                AppIconButton(onClick = { state.remove(index) }, enabled = removable) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.cd_pattern_phase_remove, index + 1),
                        tint = if (removable) colors.error else colors.onSurface.copy(alpha = TERTIARY),
                    )
                }
            }
        }
        if (selected) {
            Stepper(
                label = stringResource(R.string.pattern_editor_seconds),
                value = phase.seconds,
                range = FreePattern.SECONDS_RANGE,
                onChange = { state.setSeconds(index, it) },
                horizontal = Dimens.GapSmall,
                vertical = Dimens.GapHairline,
                spokenLabel = stringResource(R.string.pattern_editor_phase_spoken, index + 1, label),
                valueUnit = R.plurals.cd_duration_seconds,
            )
        }
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

/**
 * Gelb gestrichelte Einfügemarke „neue Phase kommt hierhin“, eingerückt bis zum Strich der Zeilen.
 * Bleibt neben dem Text weniger als [Dimens.MinTouchTarget] für die Striche (große Schrift), steht
 * die Linie über die ganze Breite und der Text darunter, rechtsbündig.
 */
@Composable
private fun InsertMarker() {
    val text = stringResource(R.string.pattern_editor_insert_here)
    val style = MaterialTheme.typography.labelSmall
    val measurer = rememberTextMeasurer()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Dimens.GapSmall * 2 + Sizes.IconDefault, end = Dimens.GapSmall, top = Dimens.GapHairline, bottom = Dimens.GapHairline),
    ) {
        val textWidth = with(LocalDensity.current) { measurer.measure(text, style, softWrap = false, maxLines = 1).size.width.toDp() }
        val label: @Composable () -> Unit = { WholeWordText(text = text, style = style, color = WarnAmber) }
        if (maxWidth - textWidth - Dimens.GapSmall >= Dimens.MinTouchTarget) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                DashedMark(Modifier.weight(1f))
                label()
            }
        } else {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
                DashedMark(Modifier.fillMaxWidth())
                label()
            }
        }
    }
}

/** Die gelb gestrichelte Linie der Einfügemarke. */
@Composable
private fun DashedMark(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(Dimens.GapHairline)
            .drawBehind {
                val dash = Dimens.GapTiny.toPx()
                drawLine(
                    color = WarnAmber,
                    start = Offset(0f, size.height / 2),
                    end = Offset(size.width, size.height / 2),
                    strokeWidth = size.height,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(dash, dash)),
                )
            },
    )
}

/**
 * „Phase hinzufügen · nach 2 · Halten voll“ (gestrichelter Rahmen): + Ein / + Halten / + Aus in
 * den Phasenfarben, neue Phasen kommen hinter die gewählte (sonst ans Ende). Bei 12 Phasen sind
 * die Knöpfe abgeschaltet.
 */
@Composable
private fun AddPhase(state: PatternEditorState) {
    val pattern = state.pattern
    val selected = state.selected
    val colors = MaterialTheme.colorScheme
    val where = if (selected == null) {
        stringResource(R.string.pattern_editor_add_end)
    } else {
        stringResource(R.string.pattern_editor_add_after, selected + 1, pattern.labelAt(selected))
    }
    val title = stringResource(R.string.pattern_editor_add)
    val statusStyle = MaterialTheme.typography.labelMedium
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = Dimens.ListGap)
            .dashedBorder(colors.outline, MaterialTheme.shapes.medium)
            .padding(Dimens.ListGap),
        verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
    ) {
        // Passt „Titel · Ort“ in eine Zeile, steht der Ort dahinter; sonst darunter und ohne Punkt,
        // damit keine Zeile mit „·“ beginnt oder endet (LAYOUT-03).
        val titleStyle = MaterialTheme.typography.titleSmall
        val whereColor = colors.onBackground.copy(alpha = SECONDARY)
        val measurer = rememberTextMeasurer()
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val oneLine = buildAnnotatedString {
                append("$title\u00A0· ")
                withStyle(statusStyle.toSpanStyle().copy(color = whereColor)) { append(where) }
            }
            val width = measurer.measure(oneLine, titleStyle, softWrap = false, maxLines = 1).size.width
            if (width <= constraints.maxWidth) {
                Text(text = oneLine, style = titleStyle, color = colors.onBackground, maxLines = 1, softWrap = false)
            } else {
                Column {
                    WholeWordText(text = title, style = titleStyle, color = colors.onBackground)
                    WholeWordText(text = where, style = statusStyle, color = whereColor)
                }
            }
        }
        val enabled = state.canAdd
        AdaptiveButtonRow(spacing = Dimens.GapSmall) {
            AddButton(R.string.pattern_editor_add_in, R.string.cd_pattern_add_in, colors.secondary, enabled) { state.add(FreePhaseType.IN) }
            AddButton(R.string.pattern_editor_add_hold, R.string.cd_pattern_add_hold, colors.onSurfaceVariant, enabled, dashed = true) { state.add(FreePhaseType.HOLD) }
            AddButton(R.string.pattern_editor_add_out, R.string.cd_pattern_add_out, BreathExhale, enabled) { state.add(FreePhaseType.OUT) }
        }
        HintText(stringResource(R.string.pattern_editor_add_hint))
    }
}

/** Ein Knopf „+ Ein“ usw.: sichtbar kurz, gesprochen „Einatmen hinzufügen“. */
@Composable
private fun AddButton(label: Int, spoken: Int, color: Color, enabled: Boolean, dashed: Boolean = false, onClick: () -> Unit) {
    val description = stringResource(spoken)
    AppOutlinedButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description },
        enabled = enabled,
        color = color,
        borderColor = color,
        dashed = dashed,
    ) {
        Text(stringResource(label), maxLines = 1, softWrap = false)
    }
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
        SectionHeader(title = stringResource(R.string.pattern_editor_name), color = headingColor(3))
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
            accent = headingColor(4),
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
