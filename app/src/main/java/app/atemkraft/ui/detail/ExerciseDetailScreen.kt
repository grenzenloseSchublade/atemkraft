package app.atemkraft.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.data.Atemlehre
import app.atemkraft.data.IntervalOverrides
import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.ExerciseIds
import app.atemkraft.domain.GeneratedExercises
import app.atemkraft.domain.PatternTexts
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.adjusted
import app.atemkraft.domain.breathRhythm
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.domain.estimatedTotalSeconds
import app.atemkraft.domain.hasOpenPhases
import app.atemkraft.domain.isRoundBased
import app.atemkraft.domain.minutesRounded
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.components.AppTextButton
import app.atemkraft.ui.components.BodyText
import app.atemkraft.ui.components.Bullet
import app.atemkraft.ui.components.CautionChip
import app.atemkraft.ui.components.CharacterChip
import app.atemkraft.ui.components.Chip
import app.atemkraft.ui.components.EvidenceChip
import app.atemkraft.ui.components.ExpanderSection
import app.atemkraft.ui.components.ForwardLinkText
import app.atemkraft.ui.components.HintText
import app.atemkraft.ui.components.PersonalMarkToggle
import app.atemkraft.ui.components.PhaseAdjust
import app.atemkraft.ui.components.PinnedActionBar
import app.atemkraft.ui.components.PushScaffold
import app.atemkraft.ui.components.ReferenceItem
import app.atemkraft.ui.components.RhythmCurve
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SessionAdjustCard
import app.atemkraft.ui.components.StartSplitButton
import app.atemkraft.ui.components.WarnText
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.color
import app.atemkraft.ui.components.flushEnd
import app.atemkraft.ui.components.rememberSafetyGate
import app.atemkraft.ui.components.title
import app.atemkraft.ui.theme.BODY
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.TERTIARY
import kotlin.math.roundToInt

/**
 * Was die Detailseite auslöst (CODE-02). [onIntervalsChange]/[onIntervalsReset] merken bzw. löschen
 * die Anpassung, [onConfirmedSafety] die Bestätigung des Sicherheitsdialogs.
 * - [onOpenLore]: Sprung zur Muster-Karte der Atemlehre aus „Aus der Yoga-Lehre“; ohne ihn kein Verweis.
 * - [onMarkedChange]: persönliche Markierung „tut mir gut“; ohne ihn (Muster) kein Schalter.
 * - [onEditPattern]: Stift im Kopf generierter Muster, öffnet den Muster-Editor (Phasen ändert man
 *   nur dort); ohne ihn kein Stift.
 */
@Immutable
data class DetailActions(
    val onIntervalsChange: (duration: Int, inhale: Int?, hold: Int?, exhale: Int?) -> Unit,
    val onIntervalsReset: () -> Unit,
    val onConfirmedSafety: () -> Unit,
    val onStart: (SessionConfig) -> Unit,
    val onBack: () -> Unit,
    val onOpenLore: ((patternId: String) -> Unit)? = null,
    val onMarkedChange: ((Boolean) -> Unit)? = null,
    val onEditPattern: (() -> Unit)? = null,
)

