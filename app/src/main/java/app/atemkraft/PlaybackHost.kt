package app.atemkraft

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.atemkraft.data.CueSettings
import app.atemkraft.data.MeditationDisplay
import app.atemkraft.data.SessionDisplay
import app.atemkraft.data.SettingsRepository
import app.atemkraft.domain.Exercise
import app.atemkraft.domain.ExerciseIds
import app.atemkraft.domain.Feeling
import app.atemkraft.domain.FeelingPrompt
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.domain.SessionConfig
import app.atemkraft.playback.MeditationStatus
import app.atemkraft.playback.SessionStatus
import app.atemkraft.ui.components.EndMark
import app.atemkraft.ui.components.FeelingAsk
import app.atemkraft.ui.meditation.MeditationActions
import app.atemkraft.ui.meditation.MeditationOverlay
import app.atemkraft.ui.meditation.MiniMeditationBar
import app.atemkraft.ui.session.MiniSessionBar
import app.atemkraft.ui.session.SessionActions
import app.atemkraft.ui.session.SessionOverlay
import app.atemkraft.ui.theme.Motion
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Was der App-Rahmen von den beiden app-weiten Abläufen (Atemübung, Meditation) weiß: ihr Status,
 * ob ihr Overlay offen ist ([sessionExpanded], [meditationExpanded], überstehen das Drehen) und die
 * Anzeige-Einstellungen der Overlays. Gestartet wird über [startBreath] bzw. [startMeditation]:
 * sofort starten und das Overlay öffnen – nach der Benachrichtigung fragt die App erst, wenn die
 * erste Sitzung zu Ende ist ([askNotifications], Eigentümer 2026-11-25). Es läuft immer höchstens
 * ein Ablauf: Ein neuer Start ersetzt den laufenden, der dabei nach der Logbuch-Regel endet ([app.atemkraft.domain.SessionTiming.MIN_LOG_MS]) statt spurlos.
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
    feelingPrompt: State<FeelingPrompt>,
    private val askNotificationsOnce: () -> Unit,
) {
    val sessionStatus by sessionStatus
    val meditationStatus by meditationStatus
    var sessionExpanded by sessionExpanded
    var meditationExpanded by meditationExpanded

    /** Klänge und Vibration der App: Tipp-Tick in den Overlays, Vorschau-Ton im Muster-Editor. */
    val cueSettings by cueSettings
    val sessionDisplay by sessionDisplay
    val meditationDisplay by meditationDisplay

    /** „Befinden am Ende fragen“: bis zum ersten Anbieten [FeelingPrompt.UNSET], dann der Schalter. */
    val feelingPrompt by feelingPrompt

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

    /**
     * Gerade aus einer Atemübung in die Stille übergegangen ([takeOverSilence]): Das Overlay der
     * Meditation blendet dann ruhig ein ([SilenceFadeIn]), statt den Atemkreis hart zu ersetzen.
     */
    var silenceFadeIn by mutableStateOf(false)

    val breath get() = container.breathSessionController
    val meditation get() = container.meditationController
    val haptics get() = container.hapticPlayer
    val silenceAfterBreath get() = container.silenceAfterBreath

    /**
     * Frage nach dem Befinden für den Ende-Screen ([answer] setzt die Antwort am Eintrag); `null`, wenn
     * die Einstellung aus ist. Beim ersten Mal mit Hinweis, das Erscheinen setzt den Schalter auf an.
     */
    fun feelingAsk(answer: (Feeling) -> Unit): FeelingAsk? = when (feelingPrompt) {
        FeelingPrompt.OFF -> null

        FeelingPrompt.ON -> FeelingAsk(onAnswer = answer)

        FeelingPrompt.UNSET -> FeelingAsk(
            onAnswer = answer,
            intro = true,
            onIntroShown = { container.appScope.launch { container.settingsRepository.feelingOffered() } },
        )
    }

    /**
     * Nach dem Ende einer Sitzung (Ende-Screen oder „Beenden“): einmal nach der Benachrichtigung
     * fragen, nie vor dem ersten Atemzug (ARC-6, Eigentümer 2026-11-25).
     */
    fun askNotifications() = askNotificationsOnce()

    /** Markierungen „Tut mir gut“ (◉), für den Schalter am Ende-Screen. */
    val markedExercises: Flow<Set<String>> get() = container.settingsRepository.markedExercises

    /**
     * ◉ „Tut mir gut“ am Ende-Screen für [exerciseId], nach derselben Regel wie auf der Detailseite
     * (`ExerciseRepository.isMarkable`: eingebaute Übungen, gespeicherte Muster, die Meditation);
     * sonst `null` (Muster des Tages, Entwurf), dann kein Schalter.
     */
    fun endMark(exerciseId: String, marked: Set<String>): EndMark? {
        if (!container.exerciseRepository.isMarkable(exerciseId)) return null
        return EndMark(marked = exerciseId in marked) { on ->
            container.appScope.launch { container.settingsRepository.setExerciseMarked(exerciseId, on) }
        }
    }

    /**
     * Atemübung starten und ihr Overlay öffnen; „Danach in die Stille“ startet mit der Wahl aus dem
     * Blatt „Anpassen“ ([SessionConfig.silenceMinutes], 0 = aus), ohne sie mit dem Standard aus den
     * Einstellungen. Eine laufende Übung ersetzt [BreathSessionController.start] selbst; eine Meditation
     * endet still, erst nach dem Start (der Service bleibt so durchgehend gehalten). [onStarted] läuft,
     * sobald die Übung läuft.
     */
    fun startBreath(exercise: Exercise, config: SessionConfig, onStarted: () -> Unit = {}) {
        breath.start(exercise, config, silenceAfter = config.silenceMinutes?.let { it > 0 } ?: sessionDisplay.silenceAfter)
        sessionExpanded = true
        if (meditationActive) {
            meditation.discard()
            meditationExpanded = false
        }
        onStarted()
    }

    /** Meditation starten und ihr Overlay öffnen (wie beim Start einer Atemübung, eine laufende Übung endet). */
    fun startMeditation(config: MeditationConfig) {
        meditation.start(config)
        meditationExpanded = true
        if (sessionActive) {
            breath.stop()
            sessionExpanded = false
        }
    }

    /**
     * „Vorher atmen“ im Tab Meditation (Eigentümer 2026-11-25): erst [breathe] (Resonanz-Atmung) für
     * [MeditationConfig.breathBeforeMinutes], dann ohne Bruch die Stille – derselbe Übergang wie
     * „Danach in die Stille“ ([app.atemkraft.playback.SilenceAfterBreath]), nur vom Tab aus. Die Stille
     * dauert, was der Zeitring zeigt (bei ∞ offen); Gong und Stimme wie im Blatt „Klang & Stimme“.
     */
    fun startBreathThenSilence(breathe: Exercise, config: MeditationConfig) {
        val silence = config.minutes.takeIf { config.mode == MeditationMode.TIMED }
        breath.start(breathe, SessionConfig(minutes = config.breathBeforeMinutes, silenceMinutes = silence), silenceAfter = true)
        sessionExpanded = true
        if (meditationActive) {
            meditation.discard()
            meditationExpanded = false
        }
    }

    /**
     * Danach in die Stille: Den Übergang macht der Ablauf selbst ([app.atemkraft.playback.SilenceAfterBreath],
     * auch hinter dem Sperrbildschirm). Die Oberfläche übernimmt nur, ob das Session-Overlay offen
     * war – auch erst beim nächsten Sichtbarwerden, wenn der Übergang im Hintergrund kam.
     */
    fun takeOverSilence() {
        if (meditationActive && silenceAfterBreath.consumeHandover()) {
            silenceFadeIn = sessionExpanded
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
    // Bis der gespeicherte Stand da ist: nicht fragen (kein Hinweis, der beim Laden wieder verschwindet).
    val feelingPrompt = settings.feelingPrompt.collectAsStateWithLifecycle(initialValue = FeelingPrompt.OFF)
    // Erlaubnis für die Benachrichtigung einmal nach dem Ende der ersten Sitzung (ARC-6).
    val askNotifications = rememberNotificationAsk(settings, container.appScope)
    val playback = remember(
        container, sessionStatus, meditationStatus, sessionExpanded, meditationExpanded, cueSettings, sessionDisplay, meditationDisplay, feelingPrompt,
        askNotifications,
    ) {
        PlaybackState(
            container = container,
            sessionStatus = sessionStatus,
            meditationStatus = meditationStatus,
            sessionExpanded = sessionExpanded,
            meditationExpanded = meditationExpanded,
            cueSettings = cueSettings,
            sessionDisplay = sessionDisplay,
            meditationDisplay = meditationDisplay,
            feelingPrompt = feelingPrompt,
            askNotificationsOnce = askNotifications,
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
 * Am Ende-Screen ◉ „Tut mir gut“ für die geübte Übung ([playback] kennt die Markierungen).
 */
@Composable
internal fun PlaybackOverlays(playback: PlaybackState) {
    KeepScreenOn(enabled = playback.keepScreenOn)
    val breath = playback.breath
    val meditation = playback.meditation
    val marked by playback.markedExercises.collectAsStateWithLifecycle(initialValue = emptySet())

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
                        playback.askNotifications()
                    },
                    feelingAsk = playback.feelingAsk(breath::answerFeeling),
                    mark = playback.endMark(state.exerciseId, marked),
                ),
                onMinimize = { playback.sessionExpanded = false },
                onToggleMute = breath::toggleMute,
                onToggleSilenceAfter = breath::toggleSilenceAfter,
            )
        }
    }

    // Vollbild-Meditation über allem (gespiegelt von der Session): minimierbar zur Mini-Leiste.
    if (playback.meditationActive && playback.meditationExpanded) {
        SilenceFadeIn(active = playback.silenceFadeIn, onDone = { playback.silenceFadeIn = false }) {
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
                            if (meditation.state.value.status == MeditationStatus.IDLE) {
                                playback.meditationExpanded = false
                                playback.askNotifications()
                            }
                        },
                        feelingAsk = playback.feelingAsk(meditation::answerFeeling),
                        // Nach „Atmen → Stille“ markiert ◉ die geübte Übung, sonst die Meditation.
                        mark = playback.endMark(state.afterExerciseId ?: ExerciseIds.MEDITATION, marked),
                    ),
                    onMinimize = { playback.meditationExpanded = false },
                    onToggleMute = meditation::toggleMute,
                )
            }
        }
    }
}

