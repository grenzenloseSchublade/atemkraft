package app.atemkraft

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.atemkraft.data.CueSettings
import app.atemkraft.data.MeditationDisplay
import app.atemkraft.data.SessionDisplay
import app.atemkraft.data.SettingsRepository
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.SessionConfig
import app.atemkraft.playback.MeditationStatus
import app.atemkraft.playback.SessionStatus
import app.atemkraft.ui.meditation.MeditationActions
import app.atemkraft.ui.meditation.MeditationOverlay
import app.atemkraft.ui.meditation.MiniMeditationBar
import app.atemkraft.ui.session.MiniSessionBar
import app.atemkraft.ui.session.SessionActions
import app.atemkraft.ui.session.SessionOverlay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Was der App-Rahmen von den beiden app-weiten Abläufen (Atemübung, Meditation) weiß: ihr Status,
 * ob ihr Overlay offen ist ([sessionExpanded], [meditationExpanded], überstehen das Drehen) und die
 * Anzeige-Einstellungen der Overlays. Gestartet wird über [startBreath] bzw. [startMeditation]:
 * beim ersten Start erst nach der Benachrichtigung fragen (ARC-6), dann starten und das Overlay
 * öffnen. Es läuft immer höchstens ein Ablauf: Ein neuer Start ersetzt den laufenden, der dabei nach
 * der Logbuch-Regel endet ([app.atemkraft.domain.SessionTiming.MIN_LOG_MS]) statt spurlos.
 *
 * Nur der Status liegt hier: Der volle Zustand tickt während einer Sitzung alle 33 ms und wird erst
 * in Mini-Leiste bzw. Overlay gelesen ([PlaybackMiniBars], [PlaybackOverlays]) – sonst setzte die
 * ganze App rund 30-mal pro Sekunde neu an (ARC-3).
 */
@Stable
internal class PlaybackState(
    private val container: AppContainer,
    sessionStatus: State<SessionStatus>,
    meditationStatus: State<MeditationStatus>,
    sessionExpanded: MutableState<Boolean>,
    meditationExpanded: MutableState<Boolean>,
    cueSettings: State<CueSettings>,
    sessionDisplay: State<SessionDisplay>,
    meditationDisplay: State<MeditationDisplay>,
    private val askNotifications: (then: () -> Unit) -> Unit,
) {
    val sessionStatus by sessionStatus
    val meditationStatus by meditationStatus
    var sessionExpanded by sessionExpanded
    var meditationExpanded by meditationExpanded

    /** Klänge und Vibration der App: Tipp-Tick in den Overlays, Vorschau-Ton im Muster-Editor. */
    val cueSettings by cueSettings
    val sessionDisplay by sessionDisplay
    val meditationDisplay by meditationDisplay

    /** „Aktiv“ (auch am Ende-Screen) steuert Overlay und Mini-Leiste. */
    val sessionActive: Boolean get() = sessionStatus != SessionStatus.IDLE
    val meditationActive: Boolean get() = meditationStatus != MeditationStatus.IDLE

    /** Eine der beiden läuft (auch minimiert): Der Vorschau-Ton des Muster-Editors schweigt dann. */
    val anyActive: Boolean get() = sessionActive || meditationActive

    /**
     * Ein Overlay liegt über allem: Die App darunter ist für TalkBack und Tastatur nicht da
     * (A11Y-07, MUSTER-07) – sonst führte Wischen hinter „Beenden“ auf unsichtbare Karten und Tabs.
     */
    val overlayOpen: Boolean get() = (sessionActive && sessionExpanded) || (meditationActive && meditationExpanded)

    /** Bildschirm wach halten: nur während des Laufs, nicht in Pause und nicht am Ende. */
    val keepScreenOn: Boolean
        get() = sessionStatus == SessionStatus.RUNNING ||
            sessionStatus == SessionStatus.WAITING_FOR_USER ||
            meditationStatus == MeditationStatus.RUNNING ||
            meditationStatus == MeditationStatus.PREPARING

    val breath get() = container.breathSessionController
    val meditation get() = container.meditationController
    val haptics get() = container.hapticPlayer
    val silenceAfterBreath get() = container.silenceAfterBreath

    /**
     * Atemübung starten und ihr Overlay öffnen; „Danach in die Stille“ startet mit dem Standard aus den
     * Einstellungen. Eine laufende Übung ersetzt [BreathSessionController.start] selbst; eine Meditation
     * endet still, erst nach dem Start (der Service bleibt so durchgehend gehalten). [onStarted] läuft,
     * sobald die Übung wirklich läuft – beim ersten Mal erst nach der Antwort auf die Erlaubnis.
     */
    fun startBreath(exercise: Exercise, config: SessionConfig, onStarted: () -> Unit = {}) {
        askNotifications {
            breath.start(exercise, config, silenceAfter = sessionDisplay.silenceAfter)
            sessionExpanded = true
            if (meditationActive) {
                meditation.discard()
                meditationExpanded = false
            }
            onStarted()
        }
    }

    /** Meditation starten und ihr Overlay öffnen (wie beim Start einer Atemübung, eine laufende Übung endet). */
    fun startMeditation(config: MeditationConfig) {
        askNotifications {
            meditation.start(config)
            meditationExpanded = true
            if (sessionActive) {
                breath.stop()
                sessionExpanded = false
            }
        }
    }

    /**
     * Danach in die Stille: Den Übergang macht der Ablauf selbst ([app.atemkraft.playback.SilenceAfterBreath],
     * auch hinter dem Sperrbildschirm). Die Oberfläche übernimmt nur, ob das Session-Overlay offen
     * war – auch erst beim nächsten Sichtbarwerden, wenn der Übergang im Hintergrund kam.
     */
    fun takeOverSilence() {
        if (meditationActive && silenceAfterBreath.consumeHandover()) {
            meditationExpanded = sessionExpanded
            sessionExpanded = false
        }
    }
}