/**
 * Detail-/Infoseite einer Übung. Progressive Disclosure: oben das Wesentliche (Name fest im Kopf,
 * Familie mit Chips, 1-Zeiler, Meta, ggf. Sicherheits-Appetizer), darunter aus-/einklappbare
 * Abschnitte; der Starten-Button ist unten **gepinnt** und ohne Scrollen erreichbar, darüber die
 * Zeile „Anpassen ›“, die Dauer und Phasen im Blatt [AdjustSheet] öffnet (`docs/spec/detail.md`).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(
    exercise: Exercise,
    requireSafetyConfirm: Boolean,
    savedIntervals: IntervalOverrides?,
    actions: DetailActions,
    marked: Boolean = false,
    // Wortlaut des Stifts für TalkBack; null = nach Herkunft („Als eigenes Muster anpassen“ beim
    // Muster des Tages, sonst „Muster bearbeiten“).
    editDescription: String? = null,
    // Dauer für diesen Einstieg (Willkommenskarte), nicht gespeichert; null = Voreinstellung.
    initialMinutes: Int? = null,
    // Eine Session oder Meditation liegt im Vollbild darüber: Der Rhythmus-Punkt steht still.
    covered: Boolean = false,
) {
    val accent = exercise.family.color()
    val roundBased = exercise.isRoundBased
    val isIntense = exercise.caution
    // Abschnittsköpfe weiß (FARBE-07); die Warnung tragen ⚠-Zeile und Chip. Die Familienfarbe
    // bleibt beim Inhalt (Familie, Dauer-Chip); der Lehre-Verweis steht wie jeder Verweis in
    // secondary (FARBE-04).
    // Anpassung (Dauer, optional Phasenlängen hinter „Phasen anpassen“) liegt im SessionAdjustState –
    // die Seite liest und ändert nur ihn (ARC-7).
    val adjust = rememberSessionAdjustState(exercise, initialMinutes)
    val defaults = adjust.defaults
    var intervalsExpanded by rememberSaveable(exercise.id) { mutableStateOf(false) }
    // Offen überlebt das Blatt „Anpassen“ Drehen; die Seite darunter bleibt, wie sie ist (MUSTER-11).
    var adjustOpen by rememberSaveable(exercise.id) { mutableStateOf(false) }

    fun persistAdjustments() {
        val overrides = adjust.overrides
        if (overrides != null) {
            actions.onIntervalsChange(overrides.duration ?: defaults.duration, overrides.inhale, overrides.hold, overrides.exhale)
        } else {
            actions.onIntervalsReset()
        }
    }

    // Gespeicherte und eigene Muster ändern ihre Phasen nur im Editor (MUSTER-10); das Muster des
    // Tages behält seine Phasen-Stepper wie in v1.6 (nicht gespeichert, siehe persistIntervals).
    val phasesInEditor = exercise.generated && exercise.id != ExerciseIds.DAILY

    // Persistierte Anpassung einmalig übernehmen (kommt asynchron aus dem DataStore). Bei
    // gespeicherten und eigenen Mustern gilt von alten Anpassungen nur noch die Dauer.
    // Schlüssel auch [adjust]: Ein neu begonnener Zustand (Muster im Editor geändert) prüft die
    // gespeicherte Anpassung erneut.
    LaunchedEffect(savedIntervals, adjust) {
        savedIntervals?.let { stored ->
            adjust.applyStored(if (phasesInEditor) IntervalOverrides(duration = stored.duration) else stored)
        }
    }

    // Grobe Gesamtdauer der aktuell eingestellten Session (Runden/Minuten + Intervalle) –
    // live neu berechnet, wenn sich einer der Anpassungswerte ändert.
    val config = adjust.config()
    val adjusted = remember(exercise, config) { exercise.adjusted(config) }
    val estimatedSeconds = remember(adjusted) { adjusted.estimatedTotalSeconds() }
    // Ein Atemzug als Kurve, mit den aktuellen Anpassungen; null, wo er nichts Wahres zeigte.
    val rhythm = remember(adjusted) { adjusted.breathRhythm() }
    // Muster des Tages: dieselbe Musterzeile wie auf seiner Karte.
    val rhythmLine = remember(adjusted) {
        if (exercise.id == ExerciseIds.DAILY) GeneratedExercises.specOf(adjusted)?.let(PatternTexts::cardLineFor) else null
    }

    // Sicherheitsdialog vor intensiven Übungen: dieselbe Abfolge wie im Muster-Editor; der offene
    // Dialog überlebt Rotation und Prozess-Neustart (CODE-03).
    val launch = rememberSafetyGate(
        require = requireSafetyConfirm,
        cautions = exercise.cautions,
        onConfirm = actions.onConfirmedSafety,
        proceed = { actions.onStart(adjust.config()) },
    )

    val scrollState = rememberScrollState()

    // Name und Markierung stehen fest wie der Titel jeder anderen Seite; lange Namen
    // („Nadi Shodhana (Wechselatmung)“) laufen im Kopf als Laufschrift, nie im Wort umbrochen.
    // Der Slot trägt eine Aktion: die Markierung bei Übungen, der Stift bei generierten Mustern.
    val editLabel = editDescription ?: stringResource(
        if (exercise.id == ExerciseIds.DAILY) R.string.cd_pattern_adapt_daily else R.string.cd_pattern_edit,
    )
    PushScaffold(
        onBack = actions.onBack,
        title = exercise.name,
        action = when {
            actions.onMarkedChange != null -> {
                { PersonalMarkToggle(marked = marked, onMarkedChange = actions.onMarkedChange) }
            }

            actions.onEditPattern != null && exercise.generated -> {
                { EditPatternButton(description = editLabel, onClick = actions.onEditPattern) }
            }

            else -> null
        },
        scrollState = scrollState,
        // Gepinnter Start (immer sichtbar) samt „Anpassen ›“ darüber – nur bei getakteten Übungen.
        bottomBar = if (exercise.guided) {
            {
                StartBar(
                    exercise = exercise,
                    adjust = adjust,
                    estimatedSeconds = estimatedSeconds,
                    onOpenAdjust = { adjustOpen = true },
                    onStart = { launch() },
                    onIntervalsReset = actions.onIntervalsReset,
                )
            }
        } else {
            null
        },
    ) {
        // Erste Inhaltszeile: Familie und Chips. Passen die Chips nicht neben die Familie,
        // rutschen sie gemeinsam darunter und brechen nur zwischen den Chips um.
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
            verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
        ) {
            WholeWordText(
                // Generierte Muster sind keine Übung der Familie: Sie heißen hier „Muster“, die
                // Familie bleibt nur als Farbe.
                text = if (exercise.generated) stringResource(R.string.family_pattern) else exercise.family.title(),
                style = MaterialTheme.typography.labelMedium,
                color = accent,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            if (exercise.evidence != null || exercise.caution) {
                FlowRow(
                    modifier = Modifier.align(Alignment.CenterVertically),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                    verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                ) {
                    exercise.evidence?.let { EvidenceChip(it) }
                    if (exercise.caution) CautionChip()
                }
            }
        }
        Spacer(Modifier.height(Dimens.GapSmall))
        BodyText(exercise.shortDescription, style = MaterialTheme.typography.bodyMedium)

        // Der Rhythmus gleich unter dem, was die Übung ist, und vor Dauer und Charakter – wie auf
        // der Karte (Takt · Dauer); eine weitere Zeile des Kopfblocks, daher GapSmall (§4.5).
        rhythm?.let {
            Spacer(Modifier.height(Dimens.GapSmall))
            RhythmCurve(it, line = rhythmLine, running = !covered)
        }

        Spacer(Modifier.height(Dimens.GapSmall))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
            verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
        ) {
            Chip(
                text = if (roundBased) {
                    pluralStringResource(R.plurals.detail_meta_rounds, exercise.rounds, exercise.rounds)
                } else {
                    stringResource(R.string.duration_approx, exercise.defaultMinutes())
                },
                color = accent,
            )
            CharacterChip(intense = isIntense, activating = exercise.activating)
        }

        if (isIntense && exercise.cautions.isNotEmpty()) {
            Spacer(Modifier.height(Dimens.GapSmall))
            WarnText(stringResource(R.string.detail_safety_appetizer))
        }

        Spacer(Modifier.height(Dimens.GapSmall))
        exercise.instructionHint?.let { hint ->
            ExpanderSection(stringResource(R.string.detail_instruction), hint)
        }
        ExpanderSection(
            title = stringResource(R.string.detail_effect),
            appetizer = exercise.effect,
            content = exercise.effectDetail?.let { detail -> { BodyText(detail) } },
        )
        // Deutung der Tradition, getrennt von „Wirkung“ (keine Studienlage, TEXT-13).
        exercise.lore?.let { lore ->
            // Hauptkarte zuerst, dann weitere (z. B. die Halten-Karte eines Tagesmusters).
            val patterns = (listOf(lore.patternId) + lore.morePatternIds).mapNotNull(Atemlehre::pattern)
            ExpanderSection(
                title = stringResource(R.string.detail_lore),
                appetizer = lore.teaser,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
                    BodyText(lore.detail)
                    // Lehrquelle wie in Atemlehre und Pranayama.
                    lore.source?.let { HintText(it) }
                    if (actions.onOpenLore != null) {
                        patterns.forEach { pattern ->
                            // Der Chevron zeigt „führt weiter“ (ICON-01). Nicht bündig (flushStart): Die
                            // Höhenanimation des Expanders schneidet alles außerhalb ab.
                            AppTextButton(onClick = { actions.onOpenLore(pattern.id) }) {
                                ForwardLinkText(
                                    text = stringResource(R.string.detail_lore_link, pattern.linkName),
                                    // Verweise im Inhalt einheitlich in secondary (FARBE-04).
                                    color = MaterialTheme.colorScheme.secondary,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (exercise.cautions.isNotEmpty()) {
            val moreCautions = exercise.cautions.drop(1)
            ExpanderSection(
                title = stringResource(R.string.detail_cautions),
                appetizer = exercise.cautions.first(),
                initiallyExpanded = isIntense,
                content = if (moreCautions.isEmpty()) {
                    null
                } else {
                    {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
                            moreCautions.forEach { Bullet(it, color = LocalContentColor.current.copy(alpha = BODY)) }
                        }
                    }
                },
            )
        }
        if (exercise.references.isNotEmpty()) {
            ExpanderSection(
                title = stringResource(R.string.detail_references),
                appetizer = pluralStringResource(R.plurals.detail_sources_count, exercise.references.size, exercise.references.size),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                    exercise.references.forEach { ReferenceItem(it) }
                }
            }
        }

        // Getaktete Übungen passt man im Blatt „Anpassen“ an (Zeile über dem Start); nur Programme
        // ohne Ablauf sagen am Ende, warum es hier keinen Start gibt.
        if (!exercise.guided) {
            Spacer(Modifier.height(Dimens.SectionGap))
            HintText(stringResource(R.string.detail_info_only))
        }
    }

    // Ein Ort für die Anpassung: das Blatt über der Seite, geöffnet aus der Start-Leiste. Werte
    // gelten sofort und werden gespeichert wie zuvor in der Karte am Ende des Inhalts.
    if (exercise.guided && adjustOpen) {
        AdjustSheet(onDismiss = { adjustOpen = false }) {
            SessionAdjustCard(
                durationLabel = if (roundBased) {
                    stringResource(R.string.adjust_rounds)
                } else {
                    stringResource(R.string.adjust_minutes)
                },
                duration = adjust.duration,
                durationDefault = defaults.duration,
                durationRange = defaults.durationRange,
                onDuration = {
                    adjust.duration = it
                    persistAdjustments()
                },
                // Gespeicherte und eigene Muster: nur die Dauer – Phasen ändert der Stift
                // (Editor), sonst überstimmten Anpassungen die gespeicherte Folge.
                inhale = defaults.inhale?.takeUnless { phasesInEditor }?.let { default ->
                    PhaseAdjust(adjust.inhale ?: default, default, AdjustLimits.INHALE_S) {
                        adjust.inhale = it
                        persistAdjustments()
                    }
                },
                hold = defaults.hold?.takeUnless { phasesInEditor }?.let { default ->
                    PhaseAdjust(adjust.hold ?: default, default, AdjustLimits.HOLD_S) {
                        adjust.hold = it
                        persistAdjustments()
                    }
                },
                exhale = defaults.exhale?.takeUnless { phasesInEditor }?.let { default ->
                    PhaseAdjust(adjust.exhale ?: default, default, AdjustLimits.EXHALE_S) {
                        adjust.exhale = it
                        persistAdjustments()
                    }
                },
                intervalsExpanded = intervalsExpanded,
                onToggleIntervals = { intervalsExpanded = !intervalsExpanded },
                // Runden sagt TalkBack als „Runden: 4“, Minuten ausgeschrieben.
                durationSpokenLabel = if (roundBased) stringResource(R.string.adjust_rounds) else stringResource(R.string.cd_adjust_duration),
                durationUnit = if (roundBased) null else R.plurals.cd_duration_minutes,
            )
        }
    }
}

/**
 * Gepinnter Start unter dem Inhalt: oben die Zeile „Anpassen ›“ ([AdjustRow], öffnet das
 * [AdjustSheet]), darunter der [StartSplitButton] mit der Gesamtdauer am Start („Starten ·
 * ca. 10 min“); das Zurücksetzen erscheint nur nach einer Anpassung.
 */
