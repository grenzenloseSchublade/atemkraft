package app.atemkraft.ui.meditation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import app.atemkraft.R
import app.atemkraft.domain.MeditationConfig
import app.atemkraft.domain.MeditationMode
import app.atemkraft.ui.components.BreathingCircle
import app.atemkraft.ui.components.CircleTapTarget
import app.atemkraft.ui.components.FinishedPanel
import app.atemkraft.ui.components.GlowText
import app.atemkraft.ui.components.MiniNowPlayingBar
import app.atemkraft.ui.components.OverlayChrome
import app.atemkraft.ui.components.PauseFlash
import app.atemkraft.ui.components.PinnedActionBar
import app.atemkraft.ui.components.ScreenHeader
import app.atemkraft.ui.components.SectionHeader
import app.atemkraft.ui.components.SegmentedChoiceRow
import app.atemkraft.ui.components.SessionControlBar
import app.atemkraft.ui.components.SessionRunningLayout
import app.atemkraft.ui.components.SettingsAction
import app.atemkraft.ui.components.StartSplitButton
import app.atemkraft.ui.components.SubLabel
import app.atemkraft.ui.components.ToggleRow
import app.atemkraft.ui.components.breathingDiscDiameter
import app.atemkraft.ui.components.choiceGroupHeading
import app.atemkraft.ui.components.rememberTapFlash
import app.atemkraft.ui.components.sessionSummaryParts
import app.atemkraft.ui.theme.Dimens

/** Gong-Wahl im Tab; die Intervall-Länge steht in den Einstellungen. */
private enum class GongChoice {
    OFF,
    START_END,
    INTERVAL,
}

/**
 * Meditations-Tab: Auswahl (Zeitring für die Dauer samt offener Sitzung „∞“, eingeklappt Gong und
 * Sprachanleitung) und laufende Sitzung als Overlay (ruhiger Ring + Zeit). Reiner Zustand + Callbacks – der Ablauf
 * liegt im [MeditationController].
 */
@Composable
fun MeditationScreen(
    initialConfig: MeditationConfig,
    speechAvailable: Boolean,
    gongIntervalMin: Int,
    onOpenSettings: () -> Unit,
    onDurationTick: (major: Boolean) -> Unit,
    onStart: (MeditationConfig) -> Unit,
) {
    // Der Tab zeigt nur die Auswahl; die laufende/abgeschlossene Sitzung liegt als Vollbild-Overlay
    // darüber ([MeditationOverlay]) – so ist sie (wie die Atem-Session) minimierbar und über die
    // Mini-Leiste erreichbar.
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        SelectionContent(initialConfig, speechAvailable, gongIntervalMin, onOpenSettings, onDurationTick, onStart)
    }
}

/** „Now-Playing"-Leiste der Meditation (Titel + Zeit/Status); tippen öffnet das Vollbild-Overlay. */
@Composable
fun MiniMeditationBar(state: MeditationUiState, onClick: () -> Unit) {
    val title = stringResource(R.string.meditation_title)
    val statusText = when (state.status) {
        MeditationStatus.FINISHED -> stringResource(R.string.meditation_done_title)

        MeditationStatus.PAUSED -> stringResource(R.string.session_paused)

        else -> if (state.mode == MeditationMode.TIMED) {
            formatMeditationTime(state.remainingMs)
        } else {
            formatMeditationTime(state.elapsedMs)
        }
    }
    MiniNowPlayingBar(title = title, statusText = statusText, onClick = onClick)
}

/**
 * Vollbild-Overlay der laufenden/abgeschlossenen Meditation – identischer Rahmen wie die
 * Atem-Session ([app.atemkraft.ui.session.SessionOverlay]) über das geteilte [OverlayChrome].
 * Am Ende dieselbe Frage samt Bilanz wie dort; System-Zurück minimiert, am Ende-Screen wirkt es
 * wie „Ende“ (Nutzerentscheidung 2026-10-19).
 */