/**
 * Übergang in die Stille (Eigentümer 2026-11-25): Der Grund steht sofort, der Inhalt des
 * Meditations-Overlays blendet in [Motion.CalmFade] ein – der Atemkreis verlischt, der Ring der Stille
 * erscheint, ohne harten Schnitt. Bewusst die ruhige Dauer der Ruhe-Ansicht statt neuer Bewegung
 * (MOTION-04). Bei Animator-Skala 0 steht der Inhalt sofort da. TalkBack sagt den Wechsel über den
 * Bereichstitel „Meditation“ und den Zustand am Titel an (eine Live-Region, A11Y-04).
 */
@Composable
private fun SilenceFadeIn(active: Boolean, onDone: () -> Unit, content: @Composable () -> Unit) {
    val alpha = remember { Animatable(if (active) 0f else 1f) }
    val done by rememberUpdatedState(onDone)
    LaunchedEffect(Unit) {
        if (alpha.value < 1f) alpha.animateTo(1f, tween(Motion.CalmFade))
        if (active) done()
    }
    Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha.value }) { content() }
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
 * ergibt, ohne den ersten Atemzug zu stören (ARC-6, Eigentümer 2026-11-25): wenn die erste Sitzung zu
 * Ende ist und ihr Overlay schließt („Ende“ am Ende-Screen oder „Beenden“), gleich welcher Art. Die
 * erste Sitzung läuft ohne Frage: Ihr Foreground-Service darf ohne die Erlaubnis laufen, nur ihre
 * Benachrichtigung erscheint dann nicht in der Leiste (Android zeigt die laufende App weiter in der
 * Liste aktiver Apps); Ton, Vibration, Bildschirm aus und „Danach in die Stille“ laufen wie sonst.
 * Gebraucht wird die Erlaubnis für diese Benachrichtigung (Pause, Beenden) und die Erinnerung, deren
 * Schalter ebenfalls fragt. Kein eigener Erklärdialog davor (wofür, sagt die Kanalbeschreibung in den
 * Systemeinstellungen). Danach fragt die App nie wieder ungefragt, auch nach einer Ablehnung. Bis der
 * gespeicherte Stand geladen ist, gilt „schon gefragt“ – lieber einmal nicht fragen als doppelt.
 * Gemerkt wird in [scope], dem App-weiten Scope.
 */
@Composable
private fun rememberNotificationAsk(settings: SettingsRepository, scope: CoroutineScope): () -> Unit {
    val context = LocalContext.current
    val asked = settings.notificationAsked.collectAsStateWithLifecycle(initialValue = true)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return remember(context, settings, scope, asked, launcher) {
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !asked.value &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                scope.launch { settings.setNotificationAsked() }
                launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
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
