package app.atemkraft.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.ui.components.MiniNowPlayingBar

/** „Now-Playing"-Leiste für die Atem-Session; tippen öffnet die Vollbild-Session. */
@Composable
fun MiniSessionBar(
    state: SessionUiState,
    onClick: () -> Unit,
) {
    val statusText = when (state.status) {
        SessionStatus.FINISHED -> stringResource(R.string.session_done_title)
        SessionStatus.PAUSED -> stringResource(R.string.session_paused)
        else -> phaseDisplayLabel(state.phaseType, state.phaseLabel)
    }
    MiniNowPlayingBar(title = state.exerciseName, statusText = statusText, onClick = onClick)
}
