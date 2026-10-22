package app.atemkraft.ui.patterns

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import app.atemkraft.R
import app.atemkraft.data.Atemlehre
import app.atemkraft.data.PatternBackup
import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PatternField
import app.atemkraft.domain.PatternWish
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.adjusted
import app.atemkraft.domain.estimatedTotalSeconds
import app.atemkraft.domain.secondsOf
import app.atemkraft.domain.toFreePattern
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppIconToggle
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.BreathCurveEditor
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
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.choiceGroupHeading
import app.atemkraft.ui.components.color
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.components.rememberArmedConfirm
import app.atemkraft.ui.theme.AtemkraftTheme
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/**
 * Muster-Editor „Eigenes Muster“ (MUSTER-10), Push-Seite. Von oben: Kopf mit Lesezeichen,
 * Wünsche (Charakter, Halten, Dauer), Atemzug als Kurve mit Griffen, darunter „Genau einstellen“
 * mit einem Stepper je Phase, Regel und Musterzeile, Name, „Aus der Yoga-Lehre“; unten fest
 * „Starten · ca. N min“ mit Reset.
 *
 * Die Seite liest und ändert nur [state] (ARC-7). Was gespeichert wird, entscheidet der Aufrufer:
 * [onSavedChange] (true = speichern bzw. denselben Eintrag aktualisieren, false = aus „Meine
 * Muster“ entfernen) schreibt ins Repository und meldet das Ergebnis mit
 * [PatternEditorState.markSaved] bzw. [PatternEditorState.markRemoved] zurück. Ein Muster, das es
 * schon vor dem Öffnen gab, entfernt das Lesezeichen nur zweistufig (MUSTER-05); ein in dieser
 * Sitzung angelegtes nimmt ein erneuter Tipp sofort wieder heraus (KOMP-02).
 *
 * [onStart] probiert den Entwurf ohne Speichern aus (Übung mit Herkunft CUSTOM und die Dauer als
 * [SessionConfig]); die Seite bleibt dabei offen. [onOpenLore] öffnet eine Karte der Atemlehre.
 */
@Composable
fun PatternEditorScreen(
    state: PatternEditorState,
    onSavedChange: (Boolean) -> Unit,
    onStart: (Exercise, SessionConfig) -> Unit,
    onOpenLore: (patternId: String) -> Unit,
    onBack: () -> Unit,
) {
    val exercise = state.exercise()
    val config = state.config()
    val estimatedSeconds = remember(exercise, config) { exercise.adjusted(config).estimatedTotalSeconds() }
    val approx = stringResource(R.string.duration_approx, ((estimatedSeconds + HALF_MINUTE) / SECONDS_PER_MINUTE).coerceAtLeast(1))

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
                Wishes(state)
                Breath(state)
                FineTuning(state)
                NameSection(state)
                Lore(exercise, onOpenLore)
                Spacer(Modifier.height(Dimens.ScreenBottom))
            }
            PinnedActionBar {
                StartSplitButton(
                    label = stringResource(R.string.action_start_duration, stringResource(R.string.action_start), approx),
                    resetVisible = state.modified,
                    onStart = { onStart(exercise, config) },
                    onReset = state::reset,
                )
            }
        }
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
    val draft = PatternSnapshot(state.spec, state.minutes, state.savedName)
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

/** Wünsche: Charakter und Halten setzen sofort den typischen Vorschlag, die Dauer nie die Phasen. */
@Composable
private fun Wishes(state: PatternEditorState) {
    val wish = state.wish
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(title = stringResource(R.string.pattern_editor_wishes), color = headingColor(0)) {
            AppIconButton(onClick = state::nextSuggestion) {
                Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.cd_pattern_suggest), tint = headingColor(0))
            }
        }
        val character = stringResource(R.string.pattern_editor_character)
        SubLabel(character, Modifier.choiceGroupHeading(), color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Dimens.GapSmall))
        SegmentedChoiceRow(listOf(false, true), wish.activating, state::chooseCharacter, groupLabel = character) { activating ->
            stringResource(if (activating) R.string.daily_pattern_gentle_up else R.string.detail_calm)
        }
        Spacer(Modifier.height(Dimens.SectionGap))
        val hold = stringResource(R.string.pattern_editor_hold)
        SubLabel(hold, Modifier.choiceGroupHeading(), color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Dimens.GapSmall))
        SegmentedChoiceRow(listOf(false, true), wish.withHold, state::chooseHold, groupLabel = hold) { withHold ->
            stringResource(if (withHold) R.string.pattern_editor_hold_with else R.string.pattern_editor_hold_without)
        }
        Spacer(Modifier.height(Dimens.SectionGap))
        Card(modifier = Modifier.fillMaxWidth()) {
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
}

