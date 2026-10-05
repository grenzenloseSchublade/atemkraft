package app.atemkraft.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.atemkraft.R
import app.atemkraft.ui.theme.Dimens
import app.atemkraft.ui.theme.SECONDARY

/** Ein anpassbarer Intervall-Wert in der Einstell-Karte; als Parameter null = Phase fehlt. */
data class PhaseAdjust(
    val value: Int,
    val default: Int,
    val range: IntRange,
    val onChange: (Int) -> Unit,
)

/**
 * Einstell-Karte einer Übung: Dauer-/Runden-Stepper, dahinter aufklappbar die Intervall-Stepper.
 * Eingeklappt markiert „· angepasst" abweichende Intervalle; der Hinweis verschwindet von
 * selbst, sobald die Werte – auch manuell zurückgesteppt – wieder den Defaults entsprechen.
 * Das Zurücksetzen selbst sitzt beim Start-Button (Split-Button im Detail-Screen).
 */
@Composable
fun SessionAdjustCard(
    durationLabel: String,
    duration: Int,
    durationDefault: Int,
    durationRange: IntRange,
    onDuration: (Int) -> Unit,
    inhale: PhaseAdjust?,
    hold: PhaseAdjust?,
    exhale: PhaseAdjust?,
    intervalsExpanded: Boolean,
    onToggleIntervals: () -> Unit,
) {
    val intervalsModified = listOfNotNull(inhale, hold, exhale).any { it.value != it.default }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(vertical = Dimens.GapTiny)) {
            Stepper(durationLabel, duration, durationRange, vertical = Dimens.GapSmall, onChange = onDuration)

            if (inhale != null || hold != null || exhale != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = Dimens.CardPadding),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    DisclosureToggle(
                        text = stringResource(R.string.adjust_intervals),
                        expanded = intervalsExpanded,
                        onToggle = onToggleIntervals,
                        // Caret-Glyph (6 dp Innenrand im Icon) steht wie bisher knapp vor der
                        // Stepper-Label-Kante (CardPadding).
                        modifier = Modifier.padding(start = Dimens.GapTiny),
                    )
                    if (!intervalsExpanded && intervalsModified) {
                        Text(
                            text = "· " + stringResource(R.string.adjust_modified),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = SECONDARY),
                            modifier = Modifier.padding(start = Dimens.GapTiny),
                        )
                    }
                }
                if (intervalsExpanded) {
                    inhale?.let {
                        Stepper(stringResource(R.string.adjust_inhale), it.value, it.range, vertical = Dimens.GapTiny, onChange = it.onChange)
                    }
                    hold?.let {
                        Stepper(stringResource(R.string.adjust_hold), it.value, it.range, vertical = Dimens.GapTiny, onChange = it.onChange)
                    }
                    exhale?.let {
                        Stepper(stringResource(R.string.adjust_exhale), it.value, it.range, vertical = Dimens.GapTiny, onChange = it.onChange)
                    }
                    Spacer(Modifier.height(Dimens.GapHairline))
                }
            }
        }
    }
}
