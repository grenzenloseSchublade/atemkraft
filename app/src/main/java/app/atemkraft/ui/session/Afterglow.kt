package app.atemkraft.ui.session

import android.os.SystemClock
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import app.atemkraft.R
import app.atemkraft.data.SessionDisplay
import app.atemkraft.domain.CircleFractions
import app.atemkraft.playback.SessionStatus
import app.atemkraft.playback.SessionUiState
import app.atemkraft.ui.components.BreathingCircle
import app.atemkraft.ui.components.SessionControlBar
import app.atemkraft.ui.components.SessionRunningLayout
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.Motion
import app.atemkraft.ui.theme.rememberAnimationsEnabled

/**
 * Ob der Nachklang gerade läuft und wie weit (0…1); `null`, wenn nicht (mehr). Läuft nur am
 * Ende-Screen einer Übung, die von selbst zu Ende kam ([SessionUiState.finishedAtMs]), mit
 * eingeschaltetem [SessionDisplay.afterglow] und ohne „Danach in die Stille“ – dort mündet das Ende
 * in die Stille. Die Zeit läuft ab dem Ende auf derselben Uhr wie die Session (MOTION-01): Wer das
 * Overlay erst später öffnet, sieht den Rest bzw. gleich „Spür kurz nach …“. Ein Tipp beendet ihn
 * ([skip]); das gilt bis zum nächsten Ende, auch über das Drehen hinweg.
 */
internal class AfterglowState(val fraction: Float?, val skip: () -> Unit)

@Composable
internal fun rememberAfterglow(state: SessionUiState, display: SessionDisplay): AfterglowState {
    val finishedAt = state.finishedAtMs
    val animated = rememberAnimationsEnabled()
    // Bei Animator-Skala 0 steht der Kreis still; dann nur eine kurze Ruhe statt der ganzen Dauer.
    val durationMs = if (animated) Motion.Afterglow else Motion.AfterglowStill
    var skipped by rememberSaveable(finishedAt) { mutableStateOf(false) }
    val wanted = finishedAt != null && display.afterglow && !state.silenceAfter && state.status == SessionStatus.FINISHED && !skipped
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(finishedAt, wanted) {
        if (!wanted) return@LaunchedEffect
        while (now - finishedAt < durationMs) {
            withFrameMillis { }
            now = SystemClock.elapsedRealtime()
        }
    }
    val elapsed = if (finishedAt == null) Long.MAX_VALUE else now - finishedAt
    val fraction = if (wanted && elapsed in 0 until durationMs) (elapsed.toFloat() / durationMs) else null
    return AfterglowState(fraction = if (animated) fraction else fraction?.let { 0f }, skip = { skipped = true })
}

/**
 * Nachklang (Eigentümer 2026-11-25): Nach dem Ende atmet der Kreis noch einmal still aus und erlischt,
 * ohne Zahl und ohne Knöpfe; erst dann kommt „Spür kurz nach …“. Der Kreis steht an derselben Stelle
 * wie in der Übung (dasselbe Gerüst wie die Ruhe-Ansicht, Kopf und Steuerung ausgeblendet, ihr Platz
 * bleibt). Ein Tipp irgendwo überspringt ihn; TalkBack hört „Nachklang“ und kann ihn mit
 * „überspringen“ beenden. Der Logbuch-Eintrag steht da schon (wie bisher beim Ende).
 *
 * ```
 *  ┌──────────────────────────┐
 *  │  (Kopf ausgeblendet)     │
 *  │                          │
 *  │          ◯  → ·          │   Kreis atmet aus und verlischt (Motion.Afterglow)
 *  │                          │
 *  │  (Steuerung ausgeblendet)│   Tipp überall = überspringen
 *  └──────────────────────────┘
 * ```
 */
@Composable
internal fun AfterglowContent(state: SessionUiState, display: SessionDisplay, progress: Float, onSkip: () -> Unit) {
    val eased = FastOutSlowInEasing.transform(progress.coerceIn(0f, 1f))
    val name = stringResource(R.string.session_afterglow)
    val skipLabel = stringResource(R.string.session_afterglow_skip)
    Box(Modifier.fillMaxSize()) {
        SessionRunningLayout(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.SessionPadding),
            centerExtra = sessionCenterExtra(display),
            chromeVisible = false,
            top = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    SessionTitle(state)
                    // Platz der Zeile „Anleitung“, wie in der Übung: Der Kreis rückt nicht.
                    if (state.patternHint != null) Spacer(Modifier.height(Dimens.MinTouchTarget))
                }
            },
            bottom = { SessionControlBar(playing = false, onMain = {}, onRestart = {}, onStop = {}) },
        ) { side ->
            BreathingCircle(
                fraction = CircleFractions.REST * (1f - eased),
                progress = 1f,
                ring = display.showProgress,
                modifier = Modifier
                    .size(side)
                    .graphicsLayer { alpha = 1f - eased },
            )
        }
        Box(
            Modifier
                .matchParentSize()
                // Ganze Fläche als Tippziel ohne Welle (sie läge über dem ganzen Bildschirm); für
                // TalkBack und Tastatur dieselbe Aktion als Schaltfläche.
                .pointerInput(onSkip) { detectTapGestures { onSkip() } }
                .semantics {
                    contentDescription = name
                    liveRegion = LiveRegionMode.Polite
                    role = Role.Button
                    onClick(label = skipLabel) {
                        onSkip()
                        true
                    }
                },
        )
    }
}
