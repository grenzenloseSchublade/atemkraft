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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.cue.HapticPlayer
import app.atemkraft.domain.PhaseType
import app.atemkraft.ui.components.AdaptiveButtonRow
import app.atemkraft.ui.components.BreathingCircle
import app.atemkraft.ui.components.FinishedPanel
import app.atemkraft.ui.components.GlowText
import app.atemkraft.ui.components.PauseFlash
import app.atemkraft.ui.components.SessionPrimaryButton
import app.atemkraft.ui.components.SessionSecondaryButton
import app.atemkraft.ui.components.SessionStopButton
import app.atemkraft.ui.components.rememberTapFlash
import app.atemkraft.ui.components.SessionRunningLayout
import app.atemkraft.ui.components.WholeWordText
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY
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
            FinishedPanel(title = stringResource(R.string.session_done_question), onAgain = onRestart, onExit = onStop)
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

    // Tap-Flash (geteilt mit der Meditation): großes Pause/Play-Symbol beim Antippen.
    val tapFlash = rememberTapFlash()
    // UI-Haptik: kurzes, weiches Tick beim Kreis-Tap – über den Vibrator (USAGE_ALARM), damit es
    // nicht am System-Schalter „Tipp-Vibration" hängt (Samsung verwirft das sonst still).
    val context = LocalContext.current
    val haptics = remember { HapticPlayer(context) }
    fun flashToggle() {
        haptics.tick()
        tapFlash.flash(isPause = state.status == SessionStatus.RUNNING) // läuft → wird pausiert
        onTogglePause()
    }

    // Gerüst teilt sich die Atem-Session mit der Meditation: Kopf oben, Kreis im freien Platz
    // dazwischen, Steuerung unten (SessionRunningLayout). Die ausgeklappte Anleitung legt sich
    // über den Kreisrand, statt ihn zu verschieben.
    // Höhe der „Als Nächstes“-Zeile aus der Zeilenhöhe ihres Stils (wächst mit der Systemschrift);
    // fest reserviert, damit der Kreis nicht springt, wenn die Zeile erscheint (LAYOUT-03).
    val nextRowHeight = with(LocalDensity.current) { MaterialTheme.typography.labelMedium.lineHeight.toDp() }
    SessionRunningLayout(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.SessionPadding),
        // „Als Nächstes“-Zeile unter dem Kreis: 12 dp Abstand + eine Zeile.
        centerExtra = if (showNextPhase) 12.dp + nextRowHeight else 0.dp,
        top = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = if (!preparing && state.roundCount > 1) {
                        stringResource(R.string.session_round, state.roundIndex + 1, state.roundCount)
                    } else {
                        state.exerciseName
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
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
                            // 48-dp-Mindest-Touch-Target (Bedienung mitten in der Session).
                            .heightIn(min = 48.dp)
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
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        },
        bottom = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Immer gemessen, nur im offenen Hold sichtbar: So bleibt die Höhe der Steuerung
                // konstant und der Kreis springt nicht, wenn der Hinweis erscheint.
                Text(
                    text = stringResource(R.string.session_tap_to_continue),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                    modifier = Modifier
                        .alpha(if (waiting) 1f else 0f)
                        .then(if (waiting) Modifier else Modifier.clearAndSetSemantics {}),
                )
                Spacer(Modifier.height(16.dp))

                // Immer alle drei Buttons rendern (im Countdown deaktiviert statt abwesend) –
                // so springt das Layout beim Übergang Countdown → Übung nicht.
                AdaptiveButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SessionPrimaryButton(
                        text = stringResource(
                            if (state.status == SessionStatus.PAUSED) R.string.action_resume
                            else R.string.action_pause,
                        ),
                        onClick = onTogglePause,
                        enabled = !preparing && !waiting,
                    )
                    SessionSecondaryButton(
                        text = stringResource(R.string.action_restart),
                        onClick = onRestart,
                        enabled = !preparing,
                    )
                    SessionStopButton(
                        text = stringResource(R.string.action_stop),
                        onClick = onStop,
                    )
                }
            }
        },
    ) { side ->
        val circleDescription = stringResource(R.string.cd_breathing_circle)
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(contentAlignment = Alignment.Center) {
                BreathingCircle(
                    fraction = fraction,
                    modifier = Modifier
                        .size(side)
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
                )
                // Texte als Geschwister ÜBER dem Kreis statt in ihm: Sie bekommen die volle Breite
                // und brechen nie im Wort, auch wenn der Kreis bei großer Schrift klein wird.
                // Antippen erreicht weiterhin den Kreis (Text hat keinen eigenen Klick).
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // LiveRegion sagt Phasenname + Notiz an (der Kreis selbst hat nur eine
                    // statische Beschreibung, sonst liest TalkBack den Phasennamen doppelt).
                    val liveText = note?.let { "$label, $it" } ?: label
                    GlowText(
                        text = label,
                        style = MaterialTheme.typography.headlineMedium,
                        fill = SessionTextYellow,
                        modifier = Modifier.semantics {
                            liveRegion = LiveRegionMode.Polite
                            contentDescription = liveText
                        },
                    )
                    note?.let {
                        GlowText(
                            text = it,
                            style = MaterialTheme.typography.bodyLarge,
                            fill = SessionNoteAmber,
                        )
                    }
                    if (secondsText.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        GlowText(
                            text = secondsText,
                            style = MaterialTheme.typography.displaySmall,
                            fill = SessionTextYellow,
                        )
                    }
                }
                // Flash-Overlay ÜBER dem Kreis (Geschwister, nicht im Kreis-Content) → sicher sichtbar.
                PauseFlash(alpha = tapFlash.alpha, isPause = tapFlash.isPause)
            }
            // Dezent direkt unter dem Kreis: die kommende Phase (abschaltbar).
            // Feste Höhe reservieren, damit der Kreis NICHT springt, wenn die Zeile
            // erscheint/verschwindet oder der Text (kurz/lang) wechselt.
            if (showNextPhase) {
                Spacer(Modifier.height(12.dp))
                val hintColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                val hintStyle = MaterialTheme.typography.labelMedium
                val nextLabel = stringResource(R.string.session_next)
                // Alle möglichen Phasennamen: Die Entscheidung unten gilt für die ganze Session,
                // die Zeile wechselt also nicht zwischen den Phasen ihr Layout.
                val phaseLabels = PhaseType.entries.map { phaseDisplayLabel(it, null) }
                val measurer = rememberTextMeasurer()
                BoxWithConstraints(
                    modifier = Modifier.heightIn(min = nextRowHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    val half = with(LocalDensity.current) {
                        (maxWidth.toPx() - measurer.measure(" · ", hintStyle).size.width) / 2f
                    }
                    val splitFits = measurer.measure(nextLabel, hintStyle).size.width <= half &&
                        phaseLabels.all { measurer.measure(it, hintStyle).size.width <= half }
                    if (!preparing && state.nextPhaseType != null) {
                        val phase = phaseDisplayLabel(state.nextPhaseType, null)
                        if (splitFits) {
                            // „Als Nächstes" rechtsbündig bis zur Mitte, Phasenname linksbündig ab Mitte
                            // → der Trennpunkt liegt fest in der Bildmitte, nichts springt bei Längenwechsel.
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = nextLabel,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.End,
                                    style = hintStyle,
                                    color = hintColor,
                                    maxLines = 1,
                                )
                                Text(text = " · ", style = hintStyle, color = hintColor)
                                Text(
                                    text = phase,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Start,
                                    style = hintStyle,
                                    color = hintColor,
                                    maxLines = 1,
                                )
                            }
                        } else {
                            // Sehr große Schrift auf schmalem Gerät: eine Hälfte reicht nicht –
                            // dann als ein zentrierter Text, ganz statt abgeschnitten (LAYOUT-03).
                            WholeWordText(
                                text = "$nextLabel · $phase",
                                style = hintStyle,
                                color = hintColor,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }
        }
    }
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