/** Der [PlaybackState] der App, gesammelt ab App-Start (Anzeige-Einstellungen springen nicht). */
@Composable
internal fun rememberPlaybackState(container: AppContainer): PlaybackState {
    val settings = container.settingsRepository
    val sessionStatus = container.breathSessionController.status.collectAsStateWithLifecycle()
    val meditationStatus = container.meditationController.status.collectAsStateWithLifecycle()
    val sessionExpanded = rememberSaveable { mutableStateOf(false) }
    val meditationExpanded = rememberSaveable { mutableStateOf(false) }
    val cueSettings = settings.cueSettings.collectAsStateWithLifecycle(initialValue = CueSettings())
    val sessionDisplay = settings.sessionDisplay.collectAsStateWithLifecycle(initialValue = SessionDisplay())
    val meditationDisplay = settings.meditationDisplay.collectAsStateWithLifecycle(initialValue = MeditationDisplay())
    // Erlaubnis für die Benachrichtigung beim ersten Start einer Sitzung, gleich welcher Art (ARC-6).
    val askNotifications = rememberNotificationAsk(settings, container.appScope)
    val playback = remember(container, sessionStatus, meditationStatus, sessionExpanded, meditationExpanded, cueSettings, sessionDisplay, meditationDisplay, askNotifications) {
        PlaybackState(
            container = container,
            sessionStatus = sessionStatus,
            meditationStatus = meditationStatus,
            sessionExpanded = sessionExpanded,
            meditationExpanded = meditationExpanded,
            cueSettings = cueSettings,
            sessionDisplay = sessionDisplay,
            meditationDisplay = meditationDisplay,
            askNotifications = askNotifications,
        )
    }
    // Übergang in die Stille, auch erst beim nächsten Sichtbarwerden, wenn er im Hintergrund kam.
    val handedOver by container.silenceAfterBreath.handedOver.collectAsStateWithLifecycle()
    LaunchedEffect(handedOver, playback.meditationActive) { if (handedOver) playback.takeOverSilence() }
    return playback
}

/** Minimiert erscheinen laufende Abläufe als „Now-Playing“-Mini-Leiste über der Tab-Leiste. */
@Composable
internal fun PlaybackMiniBars(playback: PlaybackState) {
    if (playback.sessionActive && !playback.sessionExpanded) {
        SessionHost(playback.breath.state) { state ->
            MiniSessionBar(state = state, onClick = { playback.sessionExpanded = true })
        }
    }
    if (playback.meditationActive && !playback.meditationExpanded) {
        SessionHost(playback.meditation.state) { state ->
            MiniMeditationBar(state = state, onClick = { playback.meditationExpanded = true })
        }
    }
}

/**
 * Vollbild-Overlays von Atemübung und Meditation über allem, dazu das Wach-Halten des
 * Bildschirms. System-Zurück regelt jedes Overlay selbst (minimieren, am Ende-Screen wie „Ende“).
 * [onOpenLogbook]: „Im Logbuch ansehen ›“ am Ende, nachdem der Ablauf beendet und das Overlay
 * geschlossen ist.
 */