@Composable
fun MeditationOverlay(
    state: MeditationUiState,
    onMinimize: () -> Unit,
    onToggleMute: () -> Unit,
    onTogglePause: () -> Unit,
    onTapHaptic: () -> Unit,
    onRestart: () -> Unit,
    onEnd: () -> Unit,
) {
    BackHandler { if (state.status == MeditationStatus.FINISHED) onEnd() else onMinimize() }
    OverlayChrome(
        title = stringResource(R.string.meditation_title),
        muted = state.muted,
        onMinimize = onMinimize,
        onToggleMute = onToggleMute,
    ) {
        if (state.status == MeditationStatus.FINISHED) {
            FinishedPanel(
                title = stringResource(R.string.session_done_question),
                onAgain = onRestart,
                onExit = onEnd,
                // Bilanz wie in der Atem-Session: die aktive (pausenbereinigte) Zeit, dieselbe Zahl
                // wie im Logbuch – der Controller setzt elapsedMs beim Beenden darauf.
                summary = sessionSummaryParts(durationMs = state.elapsedMs),
            )
        } else {
            RunningContent(
                state = state,
                onTogglePause = onTogglePause,
                onTapHaptic = onTapHaptic,
                onRestart = onRestart,
                onEnd = onEnd,
            )
        }
    }
}

@Composable
private fun SelectionContent(
    initialConfig: MeditationConfig,
    speechAvailable: Boolean,
    gongIntervalMin: Int,
    onOpenSettings: () -> Unit,
    onDurationTick: (major: Boolean) -> Unit,
    onStart: (MeditationConfig) -> Unit,
) {
    // Saveable: Die Auswahl überlebt den Abstecher in die Einstellungen und Tab-Wechsel. Schlüssel
    // ohne die Intervall-LÄNGE (die ist global, s. SettingsRepository) – sonst würde eine Änderung
    // in den Einstellungen die noch nicht gestartete Auswahl zurücksetzen.
    val key = initialConfig.copy(gongEveryMin = initialConfig.gongEveryMin?.let { 0 })
    // Dauer samt ∞ liegt im DurationPickerState – die Darstellung (Zeitring) liest und ändert nur ihn.
    val duration = rememberDurationPickerState(initialConfig.minutes, initialConfig.mode == MeditationMode.FREE, key)
    var startEndGong by rememberSaveable(key) { mutableStateOf(initialConfig.startEndGong) }
    var intervalOn by rememberSaveable(key) { mutableStateOf(initialConfig.gongEveryMin != null) }
    var speech by rememberSaveable(key) { mutableStateOf(initialConfig.speech) }
    var soundExpanded by rememberSaveable { mutableStateOf(false) }
    val mode = if (duration.free) MeditationMode.FREE else MeditationMode.TIMED

    Column(modifier = Modifier.fillMaxSize()) {
        // Die Seite scrollt nicht: Der Regler bekommt den Platz zwischen Kopf und „Klang & Stimme“
        // und wird bei großer Schrift oder aufgeklapptem Klang kleiner. Erst wenn selbst seine
        // Mindestgröße nicht passt, scrollt die Seite als Notlösung – abgeschnitten wird nie.
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val viewport = maxHeight
            FillBetweenLayout(
                viewportHeight = viewport,
                minCenterHeight = durationPickerMinSide() + Dimens.GapSmall * 2,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Dimens.ScreenPadding),
                top = {
                    ScreenHeader(
                        title = stringResource(R.string.meditation_title),
                        subtitle = stringResource(R.string.meditation_subtitle),
                        action = { SettingsAction(onClick = onOpenSettings) },
                    )
                },
                center = {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(vertical = Dimens.GapSmall),
                        contentAlignment = Alignment.Center,
                    ) {
                        DurationPicker(state = duration, onTick = onDurationTick)
                    }
                },
                bottom = {
                    SoundSection(
                        startEndGong = startEndGong,
                        intervalOn = intervalOn,
                        gongIntervalMin = gongIntervalMin,
                        speech = speech,
                        speechAvailable = speechAvailable,
                        expanded = soundExpanded,
                        onExpandedChange = { soundExpanded = it },
                        onGong = { choice ->
                            startEndGong = choice != GongChoice.OFF
                            intervalOn = choice == GongChoice.INTERVAL
                        },
                        onSpeech = { speech = it },
                    )
                },
            )
        }

        // Gepinnter Start über der Tab-Leiste (wie auf der Detailseite) – als Split-Button:
        // weicht die Auswahl von den App-Standards ab, gleitet rechts der Kreispfeil herein.
        PinnedActionBar {
            val defaults = MeditationConfig()
            StartSplitButton(
                label = stringResource(R.string.action_start),
                resetVisible = mode != defaults.mode || duration.minutes != defaults.minutes ||
                    startEndGong != defaults.startEndGong ||
                    intervalOn != (defaults.gongEveryMin != null) || speech != defaults.speech,
                onStart = {
                    onStart(
                        MeditationConfig(
                            mode = mode,
                            minutes = duration.minutes,
                            startEndGong = startEndGong,
                            gongEveryMin = if (intervalOn) gongIntervalMin else null,
                            speech = speech && speechAvailable,
                        ),
                    )
                },
                onReset = {
                    duration.select(defaults.minutes)
                    if (defaults.mode == MeditationMode.FREE) duration.selectFree()
                    if (duration.fineAdjust) duration.toggleFineAdjust()
                    startEndGong = defaults.startEndGong
                    intervalOn = defaults.gongEveryMin != null
                    speech = defaults.speech
                },
            )
        }
    }
}

