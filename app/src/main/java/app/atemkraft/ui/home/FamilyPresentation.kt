package app.atemkraft.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.domain.BreathingFamily
import app.atemkraft.domain.EvidenceLevel
import app.atemkraft.ui.theme.FamilyBalance
import app.atemkraft.ui.theme.FamilyFunctional
import app.atemkraft.ui.theme.FamilySympathetic
import app.atemkraft.ui.theme.FamilyVagal

/** Akzentfarbe je Familie für Überschriften, Karten-Rand und Tags. */
fun BreathingFamily.color(): Color = when (this) {
    BreathingFamily.DOWNREGULATE -> FamilyVagal
    BreathingFamily.UPREGULATE -> FamilySympathetic
    BreathingFamily.BALANCE -> FamilyBalance
    BreathingFamily.FUNCTIONAL -> FamilyFunctional
}

/** Lokalisierter Familien-Titel. */
@Composable
fun BreathingFamily.title(): String = stringResource(
    when (this) {
        BreathingFamily.DOWNREGULATE -> R.string.family_downregulate
        BreathingFamily.UPREGULATE -> R.string.family_upregulate
        BreathingFamily.BALANCE -> R.string.family_balance
        BreathingFamily.FUNCTIONAL -> R.string.family_functional
    },
)

/** Lokalisierter Text des Studienlage-Etiketts. */
@Composable
fun EvidenceLevel.label(): String = stringResource(
    when (this) {
        EvidenceLevel.WELL_SUPPORTED -> R.string.tag_well_supported
        EvidenceLevel.STUDIED -> R.string.tag_studied
        EvidenceLevel.LITTLE_STUDIED -> R.string.tag_little_studied
    },
)
