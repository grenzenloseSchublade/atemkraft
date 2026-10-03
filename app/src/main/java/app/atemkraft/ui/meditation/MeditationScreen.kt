package app.atemkraft.ui.meditation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.atemkraft.R
import app.atemkraft.cue.HapticPlayer
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.ui.components.AdaptiveButtonRow
import app.atemkraft.ui.components.FinishedPanel
import app.atemkraft.ui.components.GlowText
import app.atemkraft.ui.components.MiniNowPlayingBar
import app.atemkraft.ui.components.OverlayChrome
import app.atemkraft.ui.components.SessionPrimaryButton
import app.atemkraft.ui.components.SessionSecondaryButton
import app.atemkraft.ui.components.SessionStopButton
import app.atemkraft.ui.components.rememberTapFlash
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SelectChip
import app.atemkraft.ui.components.StartSplitButton
import app.atemkraft.ui.components.Stepper
import app.atemkraft.ui.components.BreathingCircle
import app.atemkraft.ui.components.PauseFlash
import app.atemkraft.ui.components.SessionRunningLayout
import app.atemkraft.ui.theme.NeonCyan
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.SessionTextGlow
import app.atemkraft.ui.theme.SynthTrack
import app.atemkraft.ui.theme.SessionTextYellow
import app.atemkraft.ui.theme.SessionButtonCyan
import app.atemkraft.ui.theme.SessionButtonPink
import kotlinx.coroutines.delay

/** Auswählbare Dauer-Vorgaben (Minuten) und Intervall-Gong-Optionen (Minuten). */
private val DURATION_PRESETS = listOf(5, 10, 15, 20, 30, 45, 60, 90)

/**
 * Meditations-Tab: Auswahl (Modus Timer/Frei, Dauer, Intervall-Gong, Sprach-Anleitung) und
 * laufende Sitzung als Overlay (ruhiger Ring + Zeit). Reiner Zustand + Callbacks – der Ablauf
 * liegt im [MeditationController].
 */
@Composable
fun MeditationScreen(
    initialConfig: MeditationConfig,
    speechAvailable: Boolean,
    gongIntervalMin: Int,
    onStart: (MeditationConfig) -> Unit,
) {
    // Der Tab zeigt nur die Auswahl; die laufende/abgeschlossene Sitzung liegt als Vollbild-Overlay
    // darüber ([MeditationOverlay]) – so ist sie (wie die Atem-Session) minimierbar und über die
    // Mini-Leiste erreichbar.
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        SelectionContent(initialConfig, speechAvailable, gongIntervalMin, onStart)
    }
}

/** „Now-Playing"-Leiste der Meditation (Titel + Zeit/Status); tippen öffnet das Vollbild-Overlay. */
@Composable
fun MiniMeditationBar(state: MeditationUiState, onClick: () -> Unit) {
    val title = stringResource(R.string.meditation_title)
    val statusText = when (state.status) {
        MeditationStatus.FINISHED -> stringResource(R.string.meditation_done_title)
        MeditationStatus.PAUSED -> stringResource(R.string.session_paused)
        else -> if (state.mode == MeditationMode.TIMED) formatMeditationTime(state.remainingMs)
        else formatMeditationTime(state.elapsedMs)
    }
    MiniNowPlayingBar(title = title, statusText = statusText, onClick = onClick)
}

/**
 * Vollbild-Overlay der laufenden/abgeschlossenen Meditation – identischer Rahmen wie die
 * Atem-Session ([app.atemkraft.ui.session.SessionOverlay]) über das geteilte [OverlayChrome].
 */
