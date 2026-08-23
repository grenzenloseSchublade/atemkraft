package app.atemkraft.ui.meditation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.atemkraft.R
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.ui.components.MiniNowPlayingBar
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SelectChip
import app.atemkraft.ui.components.Stepper
import app.atemkraft.ui.session.BreathingCircle
import app.atemkraft.ui.session.PauseFlash
import app.atemkraft.ui.theme.NeonCyan
import app.atemkraft.ui.theme.SECONDARY
import app.atemkraft.ui.theme.SessionTextGlow
import app.atemkraft.ui.theme.SessionTextYellow
import app.atemkraft.ui.theme.SessionButtonCyan
import app.atemkraft.ui.theme.SessionButtonPink
import kotlinx.coroutines.delay

/** Auswählbare Dauer-Vorgaben (Minuten) und Intervall-Gong-Optionen (Minuten). */
private val DURATION_PRESETS = listOf(5, 10, 15, 20, 30, 45, 60, 90)

/**
 * Meditations-Tab: Auswahl (Modus Timer/Frei, Dauer, Intervall-Gong, Sprach-Anleitung) und
 * laufende Sitzung (ruhiger Ring + Zeit). Reiner Zustand + Callbacks – der Ablauf liegt im
 * [MeditationViewModel].
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
        else -> if (state.mode == MeditationMode.TIMED) formatTime(state.remainingMs)
        else formatTime(state.elapsedMs)
    }
    MiniNowPlayingBar(title = title, statusText = statusText, onClick = onClick)
}

/**
 * Vollbild-Overlay der laufenden/abgeschlossenen Meditation (spiegelt [app.atemkraft.ui.session.SessionOverlay]):
 * oben Minimieren (˅) + In-Session-Stummschalter; darunter die bestehende [RunningContent]/[FinishedContent].
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
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(onClick = onMinimize) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.action_minimize),
                        tint = MaterialTheme.colorScheme.onBackground,
                    )
                }
                // Schneller In-Session-Ton-Schalter (Einstellung bleibt unberührt) – wie in der Atem-Session.
                IconButton(onClick = onToggleMute) {
                    Icon(
                        painter = painterResource(
                            if (state.muted) R.drawable.ic_sound_off else R.drawable.ic_sound_on,
                        ),
                        contentDescription = stringResource(
                            if (state.muted) R.string.action_sound_off else R.string.action_sound_on,
                        ),
                        tint = MaterialTheme.colorScheme.onBackground.copy(alpha = if (state.muted) 0.5f else 1f),
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                if (state.status == MeditationStatus.FINISHED) {
                    FinishedContent(onAgain = onRestart, onExit = onEnd)
                } else {
                    RunningContent(state = state, onTogglePause = onTogglePause, onRestart = onRestart, onEnd = onEnd)
                }
            }
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
                .padding(horizontal = 20.dp),
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

        // Gepinnter Start über der Tab-Leiste (wie auf der Detailseite).
        Surface(tonalElevation = 3.dp, color = MaterialTheme.colorScheme.surface) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Button(
                    onClick = {
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
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.action_start))
                }
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
        state.mode == MeditationMode.TIMED -> formatTime(state.remainingMs)
        else -> formatTime(state.elapsedMs)
    }
    val circleInteraction = remember { MutableInteractionSource() }

    // Tap-Flash wie in der Atem-Session: großes Pause/Play-Symbol kurz aufblinken. Alpha per
    // delay-Schleife animiert (nicht animateTo), damit es auch bei abgeschalteter System-Animation
    // sichtbar ist.
    var flashAlpha by remember { mutableFloatStateOf(0f) }
    var flashIsPause by remember { mutableStateOf(true) }
    var flashTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(flashTrigger) {
        if (flashTrigger > 0) {
            flashAlpha = 1f
            delay(220)
            val steps = 16
            for (i in 1..steps) {
                delay(34)
                flashAlpha = (1f - i.toFloat() / steps).coerceAtLeast(0f)
            }
            flashAlpha = 0f
        }
    }
    fun flashToggle() {
        flashIsPause = state.status == MeditationStatus.RUNNING // läuft → wird pausiert
        flashTrigger++
        onTogglePause()
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        // Titel oben (wie die Atem-Session ihren Übungsnamen zeigt) – nicht nur „nackte" Zeit.
        Text(
            text = stringResource(R.string.meditation_title),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY),
            // zIndex wie in der Atem-Session: Titel bleibt über dem Kreis, falls sie sich je berühren.
            // liveRegion: TalkBack sagt Zustandswechsel (Vorbereitung/läuft/pausiert) an.
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(1f)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = stateAnnounce
                },
        )
        Box(modifier = Modifier.align(Alignment.Center), contentAlignment = Alignment.Center) {
            BreathingCircle(
                fraction = 1f,
                modifier = Modifier
                    .fillMaxWidth(0.90f)
                    .aspectRatio(1f)
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
                        GlowTime(
                            text = stringResource(R.string.session_get_ready),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Spacer(Modifier.height(6.dp))
                    }
                    GlowTime(text = timeText, style = MaterialTheme.typography.displaySmall)
                }
            }
            // Flash-Overlay ÜBER dem Kreis (Geschwister, nicht im Kreis-Content).
            PauseFlash(alpha = flashAlpha, isPause = flashIsPause)
        }

        Column(
            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onTogglePause,
                    modifier = Modifier.weight(1f),
                    enabled = !preparing,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SessionButtonCyan,
                        contentColor = SessionTextGlow,
                        disabledContainerColor = SessionButtonCyan.copy(alpha = 0.25f),
                        disabledContentColor = SessionTextGlow.copy(alpha = 0.5f),
                    ),
                ) {
                    Text(
                        stringResource(
                            if (paused) R.string.action_resume else R.string.action_pause,
                        ),
                    )
                }
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.weight(1f),
                    enabled = !preparing,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = SessionButtonCyan),
                    border = BorderStroke(1.dp, SessionButtonCyan),
                ) {
                    Text(stringResource(R.string.action_restart))
                }
                OutlinedButton(
                    onClick = onEnd,
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
private fun FinishedContent(onAgain: () -> Unit, onExit: () -> Unit) {
    // Aufbau + Farb-Tokens wie die Atem-Session (SessionScreen.FinishedContent).
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // Eyebrow „✓ Geschafft" wie die Atem-Session (Häkchen im Text, nicht als nackter Glyph).
        Text(
            text = "✓ " + stringResource(R.string.session_done_title),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.meditation_done_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Button(
                onClick = onAgain,
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

/** Timer-Schrift im Kreis: exakt das Gold + der Schimmer der Atem-Session (konsistent). */
@Composable
private fun GlowTime(text: String, style: TextStyle) {
    Text(
        text = text,
        style = style.merge(
            TextStyle(
                color = SessionTextYellow,
                shadow = Shadow(color = SessionTextGlow, offset = Offset.Zero, blurRadius = 18f),
            ),
        ),
    )
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    return "%d:%02d".format(totalSeconds / 60L, totalSeconds % 60L)
}