/**
 * Darstellung der Dauerwahl – die EINZIGE Stelle, die sie kennt. Variante F (wischbare Skala)
 * tauscht nur `TimeDial` gegen `TimeRuler` mit derselben Signatur; Zustand, Haptik, Start, Reset
 * und Platzverteilung bleiben unverändert. Beide bekommen den ganzen freien Platz (der Ring nimmt
 * darin das größte Quadrat, mittig).
 */
@Composable
private fun DurationPicker(state: DurationPickerState, onTick: (major: Boolean) -> Unit) {
    TimeDial(state = state, onTick = onTick, modifier = Modifier.fillMaxSize())
}

/**
 * Mindestkante der Dauerwahl: [Dimens.DurationPickerMin], bei großer Schrift so viel mehr, dass die
 * Mitte ganz im Ring bleibt (LAYOUT-03) – die Seite scrollt dann, abgeschnitten wird nie.
 */
@Composable
private fun durationPickerMinSide(): Dp = timeDialMinSide(Dimens.DurationPickerMin)

/**
 * Kopf oben, [center] im restlichen Platz der Ansichtshöhe [viewportHeight], [bottom] darunter.
 * [top] und [bottom] behalten ihre natürliche Höhe; [center] bekommt die volle Breite und den
 * Rest, mindestens [minCenterHeight]. Reicht die Ansicht dafür nicht, wird das Ganze höher als
 * [viewportHeight] (der Aufrufer scrollt dann) – so wird nie etwas abgeschnitten. Gedacht für
 * einen Eltern-`verticalScroll`, in dem die Höhe sonst unbegrenzt wäre.
 */
@Composable
private fun FillBetweenLayout(
    viewportHeight: Dp,
    minCenterHeight: Dp,
    top: @Composable () -> Unit,
    center: @Composable () -> Unit,
    bottom: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(contents = listOf(top, center, bottom), modifier = modifier) { (topM, centerM, bottomM), constraints ->
        val w = constraints.maxWidth
        val loose = Constraints(maxWidth = w)
        val topP = topM.map { it.measure(loose) }
        val bottomP = bottomM.map { it.measure(loose) }
        val topH = topP.sumOf { it.height }
        val bottomH = bottomP.sumOf { it.height }
        val centerH = maxOf(viewportHeight.roundToPx() - topH - bottomH, minCenterHeight.roundToPx())
        val centerP = centerM.map { it.measure(Constraints.fixed(w, centerH)) }
        layout(w, topH + centerH + bottomH) {
            var y = 0
            topP.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
            centerP.forEach { it.placeRelative(0, y) }
            y += centerH
            bottomP.forEach {
                it.placeRelative(0, y)
                y += it.height
            }
        }
    }
}