@Composable
fun MeditationOverlay(
    state: MeditationUiState,
    onMinimize: () -> Unit,
    onToggleMute: () -> Unit,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    onEnd: () -> Unit,
) {
    OverlayChrome(muted = state.muted, onMinimize = onMinimize, onToggleMute = onToggleMute) {
        if (state.status == MeditationStatus.FINISHED) {
            FinishedPanel(
                title = stringResource(R.string.meditation_done_title),
                onAgain = onRestart,
                onExit = onEnd,
            )
        } else {
            RunningContent(state = state, onTogglePause = onTogglePause, onRestart = onRestart, onEnd = onEnd)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SelectionContent(
    initialConfig: MeditationConfig,
    speechAvailable: Boolean,
    gongIntervalMin: Int,
    onStart: (MeditationConfig) -> Unit,
) {
    var mode by remember(initialConfig) { mutableStateOf(initialConfig.mode) }
    var minutes by remember(initialConfig) { mutableIntStateOf(initialConfig.minutes) }
    var startEndGong by remember(initialConfig) { mutableStateOf(initialConfig.startEndGong) }
    var intervalOn by remember(initialConfig) { mutableStateOf(initialConfig.gongEveryMin != null) }
    var speech by remember(initialConfig) { mutableStateOf(initialConfig.speech) }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenPadding),
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringResource(R.string.meditation_title),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = stringResource(R.string.meditation_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
            )

            Spacer(Modifier.height(20.dp))
            // Modus: Timer / Frei
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip(
                    label = stringResource(R.string.meditation_mode_timed),
                    selected = mode == MeditationMode.TIMED,
                    onClick = { mode = MeditationMode.TIMED },
                )
                SelectChip(
                    label = stringResource(R.string.meditation_mode_free),
                    selected = mode == MeditationMode.FREE,
                    onClick = { mode = MeditationMode.FREE },
                )
            }

            if (mode == MeditationMode.TIMED) {
                SectionHeader(stringResource(R.string.meditation_duration), NeonCyan)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DURATION_PRESETS.forEach { m ->
                        SelectChip(
                            label = stringResource(R.string.meditation_minutes, m),
                            selected = minutes == m,
                            onClick = { minutes = m },
                        )
                    }
                }
                // Frei wählbare Dauer (beliebige Minuten); Presets sind nur Schnellwahl.
                Stepper(
                    label = stringResource(R.string.adjust_minutes),
                    value = minutes,
                    range = 1..120,
                    onChange = { minutes = it },
                )
            } else {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.meditation_free_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                )
            }

            SectionHeader(stringResource(R.string.meditation_gong_label), NeonCyan)
            // Nur der Modus wird hier gewählt; die Intervall-Länge steht in den Einstellungen.
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip(
                    label = stringResource(R.string.meditation_off),
                    selected = !startEndGong && !intervalOn,
                    onClick = { startEndGong = false; intervalOn = false },
                )
                SelectChip(
                    label = stringResource(R.string.meditation_gong_startstop),
                    selected = startEndGong && !intervalOn,
                    onClick = { startEndGong = true; intervalOn = false },
                )
                SelectChip(
                    label = stringResource(R.string.meditation_interval_minutes, gongIntervalMin),
                    selected = intervalOn,
                    onClick = { startEndGong = true; intervalOn = true },
                )
            }

            SectionHeader(stringResource(R.string.meditation_speech_label), NeonCyan)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = if (speechAvailable) stringResource(R.string.meditation_speech_hint)
                    else stringResource(R.string.meditation_speech_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = speech && speechAvailable,
                    onCheckedChange = { speech = it },
                    enabled = speechAvailable,
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        // Gepinnter Start über der Tab-Leiste (wie auf der Detailseite) – als Split-Button:
        // weicht die Auswahl von den App-Standards ab, gleitet rechts der Kreispfeil herein.
        Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.ScreenPadding, vertical = Dimens.ListGap),
            ) {
                val defaults = MeditationConfig()
                StartSplitButton(
                    label = stringResource(R.string.action_start),
                    resetVisible = mode != defaults.mode || minutes != defaults.minutes ||
                        startEndGong != defaults.startEndGong ||
                        intervalOn != (defaults.gongEveryMin != null) || speech != defaults.speech,
                    onStart = {
                        onStart(
                            MeditationConfig(
                                mode = mode,
                                minutes = minutes,
                                startEndGong = startEndGong,
                                gongEveryMin = if (intervalOn) gongIntervalMin else null,
                                speech = speech && speechAvailable,
                            ),
                        )
                    },
                    onReset = {
                        mode = defaults.mode
                        minutes = defaults.minutes
                        startEndGong = defaults.startEndGong
                        intervalOn = defaults.gongEveryMin != null
                        speech = defaults.speech
                    },
                )
            }
        }
    }
}

