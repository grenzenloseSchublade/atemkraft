package app.atemkraft.ui.patterns

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.atemkraft.R
import app.atemkraft.cue.PhaseAudio
import app.atemkraft.data.CueSettings
import app.atemkraft.domain.SoundMode
import app.atemkraft.playback.AppPreviewSoundOutput
import app.atemkraft.playback.PreviewCuePlayer
import app.atemkraft.playback.PreviewSoundOutput
import app.atemkraft.ui.components.SoundSwitch

/**
 * Ton der Editor-Vorschau (Nachtrag 3): hält den [PreviewCuePlayer] und den Lautsprecher-Schalter.
 * [soundOn] startet an, wenn der Klangmodus der App nicht „Aus“ ist ([available]); die Wahl des
 * Nutzers übersteht Drehen und Prozesstod. Getaktet wird über [onPhase]/[stop] oder bequemer über
 * [PreviewSoundEffect]. Anlegen mit [rememberPreviewSound].
 */
@Stable
class PreviewSoundState internal constructor(
    private val player: PreviewCuePlayer,
    private val choice: MutableState<Boolean?>,
) {
    private var settings by mutableStateOf<CueSettings?>(null)

    /** Klangmodus der App ist nicht „Aus“ – nur dann gibt es einen Vorschau-Ton. */
    val available: Boolean get() = settings?.soundMode != SoundMode.OFF

    /** Stand des Lautsprecher-Schalters: an, solange der Nutzer ihn nicht ausgeschaltet hat und [available]. */
    val soundOn: Boolean get() = available && choice.value != false

    /** Lautsprecher-Schalter umlegen; aus = sofort still. */
    fun setSoundOn(on: Boolean) {
        choice.value = on
        player.enabled = on
    }

    /** Phasenbeginn (bzw. Fortsetzen mit Restdauer, [phaseBegins] = false): siehe [PreviewCuePlayer.onPhase]. */
    fun onPhase(phase: PhaseAudio, phaseBegins: Boolean = true) = player.onPhase(phase, phaseBegins)

    /** Pause der Vorschau: Ton aus, Audio-Fokus abgeben. */
    fun stop() = player.stop()

    internal fun update(next: CueSettings, blocked: Boolean) {
        settings = next
        player.enabled = choice.value != false
        player.updateSettings(next)
        player.blocked = blocked
    }
}

/**
 * Legt den Vorschau-Ton für die Lebensdauer der Seite an. [settings] = die Klang-Einstellungen
 * der App (`SettingsRepository.cueSettings`; Modus, Lautstärke, Zäsur). [otherAudioActive] = eine
 * Atem-Übung oder Meditation läuft (auch minimiert) – dann bleibt die Vorschau still.
 *
 * Still wird es außerdem, sobald die Seite nicht mehr zu sehen ist – App im Hintergrund oder eine
 * andere Seite darüber (Lifecycle `ON_STOP`; danach [onHide], damit die Seite ihre Vorschau
 * anhält) – und endgültig beim Verlassen der Seite (Threads und Audio-Fokus frei). [output]
 * tauschen nur Tests.
 */
@Composable
fun rememberPreviewSound(
    settings: CueSettings,
    otherAudioActive: Boolean = false,
    onHide: () -> Unit = {},
    output: (Context) -> PreviewSoundOutput = ::AppPreviewSoundOutput,
): PreviewSoundState {
    val context = LocalContext.current.applicationContext
    val choice = rememberSaveable { mutableStateOf<Boolean?>(null) }
    val player = remember { PreviewCuePlayer(output(context)) }
    val state = remember(player) { PreviewSoundState(player, choice) }
    var hidden by remember { mutableStateOf(false) }
    val currentOnHide by rememberUpdatedState(onHide)

    SideEffect { state.update(settings, blocked = otherAudioActive || hidden) }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        // Direkt, nicht erst über die nächste Komposition: Im Hintergrund kommen keine Frames.
        hidden = true
        player.blocked = true
        currentOnHide()
    }
    LifecycleEventEffect(Lifecycle.Event.ON_START) { hidden = false }
    DisposableEffect(player) { onDispose { player.release() } }
    return state
}

/**
 * Taktet [sound] aus der laufenden Vorschau: [playing] = Vorschau läuft, [phaseIndex] = Index der
 * aktuellen Phase im Ablauf, [phase] liefert sie als [PhaseAudio] (Typ, Füllstand, Dauer – beim
 * Fortsetzen oder Einschalten die Restdauer). Ein neuer Index ist ein Phasenbeginn (Wechselton);
 * Einschalten des Tons mitten in der Phase setzt nur den Dauerton fort. Pause und Verlassen
 * machen still.
 *
 * Ändert sich die Folge während der Vorschau, kann derselbe Index auf eine andere Phase zeigen:
 * Eine neue [revision] (die Folge) sendet die Phase neu; ist dabei ihre Art [phaseKind] (Typ und
 * Füllstand) eine andere, gilt das als Phasenbeginn mit Wechselton, sonst läuft der Ton mit der
 * neuen Restdauer weiter.
 */
@Composable
fun PreviewSoundEffect(
    sound: PreviewSoundState,
    playing: Boolean,
    phaseIndex: Int,
    phaseKind: Any? = null,
    revision: Any? = null,
    phase: () -> PhaseAudio,
) {
    val currentPhase by rememberUpdatedState(phase)
    val last = remember { intArrayOf(NO_PHASE) }
    val lastKind = remember { arrayOfNulls<Any?>(1) }
    val soundOn = sound.soundOn
    LaunchedEffect(sound, playing, phaseIndex, phaseKind, revision, soundOn) {
        if (!playing) {
            last[0] = NO_PHASE
            lastKind[0] = null
            sound.stop()
            return@LaunchedEffect
        }
        val begins = phaseIndex != last[0] || phaseKind != lastKind[0]
        last[0] = phaseIndex
        lastKind[0] = phaseKind
        if (soundOn) sound.onPhase(currentPhase(), phaseBegins = begins) else sound.stop()
    }
    DisposableEffect(sound) { onDispose { sound.stop() } }
}

/**
 * Lautsprecher-Schalter der Vorschau ([SoundSwitch], „Ton der Vorschau“, An/Aus) in der Farbe
 * der Vorschau (`secondary`). Ist der Klangmodus der App „Aus“, steht er auf Aus und ist
 * deaktiviert.
 */
@Composable
fun PreviewSoundSwitch(sound: PreviewSoundState, modifier: Modifier = Modifier) {
    SoundSwitch(
        on = sound.soundOn,
        onToggle = sound::setSoundOn,
        name = stringResource(R.string.cd_preview_sound),
        modifier = modifier,
        enabled = sound.available,
        tint = MaterialTheme.colorScheme.secondary,
    )
}

private const val NO_PHASE = -1