/**
 * „Klang & Stimme“: zugeklappt nur eine Zeile mit der aktuellen Wahl (MUSTER-03) – eine schlichte
 * Sitzung braucht nur die Dauer. Aufgeklappt Gong und Sprachanleitung. Der Zustand bleibt
 * startEndGong/intervalOn, die Reihe bildet ihn nur auf eine Wahl ab.
 */
@Composable
private fun SoundSection(
    startEndGong: Boolean,
    intervalOn: Boolean,
    gongIntervalMin: Int,
    speech: Boolean,
    speechAvailable: Boolean,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onGong: (GongChoice) -> Unit,
    onSpeech: (Boolean) -> Unit,
) {
    val gong = when {
        intervalOn -> GongChoice.INTERVAL
        startEndGong -> GongChoice.START_END
        else -> GongChoice.OFF
    }
    Column {
        SectionHeader(
            title = stringResource(R.string.meditation_sound_voice_label),
            color = MaterialTheme.colorScheme.secondary,
            teaser = stringResource(
                R.string.meditation_sound_summary,
                when (gong) {
                    GongChoice.OFF -> stringResource(R.string.meditation_gong_off_short)
                    GongChoice.START_END -> stringResource(R.string.meditation_gong_startstop)
                    GongChoice.INTERVAL -> stringResource(R.string.meditation_interval_minutes, gongIntervalMin)
                },
                stringResource(
                    if (speech && speechAvailable) R.string.meditation_voice_on else R.string.meditation_voice_off,
                ),
            ),
            expanded = expanded,
            onExpandedChange = onExpandedChange,
        )
        if (expanded) {
            SoundOptions(
                gong = gong,
                gongIntervalMin = gongIntervalMin,
                speech = speech,
                speechAvailable = speechAvailable,
                onGong = onGong,
                onSpeech = onSpeech,
            )
        }
        // Die gepinnte Start-Leiste hat selbst ListGap Innenrand – hier nur ein kleiner Abstand.
        Spacer(Modifier.height(Dimens.GapSmall))
    }
}

/** Aufgeklappter Inhalt von „Klang & Stimme“: Gong-Wahl und Sprachanleitung. */
@Composable
private fun SoundOptions(
    gong: GongChoice,
    gongIntervalMin: Int,
    speech: Boolean,
    speechAvailable: Boolean,
    onGong: (GongChoice) -> Unit,
    onSpeech: (Boolean) -> Unit,
) {
    Column {
        Spacer(Modifier.height(Dimens.GapSmall))
        SubLabel(stringResource(R.string.meditation_gong_label), Modifier.choiceGroupHeading(), color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(Dimens.GapSmall))
        // Nur der Modus wird hier gewählt; die Intervall-Länge steht in den Einstellungen.
        SegmentedChoiceRow(GongChoice.entries, gong, onGong, groupLabel = stringResource(R.string.meditation_gong_label)) { choice ->
            when (choice) {
                GongChoice.OFF -> stringResource(R.string.meditation_off)
                GongChoice.START_END -> stringResource(R.string.meditation_gong_startstop)
                GongChoice.INTERVAL -> stringResource(R.string.meditation_interval_minutes, gongIntervalMin)
            }
        }
        // Abstand wie nach dem Dauer-Raster; der Touch-Rand der Segmente allein wirkte zu eng.
        Spacer(Modifier.height(Dimens.GapSmall))
        // Schalter in der Zeile seines Namens, Hinweis darunter; die ganze Zeile schaltet.
        ToggleRow(
            label = stringResource(R.string.meditation_speech_label),
            checked = speech && speechAvailable,
            onCheckedChange = onSpeech,
            hint = if (speechAvailable) {
                stringResource(R.string.meditation_speech_hint)
            } else {
                stringResource(R.string.meditation_speech_unavailable)
            },
            enabled = speechAvailable,
        )
    }
}

