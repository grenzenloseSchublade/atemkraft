package app.atemkraft.ui.meditation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.MeditationMode
import app.atemkraft.domain.formatClock
import app.atemkraft.playback.MeditationStatus
import app.atemkraft.playback.MeditationUiState
import app.atemkraft.ui.components.MiniNowPlayingBar

/** „Now-Playing"-Leiste der Meditation (Titel + Zeit/Status); tippen öffnet das Vollbild-Overlay. */
@Composable
fun MiniMeditationBar(state: MeditationUiState, onClick: () -> Unit) {
    val title = stringResource(R.string.meditation_title)
    val statusText = when (state.status) {
        MeditationStatus.FINISHED -> stringResource(R.string.meditation_done_title)

        MeditationStatus.PAUSED -> stringResource(R.string.session_paused)

        else -> if (state.mode == MeditationMode.TIMED) {
            formatClock(state.remainingMs)
        } else {
            formatClock(state.elapsedMs)
        }
    }
    MiniNowPlayingBar(title = title, statusText = statusText, onClick = onClick)
}