@Composable
private fun RunningContent(
    state: MeditationUiState,
    onTogglePause: () -> Unit,
    onRestart: () -> Unit,
    onEnd: () -> Unit,
) {
    val preparing = state.status == MeditationStatus.PREPARING
    val paused = state.status == MeditationStatus.PAUSED
    val ringDescription = stringResource(R.string.cd_meditation_ring)
    // TalkBack-Ansage der Zustandswechsel (wie die Atem-Session ihren Phasennamen ansagt).
    val stateAnnounce = when {
        preparing -> stringResource(R.string.session_get_ready)
        paused -> stringResource(R.string.session_paused)
        else -> stringResource(R.string.meditation_title)
    }

    // Ruhiger, VOLL gefüllter Kreis (kein Wachstum); der Fortschritt steht in der Zahl.
    val timeText = when {
        preparing -> state.countdown.toString()
        state.mode == MeditationMode.TIMED -> formatMeditationTime(state.remainingMs)
        else -> formatMeditationTime(state.elapsedMs)
    }
    val circleInteraction = remember { MutableInteractionSource() }

    // Tap-Flash (geteilt mit der Atem-Session).
    val tapFlash = rememberTapFlash()
    // UI-Haptik: kurzes, weiches Tick beim Kreis-Tap – über den Vibrator (USAGE_ALARM), damit es
    // nicht am System-Schalter „Tipp-Vibration" hängt (Samsung verwirft das sonst still).
    val context = LocalContext.current
    val haptics = remember { HapticPlayer(context) }
    fun flashToggle() {
        haptics.tick()
        tapFlash.flash(isPause = state.status == MeditationStatus.RUNNING) // läuft → wird pausiert
        onTogglePause()
    }

    // Gleiches Gerüst wie die Atem-Session (SessionRunningLayout): Titel oben, Kreis im freien
    // Platz, Steuerung unten – nichts überlappt, auch bei großer Schrift.
    SessionRunningLayout(
        modifier = Modifier.fillMaxSize().padding(Dimens.SessionPadding),
        top = {
            // Titel oben (wie die Atem-Session ihren Übungsnamen zeigt) – nicht nur „nackte" Zeit.
            Text(
                text = stringResource(R.string.meditation_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
                // liveRegion: TalkBack sagt Zustandswechsel (Vorbereitung/läuft/pausiert) an.
                modifier = Modifier
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        contentDescription = stateAnnounce
                    },
            )
        },
        bottom = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AdaptiveButtonRow(modifier = Modifier.fillMaxWidth(), spacing = Dimens.ListGap) {
                    SessionPrimaryButton(
                        text = stringResource(if (paused) R.string.action_resume else R.string.action_pause),
                        onClick = onTogglePause,
                        enabled = !preparing,
                    )
                    SessionSecondaryButton(
                        text = stringResource(R.string.action_restart),
                        onClick = onRestart,
                        enabled = !preparing,
                    )
                    SessionStopButton(
                        text = stringResource(R.string.action_stop),
                        onClick = onEnd,
                    )
                }
            }
        },
    ) { side ->
            Box(contentAlignment = Alignment.Center) {
                BreathingCircle(
                    fraction = 1f,
                    modifier = Modifier
                        .size(side)
                        .semantics { contentDescription = ringDescription }
                        // Wie in der Atem-Session: Tippen auf den Kreis pausiert/setzt fort.
                        .then(
                            if (!preparing) {
                                Modifier.clickable(
                                    interactionSource = circleInteraction,
                                    indication = null,
                                ) { flashToggle() }
                            } else {
                                Modifier
                            },
                        ),
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        if (preparing) {
                            GlowText(
                                text = stringResource(R.string.session_get_ready),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                            Spacer(Modifier.height(6.dp))
                        }
                        GlowText(text = timeText, style = MaterialTheme.typography.displaySmall)
                    }
                }
                // Dezenter Fortschritts-Ring (nur Timer): dünner Bogen entlang der Bahn.
                if (state.mode == MeditationMode.TIMED && !preparing) {
                    val total = (state.elapsedMs + state.remainingMs).coerceAtLeast(1L)
                    val progress = (state.elapsedMs.toFloat() / total).coerceIn(0f, 1f)
                    Canvas(modifier = Modifier.size(side)) {
                        val stroke = 2.5.dp.toPx()
                        drawArc(
                            color = SynthTrack.copy(alpha = 0.35f),
                            startAngle = -90f,
                            sweepAngle = progress * 360f,
                            useCenter = false,
                            topLeft = Offset(stroke / 2f, stroke / 2f),
                            size = Size(size.width - stroke, size.height - stroke),
                            style = Stroke(width = stroke, cap = StrokeCap.Round),
                        )
                    }
                }
                // Flash-Overlay ÜBER dem Kreis (Geschwister, nicht im Kreis-Content).
                PauseFlash(alpha = tapFlash.alpha, isPause = tapFlash.isPause)
            }
    }
}


