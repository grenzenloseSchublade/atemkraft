package app.atemkraft.ui.session

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.atemkraft.R
import app.atemkraft.domain.PhaseType
import app.atemkraft.ui.theme.SessionButtonCyan
import app.atemkraft.ui.theme.SessionButtonPink
import app.atemkraft.ui.theme.SessionNoteAmber
import app.atemkraft.ui.theme.SessionTextGlow
import app.atemkraft.ui.theme.SessionTextYellow
import kotlin.math.ceil
import kotlinx.coroutines.delay

/**
 * Zeigt die laufende Session: Start-Countdown, Atemkreis, Phasenname (+ optionale Zusatzinfo),
 * Restzeit/Verstrichen, Steuerung und – optional, dezent – die kommende Phase.
 */
@Composable
fun SessionScreen(
    state: SessionUiState,
    showNextPhase: Boolean,
    onTogglePause: () -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        if (state.status == SessionStatus.FINISHED) {
            FinishedContent(onRestart = onRestart, onExit = onStop)
        } else {
            ActiveContent(
                state = state,
                showNextPhase = showNextPhase,
                onTogglePause = onTogglePause,
                onContinue = onContinue,
                onRestart = onRestart,
                onStop = onStop,
            )
        }
    }
}

@Composable
private fun ActiveContent(
    state: SessionUiState,
    showNextPhase: Boolean,
    onTogglePause: () -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
) {
    val preparing = state.status == SessionStatus.PREPARING
    val waiting = state.status == SessionStatus.WAITING_FOR_USER

    val label = if (preparing) stringResource(R.string.session_get_ready)
    else phaseDisplayLabel(state.phaseType, state.phaseLabel)
    val fraction = if (preparing) 0.5f else breathingFraction(state)
    val secondsText = if (preparing) state.countdown.toString() else secondsText(state)
    val note = if (preparing) null else state.phaseNote
    val circleInteraction = remember { MutableInteractionSource() }

    // Tap-Flash: großes Pause/Play-Symbol direkt beim Antippen kurz aufblinken lassen.
    // Wichtig: Alpha per delay-Schleife (Snapshot-Writes) animieren, NICHT per Animatable.animateTo.
    // Grund: animateTo respektiert animator_duration_scale; ist die System-Animation aus
    // (Entwickleroptionen/Energiesparen, scale=0), springt es sofort auf den Zielwert und der Flash
    // bleibt unsichtbar. delay() läuft auf dem Dispatcher und ist davon unabhängig.
    var flashAlpha by remember { mutableFloatStateOf(0f) }
    var flashIsPause by remember { mutableStateOf(true) }
    var flashTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(flashTrigger) {
        if (flashTrigger > 0) {
            flashAlpha = 1f
            delay(220) // kurzer Halt
            val steps = 16
            for (i in 1..steps) {
                delay(34)
                flashAlpha = (1f - i.toFloat() / steps).coerceAtLeast(0f)
            }
            flashAlpha = 0f
        }
    }
    fun flashToggle() {
        flashIsPause = state.status == SessionStatus.RUNNING // läuft → wird pausiert
        flashTrigger++
        onTogglePause()
    }

    // Box statt SpaceBetween-Column: Kopfzeile, Kreis und Buttons sind fest verankert
    // (oben/Mitte/unten). Die ausgeklappte Anleitung verschiebt den Kreis dadurch NICHT –
    // bei wenig Platz legt sie sich über den Kreisrand (zIndex), statt ihn zu drücken.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (!preparing && state.roundCount > 1) {
                    stringResource(R.string.session_round, state.roundIndex + 1, state.roundCount)
                } else {
                    state.exerciseName
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            )
            // Anleitung auf Abruf: dezente „ⓘ Anleitung"-Zeile unter dem Titel, die den
            // Pattern-Hint ein-/ausklappt – jederzeit, nicht nur im Countdown.
            state.patternHint?.let { hint ->
                var hintVisible by rememberSaveable { mutableStateOf(false) }
                val hintColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f)
                Row(
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { hintVisible = !hintVisible }
                        .semantics {
                            stateDescription = if (hintVisible) "Erweitert" else "Eingeklappt"
                        }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = hintColor,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.detail_instruction),
                        style = MaterialTheme.typography.labelMedium,
                        color = hintColor,
                    )
                }
                AnimatedVisibility(
                    visible = hintVisible,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Text(
                        text = hint,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
        }

        val circleDescription = stringResource(R.string.cd_breathing_circle)
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
        Box(contentAlignment = Alignment.Center) {
        BreathingCircle(
            fraction = fraction,
            modifier = Modifier
                .fillMaxWidth(0.90f)
                .aspectRatio(1f)
                .semantics { contentDescription = circleDescription }
                .then(
                    when (state.status) {
                        // Kreis antippen (ohne Ripple-Kästchen): laufend/pausiert = Pause/Weiter,
                        // offener Hold = weiter.
                        SessionStatus.RUNNING, SessionStatus.PAUSED ->
                            Modifier.clickable(interactionSource = circleInteraction, indication = null) { flashToggle() }
                        SessionStatus.WAITING_FOR_USER ->
                            Modifier.clickable(interactionSource = circleInteraction, indication = null) { onContinue() }
                        else -> Modifier
                    },
                ),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // LiveRegion sagt Phasenname + Notiz an (der Kreis selbst hat nur eine
                // statische Beschreibung, sonst liest TalkBack den Phasennamen doppelt).
                val liveText = note?.let { "$label, $it" } ?: label
                GlowLabel(
                    text = label,
                    style = MaterialTheme.typography.headlineMedium,
                    fill = SessionTextYellow,
                    modifier = Modifier.semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = liveText
                    },
                )
                note?.let {
                    GlowLabel(
                        text = it,
                        style = MaterialTheme.typography.bodyLarge,
                        fill = SessionNoteAmber,
                    )
                }
                if (secondsText.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    GlowLabel(
                        text = secondsText,
                        style = MaterialTheme.typography.displaySmall,
                        fill = SessionTextYellow,
                    )
                }
            }
        }
            // Flash-Overlay ÜBER dem Kreis (Geschwister, nicht im Kreis-Content) → sicher sichtbar.
            PauseFlash(alpha = flashAlpha, isPause = flashIsPause)
        }
            // Dezent direkt unter dem Kreis: die kommende Phase (abschaltbar).
            // Feste Höhe reservieren, damit der Kreis NICHT springt, wenn die Zeile
            // erscheint/verschwindet oder der Text (kurz/lang) wechselt.
            if (showNextPhase) {
                Spacer(Modifier.height(14.dp))
                Box(modifier = Modifier.height(20.dp), contentAlignment = Alignment.Center) {
                    if (!preparing && state.nextPhaseType != null) {
                        val hintColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                        val hintStyle = MaterialTheme.typography.labelMedium
                        // „Als Nächstes" rechtsbündig bis zur Mitte, Phasenname linksbündig ab Mitte
                        // → der Trennpunkt liegt fest in der Bildmitte, nichts springt bei Längenwechsel.
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = stringResource(R.string.session_next),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End,
                                style = hintStyle,
                                color = hintColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(text = " · ", style = hintStyle, color = hintColor)
                            Text(
                                text = phaseDisplayLabel(state.nextPhaseType, null),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Start,
                                style = hintStyle,
                                color = hintColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (waiting) {
                Text(
                    text = stringResource(R.string.session_tap_to_continue),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(16.dp))
            }

            // Immer alle drei Buttons rendern (im Countdown deaktiviert statt abwesend) –
            // so springt das Layout beim Übergang Countdown → Übung nicht.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onTogglePause,
                    modifier = Modifier.weight(1f),
                    enabled = !preparing && !waiting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SessionButtonCyan,
                        contentColor = SessionTextGlow,
                        disabledContainerColor = SessionButtonCyan.copy(alpha = 0.25f),
                        disabledContentColor = SessionTextGlow.copy(alpha = 0.5f),
                    ),
                ) {
                    Text(
                        stringResource(
                            if (state.status == SessionStatus.PAUSED) R.string.action_resume
                            else R.string.action_pause,
                        ),
                    )
                }
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f),
                    enabled = !preparing,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = SessionButtonCyan,
                        disabledContentColor = SessionButtonCyan.copy(alpha = 0.4f),
                    ),
                    border = BorderStroke(
                        1.dp,
                        SessionButtonCyan.copy(alpha = if (preparing) 0.4f else 1f),
                    ),
                ) {
                    Text(stringResource(R.string.action_restart))
                }
                OutlinedButton(
                    onClick = onStop,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SessionButtonPink),
                    border = BorderStroke(1.dp, SessionButtonPink),
                ) {
                    Text(stringResource(R.string.action_stop))
                }
            }
        }
    }
}