@Composable
private fun StartBar(
    exercise: Exercise,
    adjust: SessionAdjustState,
    estimatedSeconds: Int,
    onOpenAdjust: () -> Unit,
    onStart: () -> Unit,
    onIntervalsReset: () -> Unit,
) {
    PinnedActionBar {
        Column {
            AdjustRow(exercise, adjust, onOpenAdjust)
            StartButton(exercise, adjust, estimatedSeconds, onStart, onIntervalsReset)
        }
    }
}

/**
 * „Anpassen ›“: Abschnittskopf mit `onClick` wie „Klang & Stimme“ in der Meditation (MUSTER-11) –
 * dieselbe Kopf-Art wie jeder Kopf, der ein Blatt öffnet, darum `titleLarge` auch in der Leiste.
 * Möglichst einzeilig (Eigentümer 2026-10-30): Die aktuelle Dauer bzw. Runden stehen als leiser
 * Status (`smallStatus`, `labelMedium` wie „Ablauf · 4 von 12 Phasen“) hinter dem Titel
 * („Anpassen · 10 min“, nach einer Anpassung „· 4 Runden · angepasst“), nicht als Teaser darunter.
 * Klein passt er hoch bei 360 dp und Schrift 1,0 auch angepasst dahinter; bei großer Schrift rutscht
 * er laut [SectionHeader] darunter, dann nicht höher als der frühere Teaser. TalkBack
 * liest die Zeile als eine Schaltfläche („Anpassen, 10 Minuten, angepasst“) mit „Anpassen
 * öffnen“. Keine Live-Region: Den Wert sagt der Stepper im Blatt an, das Zurücksetzen der Start-Knopf.
 */
