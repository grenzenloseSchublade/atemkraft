package app.atemkraft.ui.session

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.PhaseType

/** Sichtbares Label einer Phase: eigener Text der Übung oder Standard je Typ. */
@Composable
fun phaseDisplayLabel(type: PhaseType?, custom: String?): String {
    if (!custom.isNullOrBlank()) return custom
    return when (type) {
        PhaseType.INHALE -> stringResource(R.string.phase_inhale)
        PhaseType.EXHALE -> stringResource(R.string.phase_exhale)
        PhaseType.HOLD_FULL -> stringResource(R.string.phase_hold_full)
        PhaseType.HOLD_EMPTY -> stringResource(R.string.phase_hold_empty)
        PhaseType.INHALE_TOP_UP -> stringResource(R.string.phase_inhale_top_up)
        PhaseType.REST -> stringResource(R.string.phase_rest)
        null -> ""
    }
}
