package app.atemkraft.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.data.Atemlehre
import app.atemkraft.data.IntervalOverrides
import app.atemkraft.domain.AdjustLimits
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.RandomPatternGenerator
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.adjusted
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.domain.estimatedTotalSeconds
import app.atemkraft.domain.hasOpenPhases
import app.atemkraft.domain.isRoundBased
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
import app.atemkraft.ui.components.PushHeader
import app.atemkraft.ui.components.ReferenceItem
import app.atemkraft.ui.components.SessionAdjustCard
import app.atemkraft.ui.components.StartSplitButton
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.components.color
import app.atemkraft.ui.components.headingColor
import app.atemkraft.ui.components.title
import app.atemkraft.ui.theme.BODY
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.TERTIARY
import app.atemkraft.ui.theme.WarnAmber
import kotlin.math.roundToInt

/**
 * Detail-/Infoseite einer Übung. Progressive Disclosure: oben das Wesentliche (Name fest im Kopf,
 * Familie mit Chips, 1-Zeiler, Meta, ggf. Sicherheits-Appetizer), darunter aus-/einklappbare
 * Abschnitte; der Starten-Button ist unten **gepinnt** und ohne Scrollen erreichbar.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseDetailScreen(
    exercise: Exercise,
    requireSafetyConfirm: Boolean,
    savedIntervals: IntervalOverrides?,
    onIntervalsChange: (duration: Int, inhale: Int?, hold: Int?, exhale: Int?) -> Unit,
    onIntervalsReset: () -> Unit,
    onConfirmedSafety: () -> Unit,
    onStart: (SessionConfig) -> Unit,
    onBack: () -> Unit,
    // Sprung zur Muster-Karte der Atemlehre aus „Aus der Yoga-Lehre“; ohne ihn kein Verweis.
    onOpenLore: ((patternId: String) -> Unit)? = null,
    // Persönliche Markierung „tut mir gut“; ohne [onMarkedChange] (Muster) kein Schalter.
    marked: Boolean = false,
    onMarkedChange: ((Boolean) -> Unit)? = null,
    // Stift im Kopf generierter Muster: öffnet den Muster-Editor (Phasen ändert man nur dort).
    // [editDescription] = Wortlaut für TalkBack; null = nach Herkunft („Als eigenes Muster
    // anpassen“ beim Muster des Tages, sonst „Muster bearbeiten“).
    onEditPattern: (() -> Unit)? = null,
    editDescription: String? = null,
) {
    val accent = exercise.family.color()
    val roundBased = exercise.isRoundBased
    val isIntense = exercise.caution
    // Abschnittsköpfe reihum nach ihrer Lage, ohne Bedeutung (FARBE-07): Index = Zahl der gezeigten
    // Köpfe darüber; fehlt ein Abschnitt (Anleitung, Lehre, Hinweise, Quellen), rückt der nächste
    // nach. Auch „Hinweise & Sicherheit“ geht reihum – die Warnung tragen ⚠-Zeile und Chip. Die
    // Familienfarbe bleibt beim Inhalt (Familie, Dauer-Chip, Lehre-Verweis).
    val effectHead = if (exercise.instructionHint != null) 1 else 0
    val loreHead = effectHead + 1
    val cautionsHead = loreHead + (if (exercise.lore != null) 1 else 0)
    val referencesHead = cautionsHead + (if (exercise.cautions.isNotEmpty()) 1 else 0)
    // Anpassung (Dauer, optional Phasenlängen hinter „Phasen anpassen“) liegt im SessionAdjustState –
    // die Seite liest und ändert nur ihn (ARC-7).
    val adjust = rememberSessionAdjustState(exercise)
    val defaults = adjust.defaults
    // Offener Dialog überlebt Rotation und Prozess-Neustart (CODE-03).
    var showSafety by rememberSaveable { mutableStateOf(false) }
    var intervalsExpanded by rememberSaveable(exercise.id) { mutableStateOf(false) }

    fun persistAdjustments() {
        val overrides = adjust.overrides
        if (overrides != null) {
            onIntervalsChange(overrides.duration ?: defaults.duration, overrides.inhale, overrides.hold, overrides.exhale)
        } else {
            onIntervalsReset()
        }
    }

    // Gespeicherte und eigene Muster ändern ihre Phasen nur im Editor (MUSTER-10); das Muster des
    // Tages behält seine Phasen-Stepper wie in v1.6 (nicht gespeichert, siehe persistIntervals).
    val phasesInEditor = exercise.generated && exercise.id != RandomPatternGenerator.ID

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
    val estimatedSeconds = remember(config) { exercise.adjusted(config).estimatedTotalSeconds() }

    fun launch() {
        if (requireSafetyConfirm) showSafety = true else onStart(adjust.config())
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Name und Markierung stehen fest wie der Titel jeder anderen Seite; lange Namen
            // („Nadi Shodhana (Wechselatmung)“) werden im Kopf zweizeilig, nie im Wort.
            // Der Slot trägt eine Aktion: die Markierung bei Übungen, der Stift bei generierten Mustern.
            val editLabel = editDescription ?: stringResource(
                if (exercise.id == RandomPatternGenerator.ID) R.string.cd_pattern_adapt_daily else R.string.cd_pattern_edit,
            )
            PushHeader(
                onBack = onBack,
                title = exercise.name,
                action = when {
                    onMarkedChange != null -> {
                        { PersonalMarkToggle(marked = marked, onMarkedChange = onMarkedChange) }
                    }

                    onEditPattern != null && exercise.generated -> {
                        { EditPatternButton(description = editLabel, onClick = onEditPattern) }
                    }

                    else -> null
                },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding),
            ) {
                Spacer(Modifier.height(Dimens.SectionGap))
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
                    Text(
                        text = "⚠ " + stringResource(R.string.detail_safety_appetizer),
                        style = MaterialTheme.typography.bodySmall,
                        color = WarnAmber,
                    )
                }

                Spacer(Modifier.height(Dimens.GapSmall))
                exercise.instructionHint?.let { hint ->
                    ExpanderSection(stringResource(R.string.detail_instruction), hint, headingColor(0))
                }
                ExpanderSection(
                    title = stringResource(R.string.detail_effect),
                    appetizer = exercise.effect,
                    accent = headingColor(effectHead),
                    content = exercise.effectDetail?.let { detail -> { BodyText(detail) } },
                )
                // Deutung der Tradition, getrennt von „Wirkung“ (keine Studienlage, TEXT-13).
                exercise.lore?.let { lore ->
                    // Hauptkarte zuerst, dann weitere (z. B. die Halten-Karte eines Tagesmusters).
                    val patterns = (listOf(lore.patternId) + lore.morePatternIds).mapNotNull(Atemlehre::pattern)
                    ExpanderSection(
                        title = stringResource(R.string.detail_lore),
                        appetizer = lore.teaser,
                        accent = headingColor(loreHead),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
                            BodyText(lore.detail)
                            lore.source?.let { source ->
                                Text(
                                    text = source,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = TERTIARY),
                                )
                            }
                            if (onOpenLore != null) {
                                patterns.forEach { pattern ->
                                    // Der Chevron zeigt „führt weiter“ (ICON-01). Nicht bündig (flushStart): Die
                                    // Höhenanimation des Expanders schneidet alles außerhalb ab.
                                    AppTextButton(onClick = { onOpenLore(pattern.id) }) {
                                        ForwardLinkText(
                                            text = stringResource(R.string.detail_lore_link, pattern.linkName),
                                            color = accent,
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
                        accent = headingColor(cautionsHead),
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
                        accent = headingColor(referencesHead),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall)) {
                            exercise.references.forEach { ReferenceItem(it) }
                        }
                    }
                }

                if (exercise.guided) {
                    Spacer(Modifier.height(Dimens.SectionGap))
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
                } else {
                    Spacer(Modifier.height(Dimens.SectionGap))
                    HintText(stringResource(R.string.detail_info_only))
                }
                Spacer(Modifier.height(Dimens.ScreenBottom))
            }

            // Gepinnter Start (immer sichtbar) – nur bei getakteten Übungen.
            if (exercise.guided) {
                PinnedActionBar {
                    // Gesamtdauer direkt am Start: „Starten · ca. 10 min".
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
                            stringResource(R.string.duration_approx, ((estimatedSeconds + 30) / 60).coerceAtLeast(1)),
                        )
                    }
                    StartSplitButton(
                        label = startLabel,
                        resetVisible = adjust.modified,
                        onStart = { launch() },
                        onReset = {
                            adjust.reset()
                            onIntervalsReset()
                        },
                    )
                }
            }
        }
    }

    if (showSafety) {
        SafetyDialog(
            cautions = exercise.cautions,
            onConfirm = {
                showSafety = false
                onConfirmedSafety()
                onStart(adjust.config())
            },
            onDismiss = { showSafety = false },
        )
    }
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