/** Atemzug als Kurve (Hauptbedienung); die Länge des Atemzugs steht als Status am Kopf. */
@Composable
private fun Breath(state: PatternEditorState) {
    val cycle = PatternField.entries.sumOf { state.secondsOf(it) }
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.pattern_editor_breath),
            color = headingColor(1),
            status = stringResource(R.string.pattern_editor_cycle, cycle),
            statusDescription = pluralStringResource(R.plurals.cd_pattern_editor_cycle, cycle, cycle),
        )
        // Die Kurve bringt ihre eigene Fläche mit (surface, shapes.medium) – keine Karte drumherum.
        // Übergang bis zum Umbau auf freie Folgen: die vier Felder als Folge (Halten 0 fällt weg),
        // jede Phase der Kurve zurück auf ihr Feld; set() klemmt auf die Erzeugungsregeln.
        val spec = state.spec
        val fields = PatternField.entries.filter { it == PatternField.INHALE || it == PatternField.EXHALE || spec.secondsOf(it) > 0 }
        BreathCurveEditor(
            pattern = spec.toFreePattern(),
            selected = null,
            onSelect = {},
            onChangeSeconds = { index, seconds -> fields.getOrNull(index)?.let { state.set(it, seconds) } },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * „Genau einstellen“: je Phase ein Stepper mit den Grenzen der Erzeugungsregeln ([PatternEditorState.rangeFor]);
 * ein Feld ohne Wertebereich (Halten leer bei „sanft aktivierend“) entfällt. Halten 0 heißt ohne
 * und wird „aus“ gesprochen. Darunter die Regel des Charakters in Worten; eine eigene
 * Musterzeile gibt es nicht – die Werte stehen in den Steppern, die Dauer im Start-Knopf.
 */
@Composable
private fun FineTuning(state: PatternEditorState) {
    val modified = stringResource(R.string.adjust_modified)
    Column(Modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(R.string.pattern_editor_fine),
            color = headingColor(2),
            status = if (state.modified) "· $modified" else null,
            statusDescription = modified,
            smallStatus = true,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(vertical = Dimens.GapTiny)) {
                PatternField.entries.forEach { field ->
                    val range = state.rangeFor(field) ?: return@forEach
                    val hold = field == PatternField.HOLD_FULL || field == PatternField.HOLD_EMPTY
                    Stepper(
                        label = stringResource(field.label()),
                        value = state.secondsOf(field),
                        range = range,
                        onChange = { state.set(field, it) },
                        vertical = Dimens.GapTiny,
                        spokenLabel = stringResource(field.spokenLabel()),
                        valueUnit = R.plurals.cd_duration_seconds,
                        zeroSpoken = if (hold) stringResource(R.string.stepper_value_off) else null,
                    )
                }
            }
        }
        Spacer(Modifier.height(Dimens.GapSmall))
        HintText(stringResource(if (state.spec.activating) R.string.pattern_editor_rule_active else R.string.pattern_editor_rule_calm))
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

private fun PatternField.label(): Int = when (this) {
    PatternField.INHALE -> R.string.adjust_inhale
    PatternField.HOLD_FULL -> R.string.adjust_hold_full
    PatternField.EXHALE -> R.string.adjust_exhale
    PatternField.HOLD_EMPTY -> R.string.adjust_hold_empty
}

private fun PatternField.spokenLabel(): Int = when (this) {
    PatternField.INHALE -> R.string.phase_inhale
    PatternField.HOLD_FULL -> R.string.cd_hold_full
    PatternField.EXHALE -> R.string.phase_exhale
    PatternField.HOLD_EMPTY -> R.string.cd_hold_empty
}

private const val SECONDS_PER_MINUTE = 60
private const val HALF_MINUTE = 30

@Preview(heightDp = 1400)
@Composable
private fun PatternEditorScreenPreview() {
    AtemkraftTheme {
        val state = rememberPatternEditorState { PatternEditorState.new(it, PatternWish(activating = false, withHold = true)) }
        PatternEditorScreen(state = state, onSavedChange = {}, onStart = { _, _ -> }, onOpenLore = {}, onBack = {})
    }
}