@Composable
private fun FinishedContent(onRestart: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "✓ " + stringResource(R.string.session_done_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.session_done_question),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onRestart,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SessionButtonCyan,
                    contentColor = SessionTextGlow,
                ),
            ) {
                Text(stringResource(R.string.action_again))
            }
            OutlinedButton(
                onClick = onExit,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = SessionButtonCyan),
                border = BorderStroke(1.dp, SessionButtonCyan),
            ) {
                Text(stringResource(R.string.action_end))
            }
        }
    }
}

/** Schrift mit weichem dunklen Schimmer (kein harter Rand) – ruhig und lesbar auf Magenta & Dunkel. */
@Composable
private fun GlowLabel(
    text: String,
    style: TextStyle,
    fill: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = style.merge(
            TextStyle(
                color = fill,
                shadow = Shadow(color = SessionTextGlow, offset = Offset.Zero, blurRadius = 18f),
            ),
        ),
        modifier = modifier,
    )
}

/** Anzeigeskala des Kreises: 0f ausgeatmet … 1f eingeatmet. Stetig über Phasengrenzen. */
private fun breathingFraction(state: SessionUiState): Float {
    val progress = if (state.phaseTotalMs > 0L) {
        ((state.phaseTotalMs - state.remainingMs).toFloat() / state.phaseTotalMs).coerceIn(0f, 1f)
    } else {
        0f
    }
    return when (state.phaseType) {
        PhaseType.INHALE, PhaseType.INHALE_TOP_UP -> progress
        PhaseType.EXHALE -> 1f - progress
        PhaseType.HOLD_FULL -> 1f
        PhaseType.HOLD_EMPTY -> 0f
        PhaseType.REST -> 0.5f
        null -> 0.5f
    }
}

/** Sekundentext: Countdown bei fester Dauer, Hochzählen bei offener Phase. */
private fun secondsText(state: SessionUiState): String = when {
    state.status == SessionStatus.WAITING_FOR_USER -> (state.elapsedMs / 1000L).toString()
    state.phaseTotalMs > 0L -> ceil(state.remainingMs / 1000.0).toInt().coerceAtLeast(0).toString()
    else -> ""
}