@Composable
private fun AdjustRow(exercise: Exercise, adjust: SessionAdjustState, onClick: () -> Unit) {
    val rounds = pluralStringResource(R.plurals.detail_meta_rounds, adjust.duration, adjust.duration)
    // Gezeigt kurz („10 min“), gesprochen ausgeschrieben („10 Minuten“, A11Y-06).
    val value = if (exercise.isRoundBased) rounds else stringResource(R.string.adjust_status_minutes, adjust.duration)
    val spokenValue = if (exercise.isRoundBased) rounds else pluralStringResource(R.plurals.cd_duration_minutes, adjust.duration, adjust.duration)
    val modified = stringResource(R.string.adjust_modified)
    SectionHeader(
        title = stringResource(R.string.adjust_title),
        status = stringResource(
            R.string.adjust_status,
            if (adjust.modified) stringResource(R.string.adjust_status_modified, value, modified) else value,
        ),
        statusDescription = if (adjust.modified) stringResource(R.string.cd_adjust_status_modified, spokenValue, modified) else spokenValue,
        smallStatus = true,
        onClick = onClick,
        onClickLabel = stringResource(R.string.cd_adjust_open),
    )
}

/** Start mit Gesamtdauer und Reset ([StartSplitButton]). */
@Composable
private fun StartButton(
    exercise: Exercise,
    adjust: SessionAdjustState,
    estimatedSeconds: Int,
    onStart: () -> Unit,
    onIntervalsReset: () -> Unit,
) {
    val start = stringResource(R.string.action_start)
    val startLabel = when {
        estimatedSeconds < 60 && !exercise.hasOpenPhases -> start

        exercise.hasOpenPhases -> stringResource(
            R.string.action_start_duration,
            start,
            stringResource(R.string.duration_approx_from, (estimatedSeconds / 60).coerceAtLeast(1)),
        )

        else -> stringResource(
            R.string.action_start_duration,
            start,
            stringResource(R.string.duration_approx, minutesRounded(estimatedSeconds)),
        )
    }
    StartSplitButton(
        label = startLabel,
        resetVisible = adjust.modified,
        onStart = onStart,
        onReset = {
            adjust.reset()
            onIntervalsReset()
        },
    )
}

/** Stift im Kopf generierter Muster: öffnet den Muster-Editor, Name in [description] (ICON-02). */
@Composable
private fun EditPatternButton(description: String, onClick: () -> Unit) {
    AppIconButton(onClick = onClick) {
        Icon(
            Icons.Filled.Edit,
            contentDescription = description,
            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
        )
    }
}