@Composable
private fun RunningContent(
    state: MeditationUiState,
    onTogglePause: () -> Unit,
    onTapHaptic: () -> Unit,
    onRestart: () -> Unit,
    onEnd: () -> Unit,
) {
    val preparing = state.status == MeditationStatus.PREPARING
    val paused = state.status == MeditationStatus.PAUSED
    val ringDescription = stringResource(R.string.cd_meditation_ring)
    val ringAction = stringResource(if (paused) R.string.circle_action_resume else R.string.circle_action_pause)
    // TalkBack-Ansage der Zustandswechsel (wie die Atem-Session ihren Phasennamen ansagt): als
    // Zustand am Titel („Meditation, Pausiert“), der sichtbare Name bleibt der gesprochene.
    val stateAnnounce = when {
        preparing -> stringResource(R.string.session_get_ready)
        paused -> stringResource(R.string.session_paused)
        else -> null
    }

    // Ruhiger, VOLL gefüllter Kreis (kein Wachstum); der Fortschritt steht in der Zahl.
    val timeText = when {
        preparing -> state.countdown.toString()
        state.mode == MeditationMode.TIMED -> formatMeditationTime(state.remainingMs)
        else -> formatMeditationTime(state.elapsedMs)
    }

    // Tap-Flash (geteilt mit der Atem-Session); die UI-Haptik spielt der Player im AppContainer
    // (AUDIO-04, -05).
    val tapFlash = rememberTapFlash()

    // Gleiches Gerüst wie die Atem-Session (SessionRunningLayout): Titel oben, Kreis im freien
    // Platz, Steuerung unten – nichts überlappt, auch bei großer Schrift.
    SessionRunningLayout(
        modifier = Modifier.fillMaxSize().padding(Dimens.SessionPadding),
        top = {
            // Titel oben (wie die Atem-Session ihren Übungsnamen zeigt) – nicht nur „nackte" Zeit.
            Text(
                text = stringResource(R.string.meditation_title),
                style = MaterialTheme.typography.titleSmall,
                // Wie der Übungsname der Atem-Session: voll, nicht gedämpft.
                color = MaterialTheme.colorScheme.onBackground,
                // liveRegion: TalkBack sagt Zustandswechsel (Vorbereitung/läuft/pausiert) an.
                modifier = Modifier
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        stateAnnounce?.let { stateDescription = it }
                    },
            )
        },
        bottom = {
            // Dieselbe Icon-Steuerung wie die Atem-Session; im Countdown nur „Beenden“.
            SessionControlBar(
                playing = !paused,
                onMain = onTogglePause,
                onRestart = onRestart,
                onStop = onEnd,
                preparing = preparing,
                modifier = Modifier.fillMaxWidth(),
            )
        },
    ) { side ->
        Box(contentAlignment = Alignment.Center) {
            // Fortschrittsbogen wie in der Atem-Session (nur Timer; offen = nur Spur).
            val progress = if (state.mode == MeditationMode.TIMED && !preparing) {
                (state.elapsedMs.toFloat() / (state.elapsedMs + state.remainingMs).coerceAtLeast(1L)).coerceIn(0f, 1f)
            } else {
                null
            }
            BreathingCircle(
                fraction = 1f,
                progress = progress,
                modifier = Modifier.size(side),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (preparing) {
                        // Kleiner als die Zeit und nie breiter als die Scheibe, damit es auch bei
                        // großer Schrift im Kreis bleibt (bricht dann zwischen den Wörtern um).
                        GlowText(
                            text = stringResource(R.string.session_get_ready),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.widthIn(max = breathingDiscDiameter(side, 1f)),
                        )
                        Spacer(Modifier.height(Dimens.GapTiny))
                    }
                    GlowText(text = timeText, style = MaterialTheme.typography.displaySmall)
                }
            }
            // Wie in der Atem-Session: Tippen auf den Kreis (bis zur Spur, nicht ins Quadrat
            // daneben) pausiert/setzt fort; im Countdown tut er nichts.
            CircleTapTarget(
                side = side,
                description = ringDescription,
                clickLabel = ringAction,
                onTap = if (preparing) {
                    null
                } else {
                    { tapFlash.toggle(running = state.status == MeditationStatus.RUNNING, haptic = onTapHaptic, toggle = onTogglePause) }
                },
            )
            // Flash-Overlay ÜBER dem Kreis (Geschwister, nicht im Kreis-Content).
            PauseFlash(alpha = tapFlash.alpha, isPause = tapFlash.isPause, maxSize = side)
        }
    }
}
