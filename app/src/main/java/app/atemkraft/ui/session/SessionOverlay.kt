package app.atemkraft.ui.session

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import app.atemkraft.R

/** Vollbild-Session über dem Scaffold. „Minimieren" (Chevron ˅) klappt zur Mini-Leiste zurück. */
@Composable
fun SessionOverlay(
    state: SessionUiState,
    showNextPhase: Boolean,
    onMinimize: () -> Unit,
    onToggleMute: () -> Unit,
    onTogglePause: () -> Unit,
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onStop: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
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
                // Schneller Ton-Schalter für die laufende Session (Einstellung bleibt unberührt).
                IconButton(onClick = onToggleMute) {
                    Icon(
                        painter = painterResource(
                            if (state.muted) R.drawable.ic_sound_off else R.drawable.ic_sound_on,
                        ),
                        contentDescription = stringResource(
                            if (state.muted) R.string.action_sound_off else R.string.action_sound_on,
                        ),
                        tint = MaterialTheme.colorScheme.onBackground.copy(
                            alpha = if (state.muted) 0.5f else 1f,
                        ),
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                SessionScreen(
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
}
