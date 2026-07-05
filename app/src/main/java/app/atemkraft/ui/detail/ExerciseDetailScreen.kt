package app.atemkraft.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.domain.EvidenceTag
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.PhaseDuration
import app.atemkraft.domain.PhaseType
import app.atemkraft.domain.Reference
import app.atemkraft.domain.SessionConfig
import app.atemkraft.domain.defaultMinutes
import app.atemkraft.domain.isRoundBased
import app.atemkraft.ui.components.BackButton
import app.atemkraft.ui.components.Chip
import app.atemkraft.ui.components.ExpanderSection
import app.atemkraft.ui.components.TagChip
import app.atemkraft.ui.home.color
import app.atemkraft.ui.home.title
import app.atemkraft.ui.session.SafetyDialog
import app.atemkraft.ui.theme.NeonCyan
import app.atemkraft.ui.theme.WarnAmber
import kotlin.math.roundToInt

/**
 * Detail-/Infoseite einer Übung. Progressive Disclosure: oben das Wesentliche (Titel, 1-Zeiler,
 * Meta, ggf. Sicherheits-Appetizer), darunter aus-/einklappbare Abschnitte; der Starten-Button
 * ist unten **gepinnt** und ohne Scrollen erreichbar.
 */
@Composable
fun ExerciseDetailScreen(
    exercise: Exercise,
    requireSafetyConfirm: Boolean,
    onConfirmedSafety: () -> Unit,
    onStart: (SessionConfig) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler { onBack() }
    val accent = exercise.family.color()
    val roundBased = exercise.isRoundBased
    val range = if (roundBased) 1..15 else 1..30
    val isIntense = exercise.tag == EvidenceTag.CAUTION
    var value by rememberSaveable(exercise.id) {
        mutableIntStateOf(if (roundBased) exercise.rounds else exercise.defaultMinutes())
    }
    var showSafety by remember { mutableStateOf(false) }

    // Optionale Feineinstellung der Phasenlängen (dezent hinter „Intervalle anpassen").
    val cyclePhases = remember(exercise.id) { exercise.segments.flatMap { it.phases } }
    fun defaultSec(type: PhaseType): Int? =
        (cyclePhases.firstOrNull { it.type == type && it.duration is PhaseDuration.Fixed }
            ?.duration as? PhaseDuration.Fixed)?.let { (it.millis / 1000.0).roundToInt() }
    val hasInhale = cyclePhases.any { it.type == PhaseType.INHALE }
    val hasExhale = cyclePhases.any { it.type == PhaseType.EXHALE }
    val holdDefault = defaultSec(PhaseType.HOLD_FULL) ?: defaultSec(PhaseType.HOLD_EMPTY)
    val hasHold = holdDefault != null
    var inhaleSec by rememberSaveable(exercise.id) { mutableIntStateOf(defaultSec(PhaseType.INHALE) ?: 4) }
    var exhaleSec by rememberSaveable(exercise.id) { mutableIntStateOf(defaultSec(PhaseType.EXHALE) ?: 6) }
    var holdSec by rememberSaveable(exercise.id) { mutableIntStateOf(holdDefault ?: 4) }
    var intervalsExpanded by rememberSaveable(exercise.id) { mutableStateOf(false) }
    var intervalsTouched by rememberSaveable(exercise.id) { mutableStateOf(false) }

    fun currentConfig(): SessionConfig {
        val base = if (roundBased) SessionConfig(rounds = value) else SessionConfig(minutes = value)
        return if (intervalsTouched) {
            base.copy(
                inhaleSeconds = if (hasInhale) inhaleSec.toDouble() else null,
                exhaleSeconds = if (hasExhale) exhaleSec.toDouble() else null,
                holdSeconds = if (hasHold) holdSec.toDouble() else null,
            )
        } else {
            base
        }
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
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                BackButton(onClick = onBack)
                Text(
                    text = exercise.family.title(),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    exercise.tag?.let { TagChip(it) }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = exercise.shortDescription,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.85f),
                )

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Chip(
                        text = if (roundBased) stringResource(R.string.detail_meta_rounds, exercise.rounds)
                        else stringResource(R.string.detail_meta_minutes, exercise.defaultMinutes()),
                        color = accent,
                    )
                    Chip(
                        text = if (isIntense) stringResource(R.string.detail_intense) else stringResource(R.string.detail_calm),
                        color = if (isIntense) WarnAmber else NeonCyan,
                    )
                }

                if (isIntense && exercise.cautions.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "⚠ " + stringResource(R.string.detail_safety_appetizer),
                        style = MaterialTheme.typography.bodyMedium,
                        color = WarnAmber,
                    )
                }

                Spacer(Modifier.height(8.dp))
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
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    moreCautions.forEach { Bullet(it) }
                                }
                            }
                        },
                    )
                }
                if (exercise.references.isNotEmpty()) {
                    ExpanderSection(
                        title = stringResource(R.string.detail_references),
                        appetizer = stringResource(R.string.detail_sources_count, exercise.references.size),
                        accent = accent,
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            exercise.references.forEach { ReferenceItem(it) }
                        }
                    }
                }

                if (exercise.guided) {
                    Spacer(Modifier.height(16.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Stepper(
                                label = if (roundBased) stringResource(R.string.adjust_rounds)
                                else stringResource(R.string.adjust_minutes),
                                value = value,
                                range = range,
                                onChange = { value = it },
                            )
                            if (hasInhale || hasExhale || hasHold) {
                                TextButton(
                                    onClick = { intervalsExpanded = !intervalsExpanded },
                                    modifier = Modifier.padding(start = 8.dp),
                                ) {
                                    Text(
                                        text = (if (intervalsExpanded) "▴ " else "▾ ") +
                                            stringResource(R.string.adjust_intervals),
                                        style = MaterialTheme.typography.labelLarge,
                                    )
                                }
                                if (intervalsExpanded) {
                                    if (hasInhale) {
                                        Stepper(stringResource(R.string.adjust_inhale), inhaleSec, 2..12) {
                                            inhaleSec = it; intervalsTouched = true
                                        }
                                    }
                                    if (hasHold) {
                                        Stepper(stringResource(R.string.adjust_hold), holdSec, 1..20) {
                                            holdSec = it; intervalsTouched = true
                                        }
                                    }
                                    if (hasExhale) {
                                        Stepper(stringResource(R.string.adjust_exhale), exhaleSec, 2..15) {
                                            exhaleSec = it; intervalsTouched = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.detail_info_only),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                }
                Spacer(Modifier.height(24.dp))
            }

            // Gepinnter Start (immer sichtbar) – nur bei getakteten Übungen.
            if (exercise.guided) {
                Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                    ) {
                        Button(onClick = { launch() }, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.action_start))
                        }
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
private fun Stepper(label: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalIconButton(
                onClick = { onChange((value - 1).coerceIn(range)) },
                enabled = value > range.first,
                modifier = Modifier.clearAndSetSemantics { contentDescription = "$label, verringern" },
            ) {
                Text("−", style = MaterialTheme.typography.headlineSmall)
            }
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .width(48.dp)
                    .clearAndSetSemantics { contentDescription = "$label: $value" },
            )
            FilledTonalIconButton(
                onClick = { onChange((value + 1).coerceIn(range)) },
                enabled = value < range.last,
                modifier = Modifier.clearAndSetSemantics { contentDescription = "$label, erhöhen" },
            ) {
                Text("+", style = MaterialTheme.typography.headlineSmall)
            }
        }
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
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("•", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
        )
    }
}

@Composable
private fun ReferenceItem(reference: Reference) {
    Column {
        Text(
            text = reference.citation,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
        )
        reference.identifier?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            )
        }
    }
}