@Composable
internal fun PlaybackOverlays(playback: PlaybackState, onOpenLogbook: () -> Unit) {
    KeepScreenOn(enabled = playback.keepScreenOn)
    val breath = playback.breath
    val meditation = playback.meditation

    if (playback.sessionActive && playback.sessionExpanded) {
        SessionHost(breath.state) { state ->
            SessionOverlay(
                state = state,
                display = playback.sessionDisplay,
                actions = SessionActions(
                    onTogglePause = {
                        if (state.status == SessionStatus.PAUSED) {
                            breath.resume()
                        } else {
                            breath.pause()
                        }
                    },
                    // Tipp-Tick nur bei eingeschaltetem App-Schalter „Vibration“ (AUDIO-05).
                    onTapHaptic = { playback.haptics.tick(enabled = playback.cueSettings.haptics) },
                    onContinue = breath::continueFromUserPaced,
                    onRestart = breath::restart,
                    onStop = {
                        breath.stop()
                        playback.sessionExpanded = false
                    },
                    // „Im Logbuch ansehen ›“ am Ende: beendet wie „Ende“ und öffnet das Logbuch.
                    onOpenLogbook = {
                        breath.stop()
                        playback.sessionExpanded = false
                        onOpenLogbook()
                    },
                    // Derselbe Weg wie „Danach in die Stille“; nicht nach Atmen im Gehen.
                    onIntoSilence = if (state.silenceAfterAllowed) playback.silenceAfterBreath::intoSilence else null,
                ),
                onMinimize = { playback.sessionExpanded = false },
                onToggleMute = breath::toggleMute,
                onToggleSilenceAfter = breath::toggleSilenceAfter,
            )
        }
    }

    // Vollbild-Meditation über allem (gespiegelt von der Session): minimierbar zur Mini-Leiste.
    if (playback.meditationActive && playback.meditationExpanded) {
        SessionHost(meditation.state) { state ->
            MeditationOverlay(
                state = state,
                display = playback.meditationDisplay,
                actions = MeditationActions(
                    onTogglePause = {
                        if (state.status == MeditationStatus.PAUSED) {
                            meditation.resume()
                        } else {
                            meditation.pause()
                        }
                    },
                    onTapHaptic = { playback.haptics.tick(enabled = playback.cueSettings.haptics) },
                    onRestart = meditation::restart,
                    // Ab einer Minute schließt „Beenden“ regulär ab: Das Overlay bleibt für den Ende-Screen
                    // offen. Sonst (kurz, Countdown, „Ende“ am Ende-Screen) ist die Sitzung weg.
                    onEnd = {
                        meditation.end()
                        if (meditation.state.value.status == MeditationStatus.IDLE) playback.meditationExpanded = false
                    },
                    onOpenLogbook = {
                        meditation.end()
                        playback.meditationExpanded = false
                        onOpenLogbook()
                    },
                ),
                onMinimize = { playback.meditationExpanded = false },
                onToggleMute = meditation::toggleMute,
            )
        }
    }
}

/**
 * Sammelt den vollen Sitzungszustand erst hier: Nur dieser Teilbaum (Mini-Leiste oder Overlay)
 * setzt bei jedem Frame-Tick neu an, nicht die App-Wurzel (ARC-3).
 */
@Composable
private fun <T> SessionHost(state: StateFlow<T>, content: @Composable (T) -> Unit) {
    val current by state.collectAsStateWithLifecycle()
    content(current)
}

/**
 * Fragt ab Android 13 einmalig nach der Benachrichtigungs-Erlaubnis – im Moment, in dem sie Sinn
 * ergibt: beim ersten Start einer Sitzung (Meditation oder Atemübung), nicht beim App-Start
 * (ARC-6). Und zwar **vor** dem Start: Die Sitzung ([then]) beginnt erst nach der Antwort, gleich
 * welcher – sonst liefe der Countdown unter dem Systemdialog, und die erste Phase wäre verpasst.
 * Gebraucht wird die Erlaubnis nur für die Benachrichtigung der laufenden Sitzung (Kanal-Beschreibung
 * in den Systemeinstellungen sagt, wofür); bei Ablehnung läuft die Sitzung trotzdem. Kein eigener
 * Erklärdialog davor. Weil auch die Atemübung fragt, ist sie vor „Danach in die Stille“ geklärt – den
 * Übergang startet der Ablauf ohne Oberfläche. Danach fragt die App nie wieder. Bis der gespeicherte
 * Stand geladen ist, gilt „schon gefragt“ – lieber einmal nicht fragen als doppelt. Gemerkt wird in
 * [scope], dem App-weiten Scope. Wird die Activity mitten im Dialog neu aufgebaut, entfällt der
 * wartende Start; ein zweiter Tipp startet dann ohne Frage.
 */
@Composable
private fun rememberNotificationAsk(settings: SettingsRepository, scope: CoroutineScope): (then: () -> Unit) -> Unit {
    val context = LocalContext.current
    val asked = settings.notificationAsked.collectAsStateWithLifecycle(initialValue = true)
    val pending = remember { mutableStateOf<(() -> Unit)?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        pending.value?.invoke()
        pending.value = null
    }
    return remember(context, settings, scope, asked, launcher) {
        { then ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !asked.value &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                scope.launch { settings.setNotificationAsked() }
                pending.value = then
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                then()
            }
        }
    }
}

@Composable
private fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        val window = (view.context as? Activity)?.window
        if (enabled) window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }
}
