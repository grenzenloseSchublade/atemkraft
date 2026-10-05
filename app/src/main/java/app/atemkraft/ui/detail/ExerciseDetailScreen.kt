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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.data.IntervalOverrides
import app.atemkraft.domain.EvidenceTag
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.adjusted
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.domain.estimatedTotalSeconds
import app.atemkraft.domain.hasOpenPhases
import app.atemkraft.domain.isRoundBased
import app.atemkraft.ui.components.BackButton
import app.atemkraft.ui.components.Chip
import app.atemkraft.ui.components.ExpanderSection
import app.atemkraft.ui.components.PhaseAdjust
import app.atemkraft.ui.components.ReferenceItem
import app.atemkraft.ui.components.SessionAdjustCard
import app.atemkraft.ui.components.StartSplitButton
import app.atemkraft.ui.components.TagChip
import app.atemkraft.ui.components.TitleWithChips
import app.atemkraft.ui.home.color
import app.atemkraft.ui.home.title
import app.atemkraft.ui.session.SafetyDialog
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.NeonCyan
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.WarnAmber
import kotlin.math.roundToInt

/**
 * Detail-/Infoseite einer Übung. Progressive Disclosure: oben das Wesentliche (Titel, 1-Zeiler,
 * Meta, ggf. Sicherheits-Appetizer), darunter aus-/einklappbare Abschnitte; der Starten-Button
 * ist unten **gepinnt** und ohne Scrollen erreichbar.
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
) {
    val accent = exercise.family.color()
    val roundBased = exercise.isRoundBased
    val range = if (roundBased) 1..15 else 1..30
    val isIntense = exercise.tag == EvidenceTag.CAUTION
    val valueDefault = if (roundBased) exercise.rounds else exercise.defaultMinutes()
    var value by rememberSaveable(exercise.id) { mutableIntStateOf(valueDefault) }
    var showSafety by remember { mutableStateOf(false) }

    // Optionale Feineinstellung der Phasenlängen (dezent hinter „Intervalle anpassen").
    val cyclePhases = remember(exercise.id) { exercise.segments.flatMap { it.phases } }
    fun defaultSec(type: PhaseType): Int? = (
        cyclePhases.firstOrNull { it.type == type && it.duration is PhaseDuration.Fixed }
            ?.duration as? PhaseDuration.Fixed
        )?.let { (it.millis / 1000.0).roundToInt() }
    val hasInhale = cyclePhases.any { it.type == PhaseType.INHALE }
    val hasExhale = cyclePhases.any { it.type == PhaseType.EXHALE }
    val inhaleDefault = defaultSec(PhaseType.INHALE) ?: 4
    val exhaleDefault = defaultSec(PhaseType.EXHALE) ?: 6
    val holdDefault = defaultSec(PhaseType.HOLD_FULL) ?: defaultSec(PhaseType.HOLD_EMPTY) ?: 4
    val hasHold = defaultSec(PhaseType.HOLD_FULL) != null || defaultSec(PhaseType.HOLD_EMPTY) != null
    var inhaleSec by rememberSaveable(exercise.id) { mutableIntStateOf(inhaleDefault) }
    var exhaleSec by rememberSaveable(exercise.id) { mutableIntStateOf(exhaleDefault) }
    var holdSec by rememberSaveable(exercise.id) { mutableIntStateOf(holdDefault) }
    var intervalsExpanded by rememberSaveable(exercise.id) { mutableStateOf(false) }

    // „Angepasst" ist kein Merker, sondern der Wertvergleich mit den Übungs-Defaults: wer
    // manuell auf die Defaults zurücksteppt, gilt wieder als unangepasst (Override wird gelöscht).
    fun intervalsModifiedNow(): Boolean = (hasInhale && inhaleSec != inhaleDefault) ||
        (hasHold && holdSec != holdDefault) ||
        (hasExhale && exhaleSec != exhaleDefault)

    fun modifiedNow(): Boolean = value != valueDefault || intervalsModifiedNow()

    fun persistAdjustments() {
        if (modifiedNow()) {
            onIntervalsChange(
                value,
                inhaleSec.takeIf { hasInhale },
                holdSec.takeIf { hasHold },
                exhaleSec.takeIf { hasExhale },
            )
        } else {
            onIntervalsReset()
        }
    }

    // Persistierte Anpassung einmalig übernehmen (kommt asynchron aus dem DataStore); danach
    // hat der lokale State Vorrang, damit Write-throughs nicht zurückschwappen.
    var storedApplied by rememberSaveable(exercise.id) { mutableStateOf(false) }
    LaunchedEffect(savedIntervals) {
        val stored = savedIntervals ?: return@LaunchedEffect
        if (storedApplied || modifiedNow()) {
            storedApplied = true
            return@LaunchedEffect
        }
        storedApplied = true
        stored.duration?.let { value = it.coerceIn(range) }
        if (hasInhale) stored.inhale?.let { inhaleSec = it }
        if (hasHold) stored.hold?.let { holdSec = it }
        if (hasExhale) stored.exhale?.let { exhaleSec = it }
    }

    fun currentConfig(): SessionConfig {
        val base = if (roundBased) SessionConfig(rounds = value) else SessionConfig(minutes = value)
        return if (intervalsModifiedNow()) {
            base.copy(
                inhaleSeconds = if (hasInhale) inhaleSec.toDouble() else null,
                exhaleSeconds = if (hasExhale) exhaleSec.toDouble() else null,
                holdSeconds = if (hasHold) holdSec.toDouble() else null,
            )
        } else {
            base
        }
    }

    // Grobe Gesamtdauer der aktuell eingestellten Session (Runden/Minuten + Intervalle) –
    // live neu berechnet, wenn sich einer der Anpassungswerte ändert.
    val estimatedSeconds = remember(value, inhaleSec, exhaleSec, holdSec) {
        exercise.adjusted(currentConfig()).estimatedTotalSeconds()
    }

    fun launch() {
        if (requireSafetyConfirm) showSafety = true else onStart(currentConfig())
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding),
            ) {
                Spacer(Modifier.height(Dimens.ScreenTopSub))
                BackButton(onClick = onBack)
                Text(
                    text = exercise.family.title(),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
                Spacer(Modifier.height(Dimens.GapHairline))
                TitleWithChips(
                    title = exercise.name,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    chips = exercise.tag?.let { tag -> { TagChip(tag) } },
                )
                Spacer(Modifier.height(Dimens.GapSmall))
                Text(
                    text = exercise.shortDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                )

                Spacer(Modifier.height(Dimens.GapSmall))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                    verticalArrangement = Arrangement.spacedBy(Dimens.GapSmall),
                ) {
                    Chip(
                        text = if (roundBased) {
                            pluralStringResource(R.plurals.detail_meta_rounds, exercise.rounds, exercise.rounds)
                        } else {
                            stringResource(R.string.detail_meta_minutes, exercise.defaultMinutes())
                        },
                        color = accent,
                    )
                    Chip(
                        text = if (isIntense) stringResource(R.string.detail_intense) else stringResource(R.string.detail_calm),
                        color = if (isIntense) WarnAmber else NeonCyan,
                    )
                }

                if (isIntense && exercise.cautions.isNotEmpty()) {
                    Spacer(Modifier.height(Dimens.GapSmall))
                    Text(
                        text = "⚠ " + stringResource(R.string.detail_safety_appetizer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarnAmber,
                    )
                }

                Spacer(Modifier.height(Dimens.GapSmall))
                exercise.instructionHint?.let { hint ->
                    ExpanderSection(stringResource(R.string.detail_instruction), hint, accent)
                }
                ExpanderSection(
                    title = stringResource(R.string.detail_effect),
                    appetizer = exercise.effect,
                    accent = accent,
                    content = exercise.effectDetail?.let { detail -> { Body(detail) } },
                )
                if (exercise.cautions.isNotEmpty()) {
                    val moreCautions = exercise.cautions.drop(1)
                    ExpanderSection(
                        title = stringResource(R.string.detail_cautions),
                        appetizer = exercise.cautions.first(),
                        accent = if (isIntense) WarnAmber else accent,
                        initiallyExpanded = isIntense,
                        content = if (moreCautions.isEmpty()) {
                            null
                        } else {
                            {
                                Column(verticalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
                                    moreCautions.forEach { Bullet(it) }
                                }
                            }
                        },
                    )
                }
                if (exercise.references.isNotEmpty()) {
                    ExpanderSection(
                        title = stringResource(R.string.detail_references),
                        appetizer = pluralStringResource(R.plurals.detail_sources_count, exercise.references.size, exercise.references.size),
                        accent = accent,
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
                        duration = value,
                        durationDefault = valueDefault,
                        durationRange = range,
                        onDuration = {
                            value = it
                            persistAdjustments()
                        },
                        inhale = if (hasInhale) {
                            PhaseAdjust(inhaleSec, inhaleDefault, 2..12) {
                                inhaleSec = it
                                persistAdjustments()
                            }
                        } else {
                            null
                        },
                        hold = if (hasHold) {
                            PhaseAdjust(holdSec, holdDefault, 1..20) {
                                holdSec = it
                                persistAdjustments()
                            }
                        } else {
                            null
                        },
                        exhale = if (hasExhale) {
                            PhaseAdjust(exhaleSec, exhaleDefault, 2..15) {
                                exhaleSec = it
                                persistAdjustments()
                            }
                        } else {
                            null
                        },
                        intervalsExpanded = intervalsExpanded,
                        onToggleIntervals = { intervalsExpanded = !intervalsExpanded },
                    )
                } else {
                    Spacer(Modifier.height(Dimens.SectionGap))
                    Text(
                        text = stringResource(R.string.detail_info_only),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                    )
                }
                Spacer(Modifier.height(Dimens.ScreenBottom))
            }

            // Gepinnter Start (immer sichtbar) – nur bei getakteten Übungen.
            if (exercise.guided) {
                Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.CardPadding),
                    ) {
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
                            resetVisible = modifiedNow(),
                            onStart = { launch() },
                            onReset = {
                                value = valueDefault
                                inhaleSec = inhaleDefault
                                exhaleSec = exhaleDefault
                                holdSec = holdDefault
                                onIntervalsReset()
                            },
                        )
                    }
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
                onStart(currentConfig())
            },
            onDismiss = { showSafety = false },
        )
    }
}

@Composable
private fun Body(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
    )
}

@Composable
private fun Bullet(text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.GapTiny)) {
        Text("•", color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
    }
}
