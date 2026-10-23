package app.atemkraft.ui.patterns.curve

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.FreePattern
import app.atemkraft.ui.components.AppIconButton
import app.atemkraft.ui.theme.rememberAnimationsEnabled
import kotlin.math.roundToLong

private const val MILLIS_PER_SECOND = 1000f

/**
 * Zustand der Vorschau unter der Atemkurve: ob sie läuft und wo sie im Atemzug steht ([time],
 * Sekunden ab Beginn des Durchlaufs). Hochgezogen, damit der Aufrufer sie anhalten kann (Seite
 * nicht mehr sichtbar) und den Ton danach taktet:
 *
 * ```
 * val phase by remember(pattern) { derivedStateOf { preview.phaseIndexIn(pattern) } }
 * PreviewSoundEffect(sound, preview.playing, phase) { /* PhaseAudio mit preview.remainingMillisIn(pattern) */ }
 * ```
 * Anlegen mit [rememberCurvePreviewState].
 */
@Stable
class CurvePreviewState internal constructor() {
    /** Läuft die Vorschau? */
    var playing by mutableStateOf(false)
        private set

    /** Zeit im Atemzug in Sekunden (0, solange die Vorschau nicht lief). */
    var time by mutableFloatStateOf(0f)
        internal set

    /** Startet die Vorschau am Anfang des Atemzugs (wie die Vorlage). */
    fun play() {
        time = 0f
        playing = true
    }

    /** Hält die Vorschau an (Ziehen, Seite verlassen, App im Hintergrund). */
    fun pause() {
        playing = false
    }

    /** Wechselt zwischen [play] und [pause]. */
    fun toggle() = if (playing) pause() else play()

    /** Index der Phase von [pattern], in der die Vorschau steht. */
    fun phaseIndexIn(pattern: FreePattern): Int = pattern.phaseIndexAt(time)

    /** Restdauer der aktuellen Phase von [pattern] in ms (für den Dauerton beim Einschalten mitten in der Phase). */
    fun remainingMillisIn(pattern: FreePattern): Long {
        val index = phaseIndexIn(pattern)
        val end = pattern.startOf(index) + (pattern.phases.getOrNull(index)?.seconds ?: 0)
        return ((end - time) * MILLIS_PER_SECOND).roundToLong().coerceAtLeast(0L)
    }
}

/** Legt den Zustand der Vorschau für [BreathCurveEditor] an. */
@Composable
fun rememberCurvePreviewState(): CurvePreviewState = remember { CurvePreviewState() }

/**
 * Knöpfe der Kurven-Vorschau: Play/Pause ([AppIconButton] in `secondary`, „Vorschau abspielen“ /
 * „Vorschau anhalten“) startet [preview] am Anfang des Atemzugs, dahinter [trailing] (Muster-
 * Editor: Lautsprecher-Schalter). Bei abgeschalteten System-Animationen (Animator-Skala 0) fehlt
 * die ganze Gruppe (MOTION-02) – ohne Bewegung zeigte die Vorschau nichts.
 */
@Composable
fun CurvePreviewControls(
    preview: CurvePreviewState,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {},
) {
    val animationsEnabled = rememberAnimationsEnabled()
    if (!animationsEnabled) return
    val tint = MaterialTheme.colorScheme.secondary
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        AppIconButton(onClick = preview::toggle) {
            if (preview.playing) {
                Icon(painterResource(R.drawable.ic_pause), contentDescription = stringResource(R.string.cd_preview_pause), tint = tint)
            } else {
                Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.cd_preview_play), tint = tint)
            }
        }
        trailing()
    }
}
